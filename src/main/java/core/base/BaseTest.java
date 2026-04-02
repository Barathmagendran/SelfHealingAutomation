package core.base;

import core.config.ConfigReader;
import core.driver.DriverManager;
import core.utils.NavigationUtils;
import core.reporting.ExtentReportManager;
import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * Abstract base class for all test classes.
 *
 * <p>Provides lifecycle management for:
 * <ul>
 *   <li>WebDriver initialization and teardown</li>
 *   <li>Extent Report test node creation</li>
 *   <li>Screenshot capture on failure</li>
 * </ul>
 *
 * <p>All page-level test classes should extend this class (directly or indirectly).
 * Step definitions in Cucumber should use Cucumber hooks instead — see {@code Hooks.java}.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public abstract class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);
    protected WebDriver driver;
    protected ExtentTest extentTest;

    /**
     * Runs before each test method.
     * Initializes WebDriver and creates an Extent Report test node.
     *
     * @param method the test method being executed (injected by TestNG)
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        log.info("========== TEST START: {} ==========", method.getName());

        // Initialize report node for this test
        extentTest = ExtentReportManager.createTest(method.getName());

        // Init driver (lazy, thread-local)
        DriverManager.initDriver();
        driver = DriverManager.getDriver();

        // Launch the application base URL for web tests
        String platform = ConfigReader.get("platform", "web");
        if ("web".equalsIgnoreCase(platform)) {
            NavigationUtils.launchUrl();
        }
    }

    /**
     * Runs after each test method.
     * Captures screenshot on failure, updates report, quits driver.
     *
     * @param result the TestNG result object
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("TEST FAILED: {}", testName);
            if (extentTest != null) {
                extentTest.fail(result.getThrowable());
                // Attach screenshot to report
                if (DriverManager.isDriverInitialized()) {
                    extentTest.addScreenCaptureFromBase64String(
                        core.utils.ScreenshotUtils.captureScreenshotAsBase64(),
                        "Failure Screenshot"
                    );
                }
            }
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            log.info("TEST PASSED: {}", testName);
            if (extentTest != null) extentTest.pass("Test passed.");
        } else {
            log.warn("TEST SKIPPED: {}", testName);
            if (extentTest != null) extentTest.skip("Test skipped.");
        }

        DriverManager.quitDriver();
        log.info("========== TEST END: {} ==========", testName);
    }
}
