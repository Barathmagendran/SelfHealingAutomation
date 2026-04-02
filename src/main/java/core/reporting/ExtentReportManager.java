package core.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import core.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Singleton manager for Extent Reports integration.
 *
 * <p>Provides thread-safe test node creation via {@link ThreadLocal}.
 * Must be initialized with {@link #initReports()} before test execution
 * and flushed with {@link #flushReports()} after all tests complete.
 *
 * <p>Report is saved to the path configured in {@code extent.report.path}.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);

    /** Singleton ExtentReports instance */
    private static ExtentReports extent;

    /** Thread-local test nodes for parallel execution */
    private static final ThreadLocal<ExtentTest> testThreadLocal = new ThreadLocal<>();

    private ExtentReportManager() {}

    /**
     * Initializes the Extent Reports instance with the Spark (HTML) reporter.
     * Call once before the test suite begins (e.g., in a TestNG suite listener).
     */
    public static synchronized void initReports() {
        if (extent == null) {
            String reportPath = ConfigReader.get("extent.report.path",
                "target/reports/ExtentReport.html");
            String reportTitle = ConfigReader.get("extent.report.title",
                "Automation Test Report");
            String reportName = ConfigReader.get("extent.report.name",
                "Test Execution Report");

            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setTheme(Theme.DARK);
            sparkReporter.config().setDocumentTitle(reportTitle);
            sparkReporter.config().setReportName(reportName);
            sparkReporter.config().setEncoding("UTF-8");
            sparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);

            // System info embedded in report
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("Environment", ConfigReader.getActiveEnvironment().toUpperCase());
            extent.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
            extent.setSystemInfo("Executed By", System.getProperty("user.name", "CI"));

            log.info("Extent Reports initialized. Report path: {}", reportPath);
        }
    }

    /**
     * Creates and registers a test node in the Extent Report for the current thread.
     *
     * @param testName the display name for the test in the report
     * @return the created {@link ExtentTest} node
     */
    public static synchronized ExtentTest createTest(String testName) {
        if (extent == null) {
            initReports();
        }
        ExtentTest test = extent.createTest(testName);
        testThreadLocal.set(test);
        log.debug("Created Extent test node: {}", testName);
        return test;
    }

    /**
     * Creates a test node with an optional description.
     *
     * @param testName    display name
     * @param description test description
     * @return created ExtentTest node
     */
    public static synchronized ExtentTest createTest(String testName, String description) {
        if (extent == null) {
            initReports();
        }
        ExtentTest test = extent.createTest(testName, description);
        testThreadLocal.set(test);
        return test;
    }

    /**
     * Returns the {@link ExtentTest} node for the current thread.
     *
     * @return current thread's test node, or null if not initialized
     */
    public static ExtentTest getTest() {
        return testThreadLocal.get();
    }

    /**
     * Writes all in-memory test data to the report file.
     * Must be called after the test suite completes.
     */
    public static synchronized void flushReports() {
        if (extent != null) {
            extent.flush();
            log.info("Extent Reports flushed to disk.");
        }
    }

    /**
     * Removes the current thread's test node from ThreadLocal storage.
     * Call in test teardown to prevent memory leaks.
     */
    public static void removeTest() {
        testThreadLocal.remove();
    }
}
