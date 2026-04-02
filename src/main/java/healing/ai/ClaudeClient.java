package healing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import healing.core.HealingConfig;
import healing.core.HealingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client for the Anthropic Claude API.
 *
 * <p>Uses Java 17's built-in {@link HttpClient} — no extra OkHttp dependency needed
 * since the framework already targets Java 17.
 *
 * <p>Sends the broken step's source code and error details to Claude Sonnet,
 * instructing it to return a strictly structured JSON diagnosis.
 *
 * <p><b>Required environment variable:</b>
 * <pre>
 *   export ANTHROPIC_API_KEY=sk-ant-api03-...
 * </pre>
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class ClaudeClient {

    private static final Logger log = LogManager.getLogger(ClaudeClient.class);

    private static final String API_URL         = "https://api.anthropic.com/v1/messages";
//private static final String API_URL="https://openrouter.ai/api/v1/chat/completions";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClaudeClient() {
        this.httpClient   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Sends a broken step's context to Claude AI and returns a structured diagnosis.
     *
     * @param ctx the failure context captured by the interceptor
     * @return structured diagnosis with rootCause, fixDescription, and fixedCode
     * @throws HealingException if the API call fails or the response cannot be parsed
     */
    public DiagnosisResult diagnoseAndFix(HealingContext ctx) {
        log.info("[ClaudeClient] Sending failure context to Claude AI for: {}", ctx.getStepText());

        String apiKey = HealingConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new HealingException("ANTHROPIC_API_KEY environment variable is not set.");
        }

        String requestBody = buildRequestBody(ctx);
        log.debug("[ClaudeClient] Request body length: {} chars", requestBody.length());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(Duration.ofSeconds(60))
            .header("x-api-key",           apiKey)
            .header("anthropic-version",   ANTHROPIC_VERSION)
            .header("content-type",        "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(API_URL))
//                .timeout(Duration.ofSeconds(60))
//                .header("Authorization","Bearer "+apiKey)
////                .header("x-api-key",           apiKey)
////                .header("anthropic-version",   ANTHROPIC_VERSION)
//                .header("content-type",        "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new HealingException(
                    "Claude API returned HTTP " + response.statusCode() + ": " + response.body());
            }

            return parseResponse(response.body());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HealingException("Failed to call Claude API: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Request building
    // ─────────────────────────────────────────────────────────

    private String buildRequestBody(HealingContext ctx) {
        try {
            String systemPrompt = """
                You are an expert Java test automation engineer specializing in Selenium WebDriver \
                and Cucumber BDD frameworks.
                
                Your task: diagnose a failing Cucumber step definition and produce a corrected version.
                
                CRITICAL RULES:
                1. Return ONLY valid JSON — no markdown, no code fences, no explanation outside JSON.
                2. The "fixedCode" field must be the COMPLETE corrected Java method including its \
                @When/@Then/@Given annotation.
                3. Preserve the method signature and Cucumber annotation exactly.
                4. Do NOT change imports — only fix the method body.
                5. Prefer resilient locators: data-testid, aria-label, CSS selectors with fallbacks.
                6. Add explicit waits where timeouts are the cause.
                7. Add null checks where NullPointerExceptions are the cause.
                
                Respond with this exact JSON structure:
                {
                  "rootCause":      "One sentence: why it failed",
                  "fixDescription": "One sentence: what you changed",
                  "fixedCode":      "Complete corrected Java method as a single string with \\n newlines",
                  "confidence":     "HIGH | MEDIUM | LOW"
                }
                """;

            // Trim page source to avoid huge payloads
            String pageSourceSnippet = ctx.getPageSource() != null
                ? ctx.getPageSource().substring(0, Math.min(ctx.getPageSource().length(), 2000))
                : "Not available";

            String stackTraceSnippet = ctx.getStackTrace() != null
                ? ctx.getStackTrace().lines().limit(8).reduce("", (a, b) -> a + b + "\n")
                : "Not available";

            String userMessage = String.format("""
                FAILING CUCUMBER SCENARIO
                ──────────────────────────
                Scenario  : %s
                Step      : %s
                Platform  : %s
                
                BROKEN STEP DEFINITION CLASS: %s
                METHOD: %s
                
                SOURCE CODE:
                %s
                
                ERROR:
                %s
                
                STACK TRACE (first 8 lines):
                %s
                
                PAGE SOURCE SNIPPET (first 2000 chars):
                %s
                """,
                ctx.getScenarioName(),
                ctx.getStepText(),
                ctx.getPlatform(),
                ctx.getStepClass(),
                ctx.getStepMethod(),
                ctx.getStepSourceCode(),
                ctx.getErrorMessage(),
                stackTraceSnippet,
                pageSourceSnippet
            );

            Map<String, Object> body = Map.of(
                "model",      HealingConfig.getModel(),
                "max_tokens", HealingConfig.getMaxTokens(),
                "system",     systemPrompt,
                "messages",   new Object[]{
                    Map.of("role", "user", "content", userMessage)
                }
            );

            return objectMapper.writeValueAsString(body);

        } catch (Exception e) {
            throw new HealingException("Failed to build Claude API request body: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Response parsing
    // ─────────────────────────────────────────────────────────

    private DiagnosisResult parseResponse(String responseBody) {
        try {
            JsonNode root    = objectMapper.readTree(responseBody);
            String rawText   = root.path("content").get(0).path("text").asText();

            log.debug("[ClaudeClient] Raw AI response: {}", rawText);

            // Strip any accidental markdown fences Claude may have added
            String json = rawText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*",     "")
                .trim();

            DiagnosisResult result = objectMapper.readValue(json, DiagnosisResult.class);

            log.info("[ClaudeClient] Diagnosis complete. Root cause: {}", result.getRootCause());
            log.info("[ClaudeClient] Confidence: {}", result.getConfidence());

            return result;

        } catch (Exception e) {
            throw new HealingException(
                "Failed to parse Claude API response into DiagnosisResult: " + e.getMessage() +
                " | Raw body: " + responseBody.substring(0, Math.min(responseBody.length(), 500)), e);
        }
    }
}
