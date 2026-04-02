package healing.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Structured result returned by Claude AI after diagnosing a broken step.
 *
 * <p>The {@link ClaudeClient} instructs Claude to respond strictly in JSON
 * matching this schema. The raw JSON is deserialized into this POJO
 * using Jackson's ObjectMapper.
 *
 * <p>Example Claude response:
 * <pre>
 * {
 *   "rootCause":       "Locator By.id('loginBtn') is stale after recent UI change",
 *   "fixDescription":  "Replaced brittle ID locator with resilient CSS selector using data-testid fallback",
 *   "fixedCode":       "@When(\"the user clicks the login button\")\npublic void theUserClicksLoginButton() {\n    ...\n}",
 *   "confidence":      "HIGH"
 * }
 * </pre>
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResult {

    /**
     * One-sentence explanation of why the step failed.
     * Example: "Locator By.id('loginBtn') is stale — the ID changed in the latest deploy."
     */
    private String rootCause;

    /**
     * Human-readable description of the fix applied.
     * Shown in the Extent Report and Slack notification.
     */
    private String fixDescription;

    /**
     * Complete corrected Java method as a string.
     * This is written back to the step definition source file verbatim.
     */
    private String fixedCode;

    /**
     * AI's confidence in the fix: "HIGH", "MEDIUM", or "LOW".
     * LOW confidence triggers human escalation even if sandbox tests pass.
     */
    private String confidence;

    /** @return true if the AI expressed high or medium confidence in the fix */
    public boolean isConfident() {
        return "HIGH".equalsIgnoreCase(confidence) || "MEDIUM".equalsIgnoreCase(confidence);
    }
}
