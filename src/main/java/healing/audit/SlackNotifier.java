package healing.audit;

import healing.ai.DiagnosisResult;
import healing.core.HealingConfig;
import healing.core.HealingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends healing event notifications to a Slack channel via Incoming Webhooks.
 *
 * <p>Configure the webhook URL in {@code config.properties}:
 * <pre>
 *   healing.slack.webhook=https://hooks.slack.com/services/T00/B00/XXXX
 * </pre>
 *
 * <p>Leave blank to disable Slack notifications (no errors thrown).
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class SlackNotifier {

    private static final Logger log = LogManager.getLogger(SlackNotifier.class);
    private final HttpClient httpClient;

    public SlackNotifier() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Sends a success notification when a step is healed.
     */
    public void notifyHealed(HealingContext ctx, DiagnosisResult diagnosis, long healTimeMs) {
        String text = String.format(
            "✅ *Self-Healing Engine* — Step AUTO-HEALED in %dms\n" +
            ">*Scenario:* %s\n" +
            ">*Step:* `%s`\n" +
            ">*Root Cause:* %s\n" +
            ">*Fix Applied:* %s\n" +
            ">*Confidence:* %s | *Env:* %s",
            healTimeMs,
            ctx.getScenarioName(),
            ctx.getStepText(),
            diagnosis.getRootCause(),
            diagnosis.getFixDescription(),
            diagnosis.getConfidence(),
            core.config.ConfigReader.getActiveEnvironment().toUpperCase()
        );
        send(text);
    }

    /**
     * Sends an escalation alert when healing fails or retries are exhausted.
     */
    public void notifyEscalation(HealingContext ctx, String reason) {
        String text = String.format(
            "🚨 *Self-Healing Engine* — HUMAN INTERVENTION REQUIRED\n" +
            ">*Scenario:* %s\n" +
            ">*Step:* `%s`\n" +
            ">*Class:* `%s`\n" +
            ">*Reason:* %s\n" +
            ">*Retries exhausted:* %d | *Env:* %s",
            ctx.getScenarioName(),
            ctx.getStepText(),
            ctx.getStepClass(),
            reason,
            ctx.getRetryCount(),
            core.config.ConfigReader.getActiveEnvironment().toUpperCase()
        );
        send(text);
    }

    /**
     * Sends an internal error notification for unexpected healing engine failures.
     */
    public void notifyError(HealingContext ctx, Exception error) {
        String text = String.format(
            "⚠️ *Self-Healing Engine* — Internal Error\n" +
            ">*Step:* `%s`\n" +
            ">*Error:* %s: %s",
            ctx.getStepText(),
            error.getClass().getSimpleName(),
            error.getMessage()
        );
        send(text);
    }

    // ─────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────

    private void send(String text) {
        String webhook = HealingConfig.getSlackWebhook();
        if (webhook == null || webhook.isBlank()) {
            log.debug("[SlackNotifier] Webhook not configured — notification skipped.");
            return;
        }

        try {
            String payload = "{\"text\": " + escapeJson(text) + "}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.debug("[SlackNotifier] Notification sent successfully.");
            } else {
                log.warn("[SlackNotifier] Slack returned HTTP {}: {}",
                    response.statusCode(), response.body());
            }

        } catch (Exception e) {
            // Slack notifications are best-effort — never fail the healing cycle
            log.warn("[SlackNotifier] Failed to send notification: {}", e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            + "\"";
    }
}
