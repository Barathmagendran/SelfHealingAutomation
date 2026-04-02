package healing.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import healing.ai.DiagnosisResult;
import healing.core.HealingConfig;
import healing.core.HealingContext;
import healing.repair.ValidationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Immutable append-only audit log for all self-healing events.
 *
 * <p>Writes one JSON object per line (JSONL format) to {@code target/healing-audit.jsonl}.
 * Every healing event — success, failure, and escalation — is recorded.
 *
 * <p>The log is picked up by the Extent Report and can be parsed by CI/CD pipelines
 * to report healing metrics across test runs.
 *
 * <p>Example log entry:
 * <pre>
 * {"event":"HEALED","timestamp":"2025-01-15T10:23:45Z","scenario":"User login",
 *  "step":"When the user clicks the login button","stepClass":"stepdefinitions.web.LoginSteps",
 *  "rootCause":"Stale locator By.id('loginBtn')","fixDescription":"Added resilient CSS selector",
 *  "confidence":"HIGH","testsPassed":3,"testsTotal":3,"healTimeMs":4200}
 * </pre>
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class AuditLedger {

    private static final Logger log = LogManager.getLogger(AuditLedger.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Records a successful healing event.
     */
    public void recordSuccess(HealingContext ctx, DiagnosisResult diagnosis,
                              ValidationResult validation, long healTimeMs) {
        ObjectNode entry = buildBase(ctx, "HEALED");
        entry.put("rootCause",       diagnosis.getRootCause());
        entry.put("fixDescription",  diagnosis.getFixDescription());
        entry.put("confidence",      diagnosis.getConfidence());
        entry.put("testsPassed",     validation.getPassedCount());
        entry.put("testsTotal",      validation.getChecks().size());
        entry.put("healTimeMs",      healTimeMs);
        append(entry);
        log.info("[AuditLedger] Recorded HEALED event for: {}", ctx.getStepText());
    }

    /**
     * Records a healing attempt that failed validation.
     */
    public void recordValidationFailure(HealingContext ctx, DiagnosisResult diagnosis,
                                        ValidationResult validation) {
        ObjectNode entry = buildBase(ctx, "VALIDATION_FAILED");
        entry.put("rootCause",    diagnosis.getRootCause());
        entry.put("confidence",   diagnosis.getConfidence());
        entry.put("testsPassed",  validation.getPassedCount());
        entry.put("testsFailed",  validation.getFailedCount());
        entry.put("compilerOutput", validation.getCompilerOutput());
        append(entry);
        log.warn("[AuditLedger] Recorded VALIDATION_FAILED event for: {}", ctx.getStepText());
    }

    /**
     * Records a human escalation (max retries exceeded or unrecoverable error).
     */
    public void recordEscalation(HealingContext ctx, String reason) {
        ObjectNode entry = buildBase(ctx, "ESCALATED");
        entry.put("reason",      reason);
        entry.put("retryCount",  ctx.getRetryCount());
        append(entry);
        log.warn("[AuditLedger] Recorded ESCALATED event for: {}", ctx.getStepText());
    }

    /**
     * Records an unexpected error in the healing engine itself.
     */
    public void recordError(HealingContext ctx, Exception error) {
        ObjectNode entry = buildBase(ctx, "HEALING_ERROR");
        entry.put("error",      error.getClass().getSimpleName());
        entry.put("message",    error.getMessage());
        append(entry);
        log.error("[AuditLedger] Recorded HEALING_ERROR for: {}", ctx.getStepText());
    }

    // ─────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────

    private ObjectNode buildBase(HealingContext ctx, String event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("event",        event);
        node.put("timestamp",    Instant.now().toString());
        node.put("scenario",     ctx.getScenarioName());
        node.put("step",         ctx.getStepText());
        node.put("stepClass",    ctx.getStepClass());
        node.put("stepMethod",   ctx.getStepMethod());
        node.put("platform",     ctx.getPlatform());
        node.put("environment",  core.config.ConfigReader.getActiveEnvironment());
        return node;
    }

    private void append(ObjectNode entry) {
        try {
            Path logFile = Paths.get(HealingConfig.getAuditPath());
            Files.createDirectories(logFile.getParent());
            String line = objectMapper.writeValueAsString(entry) + "\n";
            Files.writeString(logFile, line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("[AuditLedger] Failed to write audit entry: {}", e.getMessage());
        }
    }
}
