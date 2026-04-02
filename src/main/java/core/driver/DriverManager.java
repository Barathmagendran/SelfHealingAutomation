package core.driver;

import core.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Thread-safe WebDriver manager using {@link ThreadLocal} storage.
 *
 * <p>Supports parallel test execution by providing isolated driver instances
 * per thread. Drivers are lazily initialized and must be explicitly quit via
 * {@link #quitDriver()} to prevent resource leaks.
 *
 * <p><b>Thread Safety:</b> Each test thread gets its own WebDriver instance,
 * ensuring no interference between parallel test executions.
 *
 * <pre>
 * // Usage in test/step definition:
 * WebDriver driver = DriverManager.getDriver();
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    /**
     * ThreadLocal container holding one WebDriver per test thread.
     * Initialized lazily on first call to {@link #getDriver()}.
     */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Prevent instantiation — this is a utility/singleton class
    private DriverManager() {}

    /**
     * Returns the WebDriver for the current thread.
     * If no driver exists, initializes one based on the configured platform.
     *
     * @return WebDriver instance for the current thread
     */
    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            log.info("No driver found for thread [{}]. Initializing...", Thread.currentThread().getName());
            initDriver();
        }
        return driverThreadLocal.get();
    }

    /**
     * Sets a pre-created WebDriver instance for the current thread.
     * Useful for injecting mock drivers in unit tests.
     *
     * @param driver the WebDriver instance to set
     */
    public static void setDriver(WebDriver driver) {
        log.debug("Setting driver for thread [{}]", Thread.currentThread().getName());
        driverThreadLocal.set(driver);
    }

    /**
     * Initializes a WebDriver for the current thread based on configured platform.
     * Reads {@code platform} from system properties or {@code config.properties}.
     */
    public static void initDriver() {
        String platform = System.getProperty("platform",
            ConfigReader.get("platform", "web"));
        DriverType driverType = DriverType.fromString(platform);

        log.info("Initializing driver: type=[{}], thread=[{}]",
            driverType, Thread.currentThread().getName());

        WebDriver driver;

        switch (driverType) {
            case WEB:
                driver = WebDriverFactory.createDriver();
                break;
            case ANDROID:
            case IOS:
                driver = MobileDriverFactory.createDriver(driverType);
                break;
            case API:
                log.info("API platform — no WebDriver required.");
                return;
            default:
                throw new IllegalStateException("Unsupported DriverType: " + driverType);
        }

        driverThreadLocal.set(driver);
        log.info("Driver initialized successfully for thread [{}]", Thread.currentThread().getName());
    }

    /**
     * Quits the WebDriver for the current thread and removes it from ThreadLocal.
     * Must be called in the test teardown (e.g., Cucumber {@code @After} hook) to
     * prevent memory leaks, especially in parallel execution environments.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                log.info("Quitting driver for thread [{}]", Thread.currentThread().getName());
                driver.quit();
            } catch (Exception e) {
                log.warn("Exception while quitting driver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    /**
     * Checks whether a driver is currently active for the calling thread.
     *
     * @return true if a driver is initialized for the current thread
     */
    public static boolean isDriverInitialized() {
        return driverThreadLocal.get() != null;
    }
}
