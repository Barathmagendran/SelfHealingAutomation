package core.listeners;

import core.reporting.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * TestNG Suite Listener — manages suite-level lifecycle events.
 *
 * <p>Hooks into TestNG's suite start/finish events to:
 * <ul>
 *   <li>Initialize Extent Reports before any test runs</li>
 *   <li>Flush the report to disk after the suite completes</li>
 * </ul>
 *
 * <p>Registered in {@code testng.xml} under {@code <listeners>}.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class SuiteListener implements ISuiteListener {

    private static final Logger log = LogManager.getLogger(SuiteListener.class);

    /**
     * Called by TestNG before the suite begins execution.
     * Initializes the Extent Reports engine.
     *
     * @param suite the TestNG suite being started
     */
    @Override
    public void onStart(ISuite suite) {
        log.info("============================================================");
        log.info("SUITE STARTING: {}", suite.getName());
        log.info("============================================================");
        ExtentReportManager.initReports();
    }

    /**
     * Called by TestNG after the suite finishes all tests.
     * Flushes the Extent Report to the output file.
     *
     * @param suite the completed TestNG suite
     */
    @Override
    public void onFinish(ISuite suite) {
        ExtentReportManager.flushReports();
        log.info("============================================================");
        log.info("SUITE COMPLETE: {}", suite.getName());
        log.info("============================================================");
    }
}
