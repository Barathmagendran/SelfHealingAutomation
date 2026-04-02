package core.utils;

import core.config.ConfigReader;
import core.config.YamlConfigLoader;
import core.driver.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Centralized URL navigation utility for the entire framework.
 *
 * <p>Provides a single, consistent place for all URL launch operations.
 * All URL values are resolved from the config priority chain:
 * <pre>
 *   -Dbase.url=...  >  {env}.properties  >  config.properties  >  framework.yml urls.web
 * </pre>
 *
 * <p><b>Usage examples:</b>
 * <pre>
 *   // Launch application root
 *   NavigationUtils.launchUrl();
 *
 *   // Launch a specific page relative to base URL
 *   NavigationUtils.launchPage("/login");
 *   NavigationUtils.launchPage("/dashboard");
 *
 *   // Launch an absolute URL (overrides base URL)
 *   NavigationUtils.launchAbsoluteUrl("https://other.example.com/page");
 *
 *   // Navigate browser history
 *   NavigationUtils.back();
 *   NavigationUtils.forward();
 *   NavigationUtils.refresh();
 *
 *   // Check current location
 *   String url = NavigationUtils.getCurrentUrl();
 *   boolean isOn = NavigationUtils.isOnPage("/login");
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public final class NavigationUtils {

    private static final Logger log = LogManager.getLogger(NavigationUtils.class);

    private NavigationUtils() {}

    // ─────────────────────────────────────────────────────────
    // Core launch methods
    // ─────────────────────────────────────────────────────────

    /**
     * Launches the application base URL for the active environment.
     *
     * <p>URL resolved from (highest priority first):
     * <ol>
     *   <li>{@code -Dbase.url=https://...}</li>
     *   <li>{@code base.url} in {@code configs/{env}.properties}</li>
     *   <li>{@code base.url} in {@code config.properties}</li>
     *   <li>{@code framework.yml > urls.web}</li>
     * </ol>
     *
     * <p>Called automatically by {@code Hooks.beforeScenario()} for {@code @web} scenarios,
     * and by {@code BaseTest.setUp()} for TestNG tests. Can also be called explicitly
     * from step definitions when a test needs to restart from the root URL.
     */
    public static void launchUrl() {
        String baseUrl = resolveBaseUrl();
        log.info("Launching base URL: [{}]", baseUrl);
        getDriver().get(baseUrl);
        log.info("Page loaded: [{}]", getDriver().getTitle());
    }

    /**
     * Navigates to a page relative to the application base URL.
     *
     * <p>The {@code path} is appended directly to the base URL.
     * Leading slash is handled automatically.
     *
     * <pre>
     *   NavigationUtils.launchPage("/login");      // → https://app.qa.com/login
     *   NavigationUtils.launchPage("dashboard");   // → https://app.qa.com/dashboard
     *   NavigationUtils.launchPage("/admin/users");// → https://app.qa.com/admin/users
     * </pre>
     *
     * @param path page path relative to base URL (e.g., "/login", "/dashboard")
     */
    public static void launchPage(String path) {
        String baseUrl = resolveBaseUrl();
        // Normalise: strip trailing slash from base, ensure leading slash on path
        String normBase = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
        String normPath = path.startsWith("/") ? path : "/" + path;
        String fullUrl  = normBase + normPath;

        log.info("Launching page: [{}] → [{}]", path, fullUrl);
        getDriver().get(fullUrl);
        log.info("Page loaded: [{}]", getDriver().getTitle());
    }

    /**
     * Navigates to an absolute URL, bypassing the configured base URL entirely.
     * Use for cross-origin navigation or third-party service pages.
     *
     * @param absoluteUrl full URL including scheme (e.g., {@code https://example.com/page})
     * @throws IllegalArgumentException if the URL does not start with http/https
     */
    public static void launchAbsoluteUrl(String absoluteUrl) {
        if (absoluteUrl == null || absoluteUrl.isBlank()) {
            throw new IllegalArgumentException("launchAbsoluteUrl: URL must not be blank");
        }
        if (!absoluteUrl.startsWith("http://") && !absoluteUrl.startsWith("https://")) {
            throw new IllegalArgumentException(
                "launchAbsoluteUrl: URL must start with http:// or https://. Got: " + absoluteUrl);
        }
        log.info("Launching absolute URL: [{}]", absoluteUrl);
        getDriver().get(absoluteUrl);
        log.info("Page loaded: [{}]", getDriver().getTitle());
    }

    /**
     * Launches the API base URL in the browser — useful for API documentation
     * pages or Swagger UI during exploratory testing.
     *
     * @param path optional path suffix appended to the API base URL (use "" for root)
     */
    public static void launchApiUrl(String path) {
        String apiBase = resolveApiBaseUrl();
        String normBase = apiBase.endsWith("/")
            ? apiBase.substring(0, apiBase.length() - 1)
            : apiBase;
        String normPath = (path == null || path.isBlank()) ? ""
            : (path.startsWith("/") ? path : "/" + path);
        String fullUrl = normBase + normPath;

        log.info("Launching API URL: [{}]", fullUrl);
        getDriver().get(fullUrl);
    }

    // ─────────────────────────────────────────────────────────
    // Browser history navigation
    // ─────────────────────────────────────────────────────────

    /**
     * Navigates the browser back one step in history.
     */
    public static void back() {
        log.info("Browser: navigating back");
        getDriver().navigate().back();
    }

    /**
     * Navigates the browser forward one step in history.
     */
    public static void forward() {
        log.info("Browser: navigating forward");
        getDriver().navigate().forward();
    }

    /**
     * Refreshes the current page.
     */
    public static void refresh() {
        log.info("Browser: refreshing [{}]", getDriver().getCurrentUrl());
        getDriver().navigate().refresh();
    }

    // ─────────────────────────────────────────────────────────
    // URL inspection helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the current browser URL.
     *
     * @return current URL string
     */
    public static String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    /**
     * Returns the current page title.
     *
     * @return page title string
     */
    public static String getPageTitle() {
        return getDriver().getTitle();
    }

    /**
     * Returns the configured application base URL for the active environment.
     * Does not navigate — use {@link #launchUrl()} to actually navigate.
     *
     * @return base URL string
     */
    public static String getBaseUrl() {
        return resolveBaseUrl();
    }

    /**
     * Checks whether the current browser URL contains the given path fragment.
     *
     * <pre>
     *   NavigationUtils.isOnPage("/login")     // true when on https://app.qa.com/login
     *   NavigationUtils.isOnPage("/dashboard") // true when on dashboard
     * </pre>
     *
     * @param pathFragment URL fragment to check (e.g., "/login")
     * @return true if current URL contains the fragment
     */
    public static boolean isOnPage(String pathFragment) {
        String current = getCurrentUrl();
        boolean result = current.contains(pathFragment);
        log.debug("isOnPage([{}]): current=[{}] → {}", pathFragment, current, result);
        return result;
    }

    /**
     * Waits until the current URL contains the given fragment, up to the
     * explicit wait timeout configured in the framework.
     *
     * @param urlFragment the URL fragment to wait for
     */
    public static void waitForUrlContains(String urlFragment) {
        log.debug("Waiting for URL to contain: [{}]", urlFragment);
        WaitUtils.waitForUrlContains(urlFragment);
        log.info("URL now contains: [{}]", urlFragment);
    }

    /**
     * Waits until the page title contains the given text.
     *
     * @param titleFragment text expected in the page title
     */
    public static void waitForTitleContains(String titleFragment) {
        log.debug("Waiting for title to contain: [{}]", titleFragment);
        WaitUtils.waitForTitleContains(titleFragment);
        log.info("Title now contains: [{}]", titleFragment);
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Resolves the web application base URL from the config priority chain.
     * Falls back to framework.yml urls.web if not set in any properties file.
     */
    private static String resolveBaseUrl() {
        // 1. -Dbase.url system property
        String sysUrl = System.getProperty("base.url");
        if (sysUrl != null && !sysUrl.isBlank()) {
            log.debug("base.url resolved from system property");
            return sysUrl;
        }

        // 2. {env}.properties or config.properties
        String propUrl = ConfigReader.get("base.url", null);
        if (propUrl != null && !propUrl.isBlank()) {
            log.debug("base.url resolved from properties: [{}]", propUrl);
            return propUrl;
        }

        // 3. framework.yml urls.web
        String yamlUrl = YamlConfigLoader.getFramework().getFramework().getUrls().getWeb();
        if (yamlUrl != null && !yamlUrl.isBlank()) {
            log.debug("base.url resolved from framework.yml urls.web: [{}]", yamlUrl);
            return yamlUrl;
        }

        throw new RuntimeException(
            "base.url is not configured.\n" +
            "Set it via:\n" +
            "  1. -Dbase.url=https://yourapp.com  (runtime)\n" +
            "  2. base.url=https://... in configs/qa.properties\n" +
            "  3. urls.web: https://... in framework.yml");
    }

    /**
     * Resolves the API base URL from the config priority chain.
     */
    private static String resolveApiBaseUrl() {
        String sysUrl = System.getProperty("api.base.url");
        if (sysUrl != null && !sysUrl.isBlank()) return sysUrl;

        String propUrl = ConfigReader.get("api.base.url", null);
        if (propUrl != null && !propUrl.isBlank()) return propUrl;

        String yamlUrl = YamlConfigLoader.getFramework().getFramework().getUrls().getApi();
        if (yamlUrl != null && !yamlUrl.isBlank()) return yamlUrl;

        throw new RuntimeException("api.base.url is not configured.");
    }

    /** Returns the thread-local WebDriver, failing fast if not initialised. */
    private static WebDriver getDriver() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            throw new IllegalStateException(
                "NavigationUtils: WebDriver is not initialised for the current thread. " +
                "Ensure DriverManager.initDriver() has been called before navigating.");
        }
        return driver;
    }
}
