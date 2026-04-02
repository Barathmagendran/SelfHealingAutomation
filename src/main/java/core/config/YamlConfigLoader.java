package core.config;

import core.config.model.BrowserConfig;
import core.config.model.FrameworkConfig;
import core.config.model.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

/**
 * Singleton loader for all three YAML configuration files.
 *
 * <p>Loads and caches typed configuration objects at startup:
 * <ul>
 *   <li>{@code configs/framework.yml} → {@link FrameworkConfig}</li>
 *   <li>{@code configs/browser.yml}   → {@link BrowserConfig}</li>
 *   <li>{@code testdata/yaml/testData.yml} → {@link TestData}</li>
 * </ul>
 *
 * <p>All files are loaded from the classpath once and held in memory.
 * Thread-safe via double-checked locking on each singleton instance.
 *
 * <p>Usage:
 * <pre>
 *   // Framework settings
 *   int timeout = YamlConfigLoader.getFramework()
 *                     .getFramework().getTimeouts().getExplicit();
 *
 *   // Browser settings
 *   List&lt;String&gt; args = YamlConfigLoader.getBrowser()
 *                           .getBrowser().getChrome().getArguments();
 *
 *   // Test data
 *   String email = YamlConfigLoader.getTestData()
 *                      .getUsers().get("adminUser").getEmail();
 * </pre>
 *
 * <p>For convenience, prefer {@link YamlDataReader} which exposes
 * the same data through shorter accessor methods.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class YamlConfigLoader {

    private static final Logger log = LogManager.getLogger(YamlConfigLoader.class);

    // Classpath paths for each YAML file
    private static final String FRAMEWORK_YML = "configs/framework.yml";
    private static final String BROWSER_YML   = "configs/browser.yml";
    private static final String TESTDATA_YML  = "testdata/yaml/testData.yml";

    // Singleton instances — loaded once, reused everywhere
    private static volatile FrameworkConfig frameworkConfig;
    private static volatile BrowserConfig   browserConfig;
    private static volatile TestData        testData;

    private YamlConfigLoader() {}

    // ─────────────────────────────────────────────────────────
    // Public accessors
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the loaded {@link FrameworkConfig} from {@code framework.yml}.
     * Lazy-initializes on first call; cached thereafter.
     *
     * @return FrameworkConfig instance
     */
    public static FrameworkConfig getFramework() {
        if (frameworkConfig == null) {
            synchronized (YamlConfigLoader.class) {
                if (frameworkConfig == null) {
                    frameworkConfig = load(FRAMEWORK_YML, FrameworkConfig.class);
                    log.info("framework.yml loaded. Environment: [{}], Platform: [{}]",
                        frameworkConfig.getFramework().getEnvironment(),
                        frameworkConfig.getFramework().getPlatform());
                }
            }
        }
        return frameworkConfig;
    }

    /**
     * Returns the loaded {@link BrowserConfig} from {@code browser.yml}.
     * Lazy-initializes on first call; cached thereafter.
     *
     * @return BrowserConfig instance
     */
    public static BrowserConfig getBrowser() {
        if (browserConfig == null) {
            synchronized (YamlConfigLoader.class) {
                if (browserConfig == null) {
                    browserConfig = load(BROWSER_YML, BrowserConfig.class);
                    log.info("browser.yml loaded. Active browser: [{}]",
                        browserConfig.getBrowser().getActive());
                }
            }
        }
        return browserConfig;
    }

    /**
     * Returns the loaded {@link TestData} from {@code testData.yml}.
     * Lazy-initializes on first call; cached thereafter.
     *
     * @return TestData instance
     */
    public static TestData getTestData() {
        if (testData == null) {
            synchronized (YamlConfigLoader.class) {
                if (testData == null) {
                    testData = load(TESTDATA_YML, TestData.class);
                    log.info("testData.yml loaded. Users: [{}], Products: [{}]",
                        testData.getUsers().size(),
                        testData.getProducts().size());
                }
            }
        }
        return testData;
    }

    /**
     * Forces a reload of all YAML files from disk.
     * Useful in tests that modify YAML data or in multi-suite runs.
     * Normally not needed — configs are immutable during a run.
     */
    public static synchronized void reload() {
        log.info("Reloading all YAML configuration files...");
        frameworkConfig = null;
        browserConfig   = null;
        testData        = null;
        // Trigger reload on next access
        getFramework();
        getBrowser();
        getTestData();
        log.info("YAML reload complete.");
    }

    // ─────────────────────────────────────────────────────────
    // Core loader
    // ─────────────────────────────────────────────────────────

    /**
     * Loads a YAML file from the classpath and deserializes it into the given type.
     *
     * <p>Uses SnakeYAML 2.x with {@link Constructor} for type-safe deserialization.
     *
     * @param classpathPath classpath-relative path to the YAML file
     * @param type          target class to deserialize into
     * @param <T>           type parameter
     * @return deserialized config object
     * @throws RuntimeException if the file is not found or parsing fails
     */
    private static <T> T load(String classpathPath, Class<T> type) {
        log.debug("Loading YAML: [{}] as [{}]", classpathPath, type.getSimpleName());

        try (InputStream is = YamlConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(classpathPath)) {

            if (is == null) {
                throw new RuntimeException(
                    "YAML config not found on classpath: " + classpathPath +
                    "\nCheck that the file exists and Maven resources are configured correctly.");
            }

            // SnakeYAML 2.x API: Constructor requires LoaderOptions
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setAllowDuplicateKeys(false);  // Fail fast on duplicate keys
            loaderOptions.setMaxAliasesForCollections(50);

            Yaml yaml = new Yaml(new Constructor(type, loaderOptions));
            T result = yaml.load(is);

            if (result == null) {
                throw new RuntimeException("YAML file is empty: " + classpathPath);
            }

            log.debug("Successfully loaded: [{}]", classpathPath);
            return result;

        } catch (RuntimeException e) {
            throw e; // re-throw as-is
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to parse YAML file [" + classpathPath + "]: " + e.getMessage(), e);
        }
    }
}
