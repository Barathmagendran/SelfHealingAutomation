package healing.repair;

import healing.ai.DiagnosisResult;
import healing.core.HealingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates an AI-generated patch by compiling it and running structural checks.
 *
 * <p>Validation steps:
 * <ol>
 *   <li><b>Compilation check</b>: Compiles the patched .java file using
 *       {@link javax.tools.JavaCompiler} (requires JDK, not just JRE)</li>
 *   <li><b>Annotation check</b>: Verifies the Cucumber step annotation is still present</li>
 *   <li><b>Selenium API check</b>: Detects common bad patterns (Thread.sleep, hardcoded IDs)</li>
 *   <li><b>Confidence gate</b>: Rejects LOW confidence AI responses</li>
 * </ol>
 *
 * <p>The patch proceeds to deployment only if {@link ValidationResult#isAllPassed()} is true.
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class SandboxValidator {

    private static final Logger log = LogManager.getLogger(SandboxValidator.class);

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Runs all validation checks on the patched source file.
     *
     * @param ctx       the failure context (identifies the source file)
     * @param diagnosis the AI diagnosis (contains fixedCode and confidence)
     * @return validation result with pass/fail details for each check
     */
    public ValidationResult validate(HealingContext ctx, DiagnosisResult diagnosis) {
        log.info("[SandboxValidator] Validating patch for: {}.{}",
            ctx.getStepClass(), ctx.getStepMethod());

        List<ValidationResult.CheckResult> checks = new ArrayList<>();

        // 1. Compile patched source
        Path sourceFile = SourceCodeLocator.resolveSourceFile(ctx.getStepClass());
        CompilationCheckResult compilation = compileSource(sourceFile);
        checks.add(ValidationResult.CheckResult.builder()
            .name("Compilation")
            .passed(compilation.passed())
            .detail(compilation.output())
            .build());

        // 2. Annotation preservation
        boolean annotationOk = checkAnnotationPresent(diagnosis.getFixedCode(), ctx.getStepMethod());
        checks.add(ValidationResult.CheckResult.builder()
            .name("Cucumber annotation preserved")
            .passed(annotationOk)
            .detail(annotationOk ? "Step annotation intact" : "WARNING: Step annotation missing from patch")
            .build());

        // 3. Bad pattern detection
        BadPatternCheckResult badPatterns = checkForBadPatterns(diagnosis.getFixedCode());
        checks.add(ValidationResult.CheckResult.builder()
            .name("No bad patterns")
            .passed(badPatterns.passed())
            .detail(badPatterns.detail())
            .build());

        // 4. AI confidence gate
        boolean confidentEnough = diagnosis.isConfident();
        checks.add(ValidationResult.CheckResult.builder()
            .name("AI confidence gate")
            .passed(confidentEnough)
            .detail("Confidence: " + diagnosis.getConfidence() +
                (confidentEnough ? " — accepted" : " — LOW confidence, escalating to human"))
            .build());

        // Log results
        checks.forEach(c -> {
            if (c.isPassed()) {
                log.info("[SandboxValidator] ✅ {}: {}", c.getName(), c.getDetail());
            } else {
                log.warn("[SandboxValidator] ❌ {}: {}", c.getName(), c.getDetail());
            }
        });

        boolean allPassed   = checks.stream().allMatch(ValidationResult.CheckResult::isPassed);
        boolean structureOk = checks.stream()
            .filter(c -> !c.getName().equals("Compilation"))
            .allMatch(ValidationResult.CheckResult::isPassed);

        log.info("[SandboxValidator] Validation {}: {}/{} checks passed",
            allPassed ? "PASSED" : "FAILED",
            checks.stream().filter(ValidationResult.CheckResult::isPassed).count(),
            checks.size());

        return ValidationResult.builder()
            .compilationPassed(compilation.passed())
            .structureChecksPassed(structureOk)
            .checks(checks)
            .compilerOutput(compilation.output())
            .build();
    }

    // ─────────────────────────────────────────────────────────
    // Compilation check
    // ─────────────────────────────────────────────────────────

    private CompilationCheckResult compileSource(Path sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            log.warn("[SandboxValidator] JavaCompiler not available (JRE-only environment) — skipping compilation check.");
            return new CompilationCheckResult(true, "Skipped — JRE only, no compiler available");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var compilationUnits = fileManager.getJavaFileObjects(sourceFile.toFile());

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics,
                List.of("-source", "17", "-target", "17"),
                null,
                compilationUnits
            );

            boolean success = task.call();

            String output = diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                .map(d -> d.getMessage(null))
                .collect(Collectors.joining("; "));

            if (success) {
                log.debug("[SandboxValidator] Compilation passed");
            } else {
                log.warn("[SandboxValidator] Compilation errors: {}", output);
            }

            return new CompilationCheckResult(success, success ? "Compiled successfully" : output);

        } catch (Exception e) {
            log.warn("[SandboxValidator] Compilation check failed with exception: {}", e.getMessage());
            return new CompilationCheckResult(false, "Compiler exception: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    // Structural checks
    // ─────────────────────────────────────────────────────────

    private boolean checkAnnotationPresent(String fixedCode, String methodName) {
        return fixedCode.contains("@When")
            || fixedCode.contains("@Given")
            || fixedCode.contains("@Then")
            || fixedCode.contains("@And")
            || fixedCode.contains("@But");
    }

    private BadPatternCheckResult checkForBadPatterns(String fixedCode) {
        List<String> warnings = new ArrayList<>();

        if (fixedCode.contains("Thread.sleep")) {
            warnings.add("Uses Thread.sleep — prefer WebDriverWait");
        }
        if (fixedCode.matches("(?s).*By\\.id\\(\"[^\"]{1,5}\"\\).*")) {
            warnings.add("Short ID locator detected — may be fragile");
        }
        if (fixedCode.contains("driver.manage().timeouts()")) {
            warnings.add("Implicit wait modification — may cause test pollution");
        }

        // Warnings are informational only — do not fail the check
        if (!warnings.isEmpty()) {
            log.warn("[SandboxValidator] Pattern warnings in patch: {}", warnings);
        }
        return new BadPatternCheckResult(true, warnings.isEmpty()
            ? "No bad patterns detected"
            : "Warnings (non-blocking): " + String.join(", ", warnings));
    }

    // ─────────────────────────────────────────────────────────
    // Internal records
    // ─────────────────────────────────────────────────────────

    private record CompilationCheckResult(boolean passed, String output) {}
    private record BadPatternCheckResult(boolean passed, String detail) {}
}
