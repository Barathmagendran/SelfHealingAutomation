package healing.core;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Immutable snapshot of a Cucumber step failure.
 *
 * <p>Captured by {@link StepFailureInterceptor} in the {@code @After} hook
 * and passed to the {@link HealingEngine} for diagnosis and repair.
 *
 * <p>Contains everything Claude AI needs to understand the failure:
 * <ul>
 *   <li>The broken Java source code</li>
 *   <li>The exception message and stack trace</li>
 *   <li>DOM screenshot and page source at the moment of failure</li>
 *   <li>Cucumber scenario metadata for audit logging</li>
 * </ul>
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
@Data
@Builder
public class HealingContext {

    /** Cucumber scenario name, e.g. "User logs in with valid credentials" */
    private final String scenarioName;

    /** Gherkin step text, e.g. "When the user clicks the login button" */
    private final String stepText;

    /** Fully qualified class name of the step definition, e.g. "stepdefinitions.web.LoginSteps" */
    private final String stepClass;

    /** Method name of the broken step, e.g. "theUserClicksLoginButton" */
    private final String stepMethod;

    /** Full source code of the broken method body, extracted by {@link healing.repair.SourceCodeLocator} */
    private final String stepSourceCode;

    /** Exception class and message, e.g. "NoSuchElementException: Unable to find element: #loginBtn" */
    private final String errorMessage;

    /** Full stack trace string (first 10 lines are passed to AI) */
    private final String stackTrace;

    /** Base64-encoded PNG screenshot captured at the moment of failure (may be null for API steps) */
    private final String screenshotBase64;

    /** Raw page HTML at failure time (used by AI for context) */
    private final String pageSource;

    /** Execution platform derived from scenario tags: "web", "mobile", or "api" */
    private final String platform;

    /** Timestamp of failure */
    private final Instant failedAt;

    /** How many healing attempts have been made for this context (0 on first attempt) */
    @Builder.Default
    private final int retryCount = 0;
}
