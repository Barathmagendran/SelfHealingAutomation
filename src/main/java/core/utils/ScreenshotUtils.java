package core.utils;

import core.config.ConfigReader;
import core.driver.DriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for capturing and saving screenshots.
 *
 * <p>Primarily used in test failure hooks to attach visual evidence to reports.
 * Screenshots are saved under {@code target/screenshots/} by default,
 * configurable via {@code screenshot.path} in config.properties.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);

    private static final String SCREENSHOT_PATH = ConfigReader.get(
        "screenshot.path", "target/screenshots");

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private ScreenshotUtils() {}

    /**
     * Captures a screenshot and saves it to the configured screenshots directory.
     *
     * @param testName name used in the filename (e.g., scenario name)
     * @return absolute path to the saved screenshot file, or null if capture fails
     */
    public static String captureScreenshot(String testName) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("Screenshot capture skipped — no driver initialized.");
            return null;
        }

        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String safeTestName = sanitizeFileName(testName);
            String fileName = safeTestName + "_" + timestamp + ".png";
            String fullPath = SCREENSHOT_PATH + "/" + fileName;

            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destFile = new File(fullPath);
            FileUtils.copyFile(srcFile, destFile);

            log.info("Screenshot saved: {}", fullPath);
            return destFile.getAbsolutePath();

        } catch (IOException e) {
            log.error("Failed to save screenshot for test [{}]: {}", testName, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error capturing screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures a screenshot and returns raw bytes.
     * Useful for embedding directly into Extent Reports without saving to disk.
     *
     * @return screenshot as byte array, or empty array if capture fails
     */
    public static byte[] captureScreenshotAsBytes() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("Screenshot bytes capture skipped — no driver.");
            return new byte[0];
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.error("Failed to capture screenshot bytes: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Captures a screenshot as a Base64-encoded string.
     * Suitable for embedding in HTML reports.
     *
     * @return Base64 screenshot string, or empty string on failure
     */
    public static String captureScreenshotAsBase64() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("Screenshot Base64 capture skipped — no driver.");
            return "";
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.error("Failed to capture Base64 screenshot: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Sanitizes a test name into a valid filename by replacing special characters.
     *
     * @param name raw test name
     * @return filesystem-safe filename fragment
     */
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                   .replaceAll("_{2,}", "_")
                   .substring(0, Math.min(name.length(), 80));
    }
}
