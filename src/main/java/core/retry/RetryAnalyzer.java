package core.retry;

import core.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG retry analyzer that automatically retries failed tests.
 *
 * <p>The max retry count is controlled by {@code retry.count} in config.properties
 * (default: 1). Set to 0 to disable retries.
 *
 * <p>Usage — apply at method or class level:
 * <pre>
 *   &#64;Test(retryAnalyzer = RetryAnalyzer.class)
 *   public void myTest() { ... }
 * </pre>
 *
 * <p>Or globally via a TestNG listener on the suite runner.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);

    /** Current retry attempt counter per test instance */
    private int retryCount = 0;

    /**
     * Max retries per test.
     * Resolution: -Dretry.count > config.properties > framework.yml retry.count > 1
     */
    private static final int MAX_RETRY_COUNT = Integer.parseInt(
        ConfigReader.get("retry.count",
            ConfigReader.getFromFrameworkYml("retry.count", "1")));

    /**
     * Called by TestNG when a test fails.
     * Returns {@code true} to retry the test, {@code false} to mark it failed.
     *
     * @param result the failed test result
     * @return true if the test should be retried
     */
    @Override
    public boolean retry(ITestResult result) {
        if (MAX_RETRY_COUNT == 0) {
            return false;
        }

        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            log.warn("Retrying failed test [{}] — attempt {}/{}",
                result.getName(), retryCount, MAX_RETRY_COUNT);
            return true;
        }

        log.error("Test [{}] failed after {} retry attempt(s). Marking as FAILED.",
            result.getName(), MAX_RETRY_COUNT);
        return false;
    }
}
