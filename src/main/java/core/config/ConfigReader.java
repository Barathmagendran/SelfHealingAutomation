package core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration reader with 4-tier resolution.
 *
 * <p><b>Resolution order (highest → lowest priority):</b>
 * <ol>
 *   <li>JVM system properties: {@code -Dkey=value}</li>
 *   <li>Environment-specific file: {@code configs/{env}.properties}</li>
 *   <li>Base file: {@code config.properties}</li>
 *   <li>Supplied default value</li>
 * </ol>
 *
 * <p>YAML files ({@code framework.yml}, {@code browser.yml}) are loaded
 * separately by {@link YamlConfigLoader} and accessed via typed models.
 * For convenience, {@link #getFromFrameworkYml} bridges YAML → string for
 * legacy callers that still use string-based config access.
 *
 * <p>Active environment is set by {@code -Denv=qa} (default: {@code qa}):
 * <pre>
 *   mvn test -Denv=uat -Dbrowser=chrome
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);

    private static final Properties baseProperties = new Properties();
    private static final Properties envProperties  = new Properties();

    private static final String ACTIVE_ENV =
        System.getProperty("env", "qa").toLowerCase();

    static {
        loadBaseConfig();
        loadEnvConfig(ACTIVE_ENV);
        log.info("ConfigReader initialized. Active environment: [{}]", ACTIVE_ENV);
    }

    private ConfigReader() {}

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Resolves a config value using the 4-tier priority chain.
     *
     * @param key          property key
     * @param defaultValue fallback when not found in any source
     * @return resolved string value
     */
    public static String get(String key, String defaultValue) {
        // 1. JVM system property — highest priority
        String sysVal = System.getProperty(key);
        if (sysVal != null && !sysVal.isBlank()) {
            log.trace("Key [{}] ← system property: [{}]", key, sysVal);
            return sysVal;
        }

        // 2. Environment-specific .properties
        String envVal = envProperties.getProperty(key);
        if (envVal != null && !envVal.isBlank()) {
            log.trace("Key [{}] ← {}.properties: [{}]", key, ACTIVE_ENV, envVal);
            return envVal;
        }

        // 3. Base config.properties
        String baseVal = baseProperties.getProperty(key);
        if (baseVal != null && !baseVal.isBlank()) {
            log.trace("Key [{}] ← config.properties: [{}]", key, baseVal);
            return baseVal;
        }

        // 4. Supplied default
        log.debug("Key [{}] not found — using default: [{}]", key, defaultValue);
        return defaultValue;
    }

    /**
     * Retrieves a required config value.
     * Throws {@link RuntimeException} if not found in any source.
     *
     * @param key the property key
     * @return resolved value
     */
    public static String getRequired(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new RuntimeException(
                "Required config key [" + key + "] not found. " +
                "Check config.properties, configs/" + ACTIVE_ENV + ".properties, or pass -D" + key + "=<value>");
        }
        return value;
    }

    /**
     * Returns the base URL for the active environment.
     * Resolved from properties; falls back to YAML urls.web if needed.
     */
    public static String getBaseUrl() {
        String url = get("base.url", null);
        if (url == null || url.isBlank()) {
            // Bridge to framework.yml urls.web
            url = YamlConfigLoader.getFramework().getFramework().getUrls().getWeb();
        }
        if (url == null || url.isBlank()) {
            throw new RuntimeException(
                "base.url is not configured. Set it in config.properties or framework.yml > urls.web");
        }
        return url;
    }

    /**
     * Returns the API base URL for the active environment.
     * Resolved from properties; falls back to YAML urls.api if needed.
     */
    public static String getApiBaseUrl() {
        String url = get("api.base.url", null);
        if (url == null || url.isBlank()) {
            url = YamlConfigLoader.getFramework().getFramework().getUrls().getApi();
        }
        if (url == null || url.isBlank()) {
            throw new RuntimeException(
                "api.base.url is not configured. Set it in config.properties or framework.yml > urls.api");
        }
        return url;
    }

    /** @return active environment name (e.g., "qa", "uat", "prod") */
    public static String getActiveEnvironment() {
        return ACTIVE_ENV;
    }

    /**
     * Convenience bridge: read a value from {@code framework.yml} as a String.
     * Useful for legacy code that accesses typed YAML values via string keys.
     *
     * <p>Supported shortcut keys:
     * <ul>
     *   <li>{@code "explicit.wait"} → framework.timeouts.explicit</li>
     *   <li>{@code "implicit.wait"} → framework.timeouts.implicit</li>
     *   <li>{@code "page.load.timeout"} → framework.timeouts.page_load</li>
     *   <li>{@code "retry.count"} → framework.retry.count</li>
     *   <li>{@code "screenshots.on.failure"} → framework.reporting.screenshots.on_failure</li>
     * </ul>
     *
     * @param key          shortcut key
     * @param defaultValue fallback
     * @return value from framework.yml, or defaultValue if key not recognised
     */
    public static String getFromFrameworkYml(String key, String defaultValue) {
        try {
            var fw = YamlConfigLoader.getFramework().getFramework();
            return switch (key) {
                case "explicit.wait"         -> String.valueOf(fw.getTimeouts().getExplicit());
                case "implicit.wait"         -> String.valueOf(fw.getTimeouts().getImplicit());
                case "page.load.timeout"     -> String.valueOf(fw.getTimeouts().getPage_load());
                case "script.timeout"        -> String.valueOf(fw.getTimeouts().getScript());
                case "retry.count"           -> String.valueOf(fw.getRetry().getCount());
                case "retry.delay.ms"        -> String.valueOf(fw.getRetry().getDelay_ms());
                case "screenshots.on.failure"-> String.valueOf(fw.getReporting().getScreenshots().isOn_failure());
                case "screenshot.path"       -> fw.getReporting().getScreenshots().getOutput_dir();
                case "log.level"             -> fw.getLogging().getLevel();
                case "parallel.threads"      -> String.valueOf(fw.getParallel().getThread_count());
                case "testdata.path"         -> fw.getTestdata().getBase_path();
                default                      -> defaultValue;
            };
        } catch (Exception e) {
            log.debug("Could not read key [{}] from framework.yml: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    // ─────────────────────────────────────────────────────────
    // Private loaders
    // ─────────────────────────────────────────────────────────

    private static void loadBaseConfig() {
        loadPropertiesFile("config.properties", baseProperties, true);
    }

    private static void loadEnvConfig(String env) {
        loadPropertiesFile("configs/" + env + ".properties", envProperties, false);
    }

    private static void loadPropertiesFile(String path, Properties target, boolean required) {
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                if (required) throw new RuntimeException(
                    "Required config file not found on classpath: " + path);
                log.info("Optional config not found, skipping: [{}]", path);
                return;
            }
            target.load(is);
            log.info("Loaded config: [{}] ({} properties)", path, target.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + path, e);
        }
    }
}
