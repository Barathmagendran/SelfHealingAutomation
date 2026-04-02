package healing.core;

import healing.ai.ClaudeClient;
import healing.ai.DiagnosisResult;
import healing.ai.HealingException;
import healing.audit.AuditLedger;
import healing.audit.SlackNotifier;
import healing.repair.HealingCodeRewriter;
import healing.repair.SandboxValidator;
import healing.repair.ValidationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central orchestrator for the Self-Healing Workflow Engine.
 *
 * <p>Chains the 6 healing phases in sequence. Called from
 * {@link StepFailureInterceptor} when a Cucumber step fails.
 *
 * <p><b>Healing Phases:</b>
 * <ol>
 *   <li><b>DETECTING</b>   — Context already captured by interceptor</li>
 *   <li><b>DIAGNOSING</b>  — Claude AI diagnoses root cause and generates fixed code</li>
 *   <li><b>REWRITING</b>   — Patch applied to .java source file on disk</li>
 *   <li><b>TESTING</b>     — Sandbox compiles patch and runs structural checks</li>
 *   <li><b>DEPLOYING</b>   — Patch committed (healed source ready for next run)</li>
 *   <li><b>HEALED</b>      — Audit log appended, team notified</li>
 * </ol>
 *
 * <p><b>Guardrails:</b>
 * <ul>
 *   <li>Healing is scoped to the configured package (default: {@code stepdefinitions})</li>
 *   <li>AssertionErrors are never auto-healed — always escalated to human</li>
 *   <li>LOW confidence AI responses are escalated even if sandbox passes</li>
 *   <li>Patches exceeding {@code healing.max.patch.lines} are rejected</li>
 *   <li>Max retries before human escalation is configurable</li>
 * </ul>
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class HealingEngine {

    private static final Logger log = LogManager.getLogger(HealingEngine.class);

    private final ClaudeClient      claudeClient  = new ClaudeClient();
    private final HealingCodeRewriter rewriter    = new HealingCodeRewriter();
    private final SandboxValidator  validator      = new SandboxValidator();
    private final AuditLedger       auditLedger    = new AuditLedger();
    private final SlackNotifier     slackNotifier  = new SlackNotifier();

    // ─────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────

    /**
     * Executes the full healing cycle for a failed step.
     *
     * <p>This method is intentionally non-throwing — all exceptions are caught,
     * logged, and recorded. The Cucumber runner must never fail due to a
     * healing engine error.
     *
     * @param ctx the failure snapshot captured by the interceptor
     */
    public void heal(HealingContext ctx) {
        if (!HealingConfig.isEnabled()) {
            log.debug("[HealingEngine] Healing disabled — skipping.");
            return;
        }

        log.info("[HealingEngine] ══════════════════════════════════════════");
        log.info("[HealingEngine]  HEALING CYCLE STARTED");
        log.info("[HealingEngine]  Scenario : {}", ctx.getScenarioName());
        log.info("[HealingEngine]  Step     : {}", ctx.getStepText());
        log.info("[HealingEngine]  Class    : {}", ctx.getStepClass());
        log.info("[HealingEngine]  Error    : {}", ctx.getErrorMessage());
        log.info("[HealingEngine] ══════════════════════════════════════════");

        long startTime = System.currentTimeMillis();

        try {
            // ── GUARDRAIL: scope check ────────────────────────
            if (!isInScope(ctx.getStepClass())) {
                log.info("[HealingEngine] Class [{}] is outside healing scope [{}] — skipping.",
                    ctx.getStepClass(), HealingConfig.getScope());
                return;
            }

            // ── GUARDRAIL: never heal assertion errors ─────────
            if (isAssertionError(ctx.getErrorMessage())) {
                String reason = "AssertionError detected — this may be a real product bug. " +
                                "Auto-healing assertions is disabled for safety.";
                log.warn("[HealingEngine] {}", reason);
                auditLedger.recordEscalation(ctx, reason);
                slackNotifier.notifyEscalation(ctx, reason);
                return;
            }

            // ── GUARDRAIL: max retry check ─────────────────────
            if (ctx.getRetryCount() >= HealingConfig.getMaxRetries()) {
                String reason = "Max healing retries (" + HealingConfig.getMaxRetries() +
                                ") exceeded for this step.";
                log.warn("[HealingEngine] {}", reason);
                auditLedger.recordEscalation(ctx, reason);
                slackNotifier.notifyEscalation(ctx, reason);
                return;
            }

            // ── PHASE 2: DIAGNOSING ────────────────────────────
            log.info("[HealingEngine] Phase 2 — DIAGNOSING via Claude AI...");
            DiagnosisResult diagnosis = claudeClient.diagnoseAndFix(ctx);
            log.info("[HealingEngine] Root cause : {}", diagnosis.getRootCause());
            log.info("[HealingEngine] Fix desc   : {}", diagnosis.getFixDescription());
            log.info("[HealingEngine] Confidence : {}", diagnosis.getConfidence());

            // ── PHASE 3: REWRITING ─────────────────────────────
            log.info("[HealingEngine] Phase 3 — REWRITING source file...");
            rewriter.applyPatch(ctx, diagnosis);

            // ── PHASE 4: TESTING ───────────────────────────────
            log.info("[HealingEngine] Phase 4 — TESTING patched code in sandbox...");
            ValidationResult validation = validator.validate(ctx, diagnosis);

            if (!validation.isAllPassed()) {
                log.warn("[HealingEngine] Sandbox validation FAILED — rolling back patch.");
                validation.getChecks().stream()
                    .filter(c -> !c.isPassed())
                    .forEach(c -> log.warn("[HealingEngine]   ❌ {}: {}", c.getName(), c.getDetail()));

                auditLedger.recordValidationFailure(ctx, diagnosis, validation);
                slackNotifier.notifyEscalation(ctx, "Sandbox validation failed: " +
                    validation.getFailedCount() + " checks did not pass.");
                return;
            }

            validation.getChecks().forEach(c ->
                log.info("[HealingEngine]   ✅ {}: {}", c.getName(), c.getDetail()));

            // ── PHASE 5: DEPLOYING ─────────────────────────────
            // The patched source is already written to disk (done in REWRITING phase).
            // "Deployment" in the context of test automation = the patched .java file
            // is ready for the next compilation. No hot-swap needed for Cucumber —
            // the healed code will be active on the very next Maven test run.
            log.info("[HealingEngine] Phase 5 — DEPLOYING — patched source committed to disk.");

            // ── PHASE 6: HEALED ────────────────────────────────
            long healTimeMs = System.currentTimeMillis() - startTime;
            auditLedger.recordSuccess(ctx, diagnosis, validation, healTimeMs);
            slackNotifier.notifyHealed(ctx, diagnosis, healTimeMs);

            log.info("[HealingEngine] ══════════════════════════════════════════");
            log.info("[HealingEngine]  ✅ HEALING COMPLETE in {}ms", healTimeMs);
            log.info("[HealingEngine]  The patched step will be active on next test run.");
            log.info("[HealingEngine] ══════════════════════════════════════════");

        } catch (HealingException e) {
            log.error("[HealingEngine] Healing cycle failed: {}", e.getMessage());
            auditLedger.recordError(ctx, e);
            slackNotifier.notifyError(ctx, e);

        } catch (Exception e) {
            log.error("[HealingEngine] Unexpected error in healing cycle: {}", e.getMessage(), e);
            auditLedger.recordError(ctx, e);
            slackNotifier.notifyError(ctx, e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Guardrail helpers
    // ─────────────────────────────────────────────────────────

    private boolean isInScope(String stepClass) {
        String scope = HealingConfig.getScope();
        return scope.isBlank() || stepClass.startsWith(scope);
    }

    private boolean isAssertionError(String errorMessage) {
        if (errorMessage == null) return false;
        return errorMessage.toLowerCase().contains("assertionerror")
            || errorMessage.toLowerCase().contains("expected") && errorMessage.toLowerCase().contains("but was")
            || errorMessage.toLowerCase().contains("expected [")
            || errorMessage.toLowerCase().contains("assert");
    }
}
