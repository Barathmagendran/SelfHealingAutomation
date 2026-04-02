package hooks;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import core.config.ConfigReader;
import core.driver.DriverManager;
import core.reporting.ExtentReportManager;
import core.utils.NavigationUtils;
import core.utils.ScreenshotUtils;
import healing.core.HealingConfig;
import healing.core.HealingContext;
import healing.core.HealingEngine;
import healing.repair.SourceCodeLocator;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

/**
 * Cucumber lifecycle hooks for setup, teardown, and self-healing.
 *
 * <p>Handles:
 * <ul>
 *   <li>Driver initialization per scenario</li>
 *   <li>Extent Report node creation per scenario</li>
 *   <li>Screenshot capture on failure</li>
 *   <li>Driver cleanup after each scenario</li>
 *   <li>Suite-level report flush</li>
 *   <li><b>Self-Healing Engine trigger on step failure</b></li>
 * </ul>
 *
 * <p>Hook execution order on failure:
 * <pre>
 *   &#64;After(order = 2) — afterScenario  : screenshot + extent report
 *   &#64;After(order = 1) — healFailedStep : AI diagnosis + source patch
 * </pre>
 * Cucumber executes &#64;After hooks in DESCENDING order (2 before 1),
 * so reporting always completes before the healing cycle starts.
 *
 * @author Enterprise QA Team
 * @version 2.0 — Self-Healing Engine integrated
 */
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    /** Shared, stateless healing engine — safe for parallel use. */
    private static final HealingEngine HEALING_ENGINE = new HealingEngine();

    // ─────────────────────────────────────────────────────────
    // Suite-level lifecycle
    // ─────────────────────────────────────────────────────────

    @BeforeAll
    public static void beforeAll() {
        log.info("============================================================");
        log.info("  AUTOMATION SUITE STARTING");
        log.info("  Environment  : {}", ConfigReader.getActiveEnvironment().toUpperCase());
        log.info("  Platform     : {}", ConfigReader.get("platform", "web").toUpperCase());
        log.info("  Browser      : {}", ConfigReader.get("browser", "chrome").toUpperCase());
        log.info("  Self-Healing : {}", HealingConfig.isEnabled() ? "ENABLED" : "DISABLED");
        log.info("============================================================");
        ExtentReportManager.initReports();
    }

    @AfterAll
    public static void afterAll() {
        ExtentReportManager.flushReports();
        log.info("============================================================");
        log.info("  AUTOMATION SUITE COMPLETE");
        log.info("  Healing audit : target/healing-audit.jsonl");
        log.info("============================================================");
    }

    // ─────────────────────────────────────────────────────────
    // Scenario lifecycle — reporting (order=2, runs first)
    // ─────────────────────────────────────────────────────────

    @Before(order = 1)
    public void beforeScenario(Scenario scenario) {
        log.info("---------- SCENARIO START: {} ----------", scenario.getName());

        ExtentTest test = ExtentReportManager.createTest(
            scenario.getName(),
            "Tags: " + scenario.getSourceTagNames()
        );
        scenario.getSourceTagNames().forEach(tag ->
            test.assignCategory(tag.replace("@", ""))
        );

        String platform = resolvePlatform(scenario);
        if ("api".equalsIgnoreCase(platform)) {
            log.info("API scenario — skipping driver initialization.");
        } else {
            DriverManager.initDriver();
            log.info("Driver initialized for scenario: [{}]", scenario.getName());
            if ("web".equalsIgnoreCase(platform)) {
                NavigationUtils.launchUrl();
                log.info("Launched base URL for scenario: [{}]", scenario.getName());
            }
        }
    }

    /**
     * Reporting hook — screenshot capture and Extent Report update.
     * Runs at order=2 so it executes BEFORE the healing hook (order=1).
     */
    @After(order = 2)
    public void afterScenario(Scenario scenario) {
        ExtentTest test = ExtentReportManager.getTest();

        if (scenario.isFailed()) {
            log.error("SCENARIO FAILED: {}", scenario.getName());

            boolean screenshotsEnabled = Boolean.parseBoolean(
                ConfigReader.get("screenshots.on.failure", "true"));

            if (screenshotsEnabled && DriverManager.isDriverInitialized()) {
                try {
                    byte[] screenshotBytes = ScreenshotUtils.captureScreenshotAsBytes();
                    scenario.attach(screenshotBytes, "image/png", "Failure Screenshot");

                    if (test != null) {
                        test.addScreenCaptureFromBase64String(
                            ScreenshotUtils.captureScreenshotAsBase64(),
                            "Failure Screenshot"
                        );
                        test.log(Status.FAIL, "Scenario failed: " + scenario.getName());
                    }
                } catch (Exception e) {
                    log.warn("Failed to capture screenshot: {}", e.getMessage());
                }
            }
        } else {
            log.info("SCENARIO PASSED: {}", scenario.getName());
            if (test != null) {
                test.log(Status.PASS, "Scenario passed.");
            }
        }

        DriverManager.quitDriver();
        ExtentReportManager.removeTest();
        log.info("---------- SCENARIO END: {} ----------", scenario.getName());
    }

    // ─────────────────────────────────────────────────────────
    // Self-Healing Hook (order=1, runs after order=2)
    // ─────────────────────────────────────────────────────────

    /**
     * Self-healing hook — triggers AI diagnosis and source-level code repair.
     *
     * <p>Runs at {@code order=1}, which in Cucumber executes AFTER {@code order=2}.
     * This guarantees screenshots and reporting finish before healing begins.
     *
     * <p>Phase sequence triggered here:
     * <ol>
     *   <li>DETECTING  — builds HealingContext from failure snapshot</li>
     *   <li>DIAGNOSING — Claude AI diagnoses root cause and generates fix</li>
     *   <li>REWRITING  — AI patch written to .java source file on disk</li>
     *   <li>TESTING    — sandbox compiles patch and runs structural checks</li>
     *   <li>DEPLOYING  — patched source ready for next mvn test run</li>
     *   <li>HEALED     — audit log appended, Slack notification sent</li>
     * </ol>
     *
     * <p>This method is intentionally non-throwing. Any internal error is
     * caught and logged — the Cucumber runner is never affected.
     *
     * @param scenario the failed Cucumber scenario
     */
    @After(order = 1)
    public void healFailedStep(Scenario scenario) {
        if (!scenario.isFailed() || !HealingConfig.isEnabled()) {
            return;
        }

        log.info("[Hooks] Failure detected — initiating Self-Healing Engine...");

        try {
            String platform         = resolvePlatform(scenario);
            String pageSource       = "";
            String screenshotBase64 = "";

            // Capture DOM state — driver is already quit at this point (order=2 ran first),
            // so this is best-effort using saved state if available.
            // Override: if for any reason driver is still alive, capture it.
            if (DriverManager.isDriverInitialized()) {
                try {
                    pageSource       = DriverManager.getDriver().getPageSource();
                    screenshotBase64 = ScreenshotUtils.captureScreenshotAsBase64();
                } catch (Exception e) {
                    log.debug("[Hooks] Page state capture skipped: {}", e.getMessage());
                }
            }

            // Extract failed step metadata from scenario internals
            FailedStepMetadata meta = extractFailedStepMetadata(scenario, platform);

            if (meta.stepClass().isEmpty()) {
                log.warn("[Hooks] Could not determine failed step class — healing skipped.");
                return;
            }

            // Build full failure context for the AI engine
            HealingContext ctx = HealingContext.builder()
                .scenarioName(scenario.getName())
                .stepText(meta.stepText())
                .stepClass(meta.stepClass())
                .stepMethod(meta.stepMethod())
                .stepSourceCode(SourceCodeLocator.getSource(
                    HealingContext.builder()
                        .stepClass(meta.stepClass())
                        .stepMethod(meta.stepMethod())
                        .build()))
                .errorMessage(meta.errorMessage())
                .stackTrace(meta.stackTrace())
                .screenshotBase64(screenshotBase64)
                .pageSource(pageSource)
                .platform(platform)
                .failedAt(Instant.now())
                .retryCount(0)
                .build();

            HEALING_ENGINE.heal(ctx);

        } catch (Exception e) {
            // Healing must never crash the Cucumber runner
            log.warn("[Hooks] Self-healing hook error (non-fatal): {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Tag-specific hooks
    // ─────────────────────────────────────────────────────────

    @Before(value = "@web", order = 2)
    public void beforeWebScenario(Scenario scenario) {
        log.debug("Web hook triggered for: {}", scenario.getName());
    }

    @Before(value = "@mobile", order = 2)
    public void beforeMobileScenario(Scenario scenario) {
        log.debug("Mobile hook triggered for: {}", scenario.getName());
    }

    @Before(value = "@api", order = 2)
    public void beforeApiScenario(Scenario scenario) {
        log.debug("API hook triggered for: {}", scenario.getName());
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private String resolvePlatform(Scenario scenario) {
        if (scenario.getSourceTagNames().contains("@api"))    return "api";
        if (scenario.getSourceTagNames().contains("@mobile")) return "mobile";
        if (scenario.getSourceTagNames().contains("@web"))    return "web";
        return ConfigReader.get("platform", "web");
    }

    /**
     * Extracts metadata about the failed step using Cucumber's internal delegate.
     *
     * <p>Cucumber 7 wraps {@link Scenario} in a delegate holding the live
     * {@code TestCaseState}. We access the failure Throwable via reflection,
     * then walk its stack trace to find the first frame matching our
     * healing scope (e.g. {@code stepdefinitions.*}).
     *
     * <p>Falls back gracefully if reflection fails (e.g. across Cucumber patch versions).
     */
    private FailedStepMetadata extractFailedStepMetadata(Scenario scenario, String platform) {
        String stepText     = "Step details unavailable — see stack trace in audit log";
        String stepClass    = "";
        String stepMethod   = "";
        String errorMessage = "Unknown error";
        String stackTrace   = "";

        try {
            // Access Cucumber's internal TestCaseState via the delegate field
            java.lang.reflect.Field delegateField =
                scenario.getClass().getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(scenario);

            // Retrieve the Throwable that caused the failure
            java.lang.reflect.Method getErrorMethod =
                delegate.getClass().getMethod("getError");
            getErrorMethod.setAccessible(true);
            Throwable error = (Throwable) getErrorMethod.invoke(delegate);

            if (error != null) {
                errorMessage = error.getClass().getSimpleName() + ": " + error.getMessage();
                stackTrace   = buildStackTraceString(error);

                // Walk stack trace to find first frame in the healing scope
                String scope = HealingConfig.getScope();
                for (StackTraceElement frame : error.getStackTrace()) {
                    if (frame.getClassName().startsWith(scope)) {
                        stepClass  = frame.getClassName();
                        stepMethod = frame.getMethodName();
                        log.debug("[Hooks] Identified failed step: {}.{}()",
                            stepClass, stepMethod);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("[Hooks] Reflection-based step extraction failed: {}", ex.getMessage());
        }

        // Final fallback: infer class from platform tag if stack trace extraction failed
        if (stepClass.isEmpty()) {
            stepClass  = inferStepClassFromPlatform(platform);
            stepMethod = "unknown";
            log.debug("[Hooks] Using inferred step class: {}", stepClass);
        }

        return new FailedStepMetadata(stepText, stepClass, stepMethod, errorMessage, stackTrace);
    }

    /** Fallback: map platform to the most likely step definition class. */
    private String inferStepClassFromPlatform(String platform) {
        return switch (platform.toLowerCase()) {
            case "api"    -> "stepdefinitions.api.UserApiSteps";
            case "mobile" -> "stepdefinitions.mobile.MobileLoginSteps";
            default       -> "stepdefinitions.web.LoginSteps";
        };
    }

    /** Converts a Throwable's full stack trace to a string. */
    private String buildStackTraceString(Throwable t) {
        StringBuilder sb = new StringBuilder(t.toString()).append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("    at ").append(el).append("\n");
        }
        if (t.getCause() != null) {
            sb.append("Caused by: ").append(t.getCause());
        }
        return sb.toString();
    }

    /** Internal record carrying extracted failed-step metadata. */
    private record FailedStepMetadata(
        String stepText,
        String stepClass,
        String stepMethod,
        String errorMessage,
        String stackTrace
    ) {}
}
