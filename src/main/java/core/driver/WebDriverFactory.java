package core.driver;

import core.config.ConfigReader;
import core.config.YamlConfigLoader;
import core.config.model.BrowserConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.safari.SafariDriver;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating configured {@link WebDriver} instances.
 *
 * <p>All browser settings — arguments, headless flags, preferences, capabilities —
 * are sourced from {@code configs/browser.yml} via {@link YamlConfigLoader}.
 * Runtime CLI overrides ({@code -Dbrowser=}, {@code -Dheadless=}) still take
 * highest priority over YAML values.
 *
 * <p>Supported browsers: Chrome, Firefox, Edge, Safari.
 *
 * <pre>
 *   mvn test -Dbrowser=firefox -Dheadless=true
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class WebDriverFactory {

    private static final Logger log = LogManager.getLogger(WebDriverFactory.class);

    public static final String CHROME  = "chrome";
    public static final String FIREFOX = "firefox";
    public static final String EDGE    = "edge";
    public static final String SAFARI  = "safari";

    private WebDriverFactory() {}

    /**
     * Creates a WebDriver using the active browser resolved from:
     * {@code -Dbrowser} → {@code config.properties} → {@code browser.yml active} field.
     *
     * @return configured and ready-to-use {@link WebDriver}
     */
    public static WebDriver createDriver() {
        BrowserConfig.BrowserSection browserSection =
            YamlConfigLoader.getBrowser().getBrowser();

        // Priority: -Dbrowser > config.properties > browser.yml active
        String browser = System.getProperty("browser",
            ConfigReader.get("browser", browserSection.getActive()))
            .toLowerCase().trim();

        // Priority: -Dheadless > config.properties > browser.yml defaults.headless
        boolean headless = Boolean.parseBoolean(
            System.getProperty("headless",
                ConfigReader.get("headless",
                    String.valueOf(browserSection.getDefaults().isHeadless()))));

        log.info("Creating WebDriver — browser=[{}], headless=[{}]", browser, headless);

        WebDriver driver = switch (browser) {
            case CHROME  -> createChromeDriver(browserSection.getChrome(), headless);
            case FIREFOX -> createFirefoxDriver(browserSection.getFirefox(), headless);
            case EDGE    -> createEdgeDriver(browserSection.getEdge(), headless);
            case SAFARI  -> createSafariDriver(browserSection.getSafari());
            default -> throw new IllegalArgumentException(
                "Unsupported browser: '" + browser + "'. Valid: chrome, firefox, edge, safari");
        };

        configureTimeouts(driver);
        driver.manage().window().maximize();
        return driver;
    }

    // ─────────────────────────────────────────────────────────
    // Chrome
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a ChromeDriver with arguments and preferences sourced from browser.yml.
     * Switches to the headless_arguments list when headless=true.
     */
    private static WebDriver createChromeDriver(BrowserConfig.ChromeConfig cfg, boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        // Use headless or normal argument list from browser.yml
        List<String> args = headless ? cfg.getHeadless_arguments() : cfg.getArguments();
        if (args != null && !args.isEmpty()) {
            options.addArguments(args);
            log.debug("Chrome args: {}", args);
        }

        // User preferences (download dir, password manager, etc.)
        Map<String, Object> prefs = cfg.getPreferences();
        if (prefs != null && !prefs.isEmpty()) {
            options.setExperimentalOption("prefs", prefs);
            log.debug("Chrome prefs applied: {} keys", prefs.size());
        }

        // Experimental options (excludeSwitches, useAutomationExtension, etc.)
        Map<String, Object> experimental = cfg.getExperimental_options();
        if (experimental != null && !experimental.isEmpty()) {
            experimental.forEach(options::setExperimentalOption);
            log.debug("Chrome experimental options applied: {}", experimental.keySet());
        }

        options.setAcceptInsecureCerts(true);
        System.setProperty("webdriver.chrome.silentOutput", "true");

        if (headless) log.info("Chrome running in headless mode.");
        return new ChromeDriver(options);
    }

    // ─────────────────────────────────────────────────────────
    // Firefox
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a FirefoxDriver with a custom profile and arguments from browser.yml.
     */
    private static WebDriver createFirefoxDriver(BrowserConfig.FirefoxConfig cfg, boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();

        List<String> args = headless ? cfg.getHeadless_arguments() : cfg.getArguments();
        if (args != null && !args.isEmpty()) {
            options.addArguments(args);
            log.debug("Firefox args: {}", args);
        }

        // Map YAML preferences → Firefox profile
        Map<String, Object> prefs = cfg.getPreferences();
        if (prefs != null && !prefs.isEmpty()) {
            FirefoxProfile profile = new FirefoxProfile();
            prefs.forEach((key, value) -> {
                if (value instanceof Boolean b)       profile.setPreference(key, b);
                else if (value instanceof Integer i)  profile.setPreference(key, i);
                else                                   profile.setPreference(key, String.valueOf(value));
            });
            options.setProfile(profile);
            log.debug("Firefox profile prefs applied: {} keys", prefs.size());
        }

        options.setAcceptInsecureCerts(true);
        if (headless) log.info("Firefox running in headless mode.");
        return new FirefoxDriver(options);
    }

    // ─────────────────────────────────────────────────────────
    // Edge
    // ─────────────────────────────────────────────────────────

    /**
     * Creates an EdgeDriver (Chromium-based) with arguments from browser.yml.
     */
    private static WebDriver createEdgeDriver(BrowserConfig.EdgeConfig cfg, boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();

        List<String> args = headless ? cfg.getHeadless_arguments() : cfg.getArguments();
        if (args != null && !args.isEmpty()) {
            options.addArguments(args);
        }

        options.setAcceptInsecureCerts(true);
        if (headless) log.info("Edge running in headless mode.");
        return new EdgeDriver(options);
    }

    // ─────────────────────────────────────────────────────────
    // Safari
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a SafariDriver. Safari never runs headless regardless of config.
     * Requires: {@code safaridriver --enable} run once as admin on macOS.
     */
    private static WebDriver createSafariDriver(BrowserConfig.SafariConfig cfg) {
        if (cfg.isHeadless()) {
            log.warn("browser.yml sets safari.headless=true — Safari does NOT support " +
                "headless mode. Ignoring. Running in GUI mode.");
        }
        log.info("Creating SafariDriver. Ensure 'Allow Remote Automation' is enabled in Safari > Develop menu.");
        return new SafariDriver();
    }

    // ─────────────────────────────────────────────────────────
    // Common timeout config
    // ─────────────────────────────────────────────────────────

    /**
     * Applies page-load, implicit, and script timeouts.
     * Values read from ConfigReader (which overlays framework.yml via system properties).
     */
    private static void configureTimeouts(WebDriver driver) {
        int implicitWait    = Integer.parseInt(ConfigReader.get("implicit.wait",    "0"));
        int pageLoadTimeout = Integer.parseInt(ConfigReader.get("page.load.timeout","30"));
        int scriptTimeout   = Integer.parseInt(ConfigReader.get("script.timeout",   "30"));

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeout));

        log.debug("Driver timeouts — implicit={}s, pageLoad={}s, script={}s",
            implicitWait, pageLoadTimeout, scriptTimeout);
    }
}
