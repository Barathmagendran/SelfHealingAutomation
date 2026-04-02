package healing.core;

import core.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central configuration for the Self-Healing Engine.
 *
 * <p>All settings are read from the framework's standard config resolution chain:
 * JVM system properties → env properties → config.properties → hardcoded default.
 *
 * <p>Add these keys to {@code config.properties} or pass as {@code -D} flags:
 * <pre>
 *   healing.enabled=true
 *   healing.max.retries=3
 *   healing.scope=stepdefinitions
 *   healing.sandbox.timeout.ms=10000
 *   healing.audit.path=target/healing-audit.jsonl
 *   healing.slack.webhook=https://hooks.slack.com/...
 *   # ANTHROPIC_API_KEY must be set as an environment variable
 * </pre>
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class HealingConfig {

    private static final Logger log = LogManager.getLogger(HealingConfig.class);

    private HealingConfig() {}

    // ─────────────────────────────────────────────────────────
    // Master switch
    // ─────────────────────────────────────────────────────────

    /**
     * Master on/off switch for the healing engine.
     * Set {@code healing.enabled=false} to disable in CI environments if needed.
     */
    public static boolean isEnabled() {
        return Boolean.parseBoolean(ConfigReader.get("healing.enabled", "true"));
    }

    // ─────────────────────────────────────────────────────────
    // AI settings
    // ─────────────────────────────────────────────────────────

    /** Anthropic API key — loaded from the {@code ANTHROPIC_API_KEY} env variable. */
    public static String getApiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        return null;
    }

    /** Claude model to use for diagnosis and code generation. */
    public static String getModel() {
        return ConfigReader.get("healing.ai.model", "claude-sonnet-4-20250514");
    }

    /** Maximum tokens in the AI response (controls patch length). */
    public static int getMaxTokens() {
        return Integer.parseInt(ConfigReader.get("healing.ai.max.tokens", "2000"));
    }

    // ─────────────────────────────────────────────────────────
    // Healing behaviour
    // ─────────────────────────────────────────────────────────

    /** Maximum number of healing attempts before escalating to the team. */
    public static int getMaxRetries() {
        return Integer.parseInt(ConfigReader.get("healing.max.retries", "3"));
    }

    /**
     * Java package prefix to scope healing.
     * Only step classes within this package will be healed.
     * Example: {@code stepdefinitions} restricts healing to all step definition packages.
     */
    public static String getScope() {
        return ConfigReader.get("healing.scope", "stepdefinitions");
    }

    /**
     * Maximum number of lines an AI patch may change.
     * Patches larger than this are rejected as too risky.
     */
    public static int getMaxPatchLines() {
        return Integer.parseInt(ConfigReader.get("healing.max.patch.lines", "50"));
    }

    // ─────────────────────────────────────────────────────────
    // Sandbox
    // ─────────────────────────────────────────────────────────

    /** Timeout in milliseconds for sandbox compilation and test execution. */
    public static int getSandboxTimeoutMs() {
        return Integer.parseInt(ConfigReader.get("healing.sandbox.timeout.ms", "15000"));
    }

    // ─────────────────────────────────────────────────────────
    // Audit & notifications
    // ─────────────────────────────────────────────────────────

    /** Path to the JSONL audit log file. */
    public static String getAuditPath() {
        return ConfigReader.get("healing.audit.path", "target/healing-audit.jsonl");
    }

    /**
     * Optional Slack incoming-webhook URL for healing notifications.
     * Leave blank to disable Slack notifications.
     */
    public static String getSlackWebhook() {
        return ConfigReader.get("healing.slack.webhook", "");
    }
}
