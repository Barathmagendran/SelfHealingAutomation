package tests;

import core.config.YamlConfigLoader;
import core.config.YamlDataReader;
import core.config.model.BrowserConfig;
import core.config.model.FrameworkConfig;
import core.config.model.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit/integration tests validating that all YAML config files
 * load correctly and their values are accessible via the typed models.
 *
 * <p>Run with:
 * <pre>
 *   mvn test -Dtest=YamlConfigLoaderTest
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class YamlConfigLoaderTest {

    private static final Logger log = LogManager.getLogger(YamlConfigLoaderTest.class);

    @BeforeClass
    public void setup() {
        log.info("Starting YAML config loader tests...");
    }

    // ─────────────────────────────────────────────────────────
    // framework.yml tests
    // ─────────────────────────────────────────────────────────

    @Test(description = "framework.yml loads without errors")
    public void testFrameworkYmlLoads() {
        FrameworkConfig cfg = YamlConfigLoader.getFramework();
        Assert.assertNotNull(cfg, "FrameworkConfig must not be null");
        Assert.assertNotNull(cfg.getFramework(), "framework section must not be null");
        log.info("framework.yml loaded OK. env={}", cfg.getFramework().getEnvironment());
    }

    @Test(description = "framework.yml timeouts have valid positive values")
    public void testFrameworkTimeouts() {
        var timeouts = YamlConfigLoader.getFramework().getFramework().getTimeouts();
        Assert.assertTrue(timeouts.getExplicit() > 0,
            "explicit wait must be > 0, got: " + timeouts.getExplicit());
        Assert.assertTrue(timeouts.getPage_load() > 0,
            "page_load timeout must be > 0");
        Assert.assertEquals(timeouts.getImplicit(), 0,
            "implicit wait should be 0 (use explicit waits only)");
        log.info("Timeouts OK: explicit={}s, pageLoad={}s", timeouts.getExplicit(), timeouts.getPage_load());
    }

    @Test(description = "framework.yml retry config is valid")
    public void testFrameworkRetryConfig() {
        var retry = YamlConfigLoader.getFramework().getFramework().getRetry();
        Assert.assertTrue(retry.getCount() >= 0,
            "retry count must be >= 0");
        Assert.assertTrue(retry.getDelay_ms() >= 0,
            "retry delay must be >= 0");
        log.info("Retry OK: count={}, delay={}ms", retry.getCount(), retry.getDelay_ms());
    }

    @Test(description = "framework.yml reporting config has non-blank output path")
    public void testFrameworkReportingConfig() {
        var reporting = YamlConfigLoader.getFramework().getFramework().getReporting();
        Assert.assertNotNull(reporting.getExtent().getOutput(),
            "extent report output path must not be null");
        Assert.assertFalse(reporting.getExtent().getOutput().isBlank(),
            "extent report output path must not be blank");
        log.info("Reporting OK: output={}", reporting.getExtent().getOutput());
    }

    @Test(description = "framework.yml appium server config is valid")
    public void testFrameworkAppiumConfig() {
        var appium = YamlConfigLoader.getFramework().getFramework().getAppium();
        Assert.assertNotNull(appium.getServer().getHost(),
            "Appium server host must not be null");
        Assert.assertTrue(appium.getServer().getPort() > 0,
            "Appium server port must be > 0");
        log.info("Appium OK: host={}, port={}",
            appium.getServer().getHost(), appium.getServer().getPort());
    }

    // ─────────────────────────────────────────────────────────
    // browser.yml tests
    // ─────────────────────────────────────────────────────────

    @Test(description = "browser.yml loads without errors")
    public void testBrowserYmlLoads() {
        BrowserConfig cfg = YamlConfigLoader.getBrowser();
        Assert.assertNotNull(cfg, "BrowserConfig must not be null");
        Assert.assertNotNull(cfg.getBrowser(), "browser section must not be null");
        log.info("browser.yml loaded OK. active={}", cfg.getBrowser().getActive());
    }

    @Test(description = "browser.yml chrome config has non-empty arguments list")
    public void testChromeArguments() {
        List<String> args = YamlConfigLoader.getBrowser().getBrowser().getChrome().getArguments();
        Assert.assertNotNull(args, "Chrome arguments list must not be null");
        Assert.assertFalse(args.isEmpty(), "Chrome arguments must not be empty");
        Assert.assertTrue(args.contains("--no-sandbox"),
            "Chrome args should contain --no-sandbox for CI stability");
        log.info("Chrome args OK: {} arguments", args.size());
    }

    @Test(description = "browser.yml chrome headless arguments are defined")
    public void testChromeHeadlessArguments() {
        List<String> args = YamlConfigLoader.getBrowser().getBrowser().getChrome().getHeadless_arguments();
        Assert.assertNotNull(args, "Chrome headless_arguments must not be null");
        Assert.assertFalse(args.isEmpty(), "Chrome headless args must not be empty");
        boolean hasHeadlessFlag = args.stream().anyMatch(a -> a.contains("--headless"));
        Assert.assertTrue(hasHeadlessFlag, "Headless args must contain a --headless flag");
        log.info("Chrome headless args OK: {}", args);
    }

    @Test(description = "browser.yml firefox config has preferences defined")
    public void testFirefoxPreferences() {
        var prefs = YamlConfigLoader.getBrowser().getBrowser().getFirefox().getPreferences();
        Assert.assertNotNull(prefs, "Firefox preferences map must not be null");
        Assert.assertFalse(prefs.isEmpty(), "Firefox preferences must not be empty");
        log.info("Firefox prefs OK: {} keys", prefs.size());
    }

    @Test(description = "browser.yml safari headless is always false")
    public void testSafariHeadlessAlwaysFalse() {
        boolean safariHeadless = YamlConfigLoader.getBrowser().getBrowser().getSafari().isHeadless();
        Assert.assertFalse(safariHeadless, "Safari headless must be false — Safari does not support it");
        log.info("Safari headless=false correctly configured.");
    }

    @Test(description = "browser.yml active browser is a valid value")
    public void testActiveBrowserIsValid() {
        String active = YamlConfigLoader.getBrowser().getBrowser().getActive();
        List<String> valid = List.of("chrome", "firefox", "edge", "safari");
        Assert.assertTrue(valid.contains(active),
            "active browser [" + active + "] must be one of: " + valid);
        log.info("Active browser: {}", active);
    }

    // ─────────────────────────────────────────────────────────
    // testData.yml tests
    // ─────────────────────────────────────────────────────────

    @Test(description = "testData.yml loads without errors")
    public void testTestDataYmlLoads() {
        TestData td = YamlConfigLoader.getTestData();
        Assert.assertNotNull(td, "TestData must not be null");
        Assert.assertFalse(td.getUsers().isEmpty(), "Users map must not be empty");
        log.info("testData.yml OK. Users: {}, Products: {}",
            td.getUsers().size(), td.getProducts().size());
    }

    @Test(description = "validUser exists and has required fields")
    public void testValidUserData() {
        TestData.UserData user = YamlDataReader.getUser("validUser");
        Assert.assertNotNull(user,               "validUser must not be null");
        Assert.assertFalse(user.getEmail().isBlank(),    "validUser email must not be blank");
        Assert.assertFalse(user.getPassword().isBlank(), "validUser password must not be blank");
        Assert.assertFalse(user.getRole().isBlank(),     "validUser role must not be blank");
        log.info("validUser OK: email={}, role={}", user.getEmail(), user.getRole());
    }

    @Test(description = "adminUser exists and has admin role")
    public void testAdminUserData() {
        TestData.UserData admin = YamlDataReader.getUser("adminUser");
        Assert.assertEquals(admin.getRole(), "admin",
            "adminUser role must be 'admin'");
        log.info("adminUser OK: role={}", admin.getRole());
    }

    @Test(description = "lockedUser has isLocked=true")
    public void testLockedUserFlag() {
        TestData.UserData locked = YamlDataReader.getUser("lockedUser");
        Assert.assertTrue(locked.isLocked(), "lockedUser.isLocked must be true");
        log.info("lockedUser.isLocked=true OK");
    }

    @Test(description = "laptop product has positive price and is in stock")
    public void testProductData() {
        TestData.ProductData laptop = YamlDataReader.getProduct("laptop");
        Assert.assertTrue(laptop.getPrice() > 0,  "Laptop price must be > 0");
        Assert.assertTrue(laptop.isInStock(),     "Laptop must be in stock");
        Assert.assertFalse(laptop.getSku().isBlank(), "Laptop SKU must not be blank");
        log.info("Laptop product OK: price={}, sku={}", laptop.getPrice(), laptop.getSku());
    }

    @Test(description = "outOfStockItem has inStock=false")
    public void testOutOfStockProduct() {
        TestData.ProductData item = YamlDataReader.getProduct("outOfStockItem");
        Assert.assertFalse(item.isInStock(), "outOfStockItem.inStock must be false");
        Assert.assertEquals(item.getQuantity(), 0, "outOfStockItem quantity must be 0");
        log.info("outOfStockItem OK: inStock=false, quantity=0");
    }

    @Test(description = "API valid credentials are populated")
    public void testApiCredentials() {
        TestData.ApiData.AuthData.Credentials creds = YamlDataReader.getValidApiCredentials();
        Assert.assertFalse(creds.getEmail().isBlank(),    "API valid email must not be blank");
        Assert.assertFalse(creds.getPassword().isBlank(), "API valid password must not be blank");
        log.info("API creds OK: email={}", creds.getEmail());
    }

    @Test(description = "API createValid payload has all required fields")
    public void testApiCreateValidPayload() {
        TestData.ApiData.UserPayload p = YamlDataReader.getApiUserPayload("createValid");
        Assert.assertFalse(p.getEmail().isBlank(),     "createValid email must not be blank");
        Assert.assertFalse(p.getFirstName().isBlank(), "createValid firstName must not be blank");
        Assert.assertFalse(p.getRole().isBlank(),      "createValid role must not be blank");
        log.info("createValid payload OK: email={}", p.getEmail());
    }

    @Test(description = "API expected response codes are valid HTTP codes")
    public void testApiExpectedResponseCodes() {
        TestData.ApiData.ExpectedResponses exp = YamlDataReader.getApiExpectedResponses();
        Assert.assertEquals(exp.getCreateStatusCode(),  201);
        Assert.assertEquals(exp.getGetStatusCode(),     200);
        Assert.assertEquals(exp.getDeleteStatusCode(),  204);
        Assert.assertEquals(exp.getNotFoundCode(),      404);
        Assert.assertEquals(exp.getConflictCode(),      409);
        Assert.assertEquals(exp.getUnauthorizedCode(),  401);
        Assert.assertTrue(exp.getMaxResponseTimeMs() > 0,
            "maxResponseTimeMs must be > 0");
        log.info("API response codes OK.");
    }

    @Test(description = "Mobile validUser credentials are populated")
    public void testMobileUserData() {
        TestData.MobileData.LoginScreenData.MobileUser mobileUser =
            YamlDataReader.getMobileUser("validUser");
        Assert.assertFalse(mobileUser.getUsername().isBlank(), "Mobile username must not be blank");
        Assert.assertFalse(mobileUser.getPassword().isBlank(), "Mobile password must not be blank");
        log.info("Mobile validUser OK: username={}", mobileUser.getUsername());
    }

    @Test(description = "Valid email list from testData.yml is non-empty")
    public void testValidEmails() {
        List<String> emails = YamlDataReader.getValidEmails();
        Assert.assertFalse(emails.isEmpty(), "Valid email list must not be empty");
        emails.forEach(e -> Assert.assertTrue(e.contains("@"),
            "Each valid email must contain '@': " + e));
        log.info("Valid emails OK: {} entries", emails.size());
    }

    @Test(description = "Invalid email list contains known bad patterns")
    public void testInvalidEmails() {
        List<String> invalid = YamlDataReader.getInvalidEmails();
        Assert.assertFalse(invalid.isEmpty(), "Invalid email list must not be empty");
        log.info("Invalid emails OK: {} entries", invalid.size());
    }

    @Test(description = "Password test data has all required variants")
    public void testPasswordData() {
        TestData.ValidationData.PasswordData pwd = YamlDataReader.getPasswordData();
        Assert.assertFalse(pwd.getStrong().isBlank(),    "strong password must not be blank");
        Assert.assertFalse(pwd.getWeak().isBlank(),      "weak password must not be blank");
        Assert.assertFalse(pwd.getTooShort().isBlank(),  "tooShort password must not be blank");
        log.info("Password data OK: strong=[{}]", pwd.getStrong());
    }

    @Test(description = "Valid search keywords list is non-empty")
    public void testSearchKeywords() {
        List<String> keywords = YamlDataReader.getValidSearchKeywords();
        Assert.assertFalse(keywords.isEmpty(), "Valid search keywords must not be empty");
        log.info("Search keywords OK: {}", keywords);
    }

    @Test(description = "YamlConfigLoader.reload() works without error")
    public void testReload() {
        YamlConfigLoader.reload();
        Assert.assertNotNull(YamlConfigLoader.getFramework(), "After reload, framework config must not be null");
        Assert.assertNotNull(YamlConfigLoader.getBrowser(),   "After reload, browser config must not be null");
        Assert.assertNotNull(YamlConfigLoader.getTestData(),  "After reload, test data must not be null");
        log.info("reload() OK.");
    }
}
