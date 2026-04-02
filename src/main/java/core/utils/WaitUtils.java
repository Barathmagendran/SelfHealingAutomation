package core.utils;

import core.config.ConfigReader;
import core.driver.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Centralized utility class for all WebDriver wait strategies.
 *
 * <p>Provides explicit waits using {@link WebDriverWait} and {@link FluentWait}.
 * Avoids usage of Thread.sleep() in favor of condition-based polling.
 *
 * <p>All timeout values default to {@code explicit.wait} in {@code config.properties}
 * but can be overridden per call.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    /**
     * Default wait timeout in seconds.
     * Resolution: -Dexplicit.wait > config.properties > framework.yml timeouts.explicit > 15
     */
    private static final int DEFAULT_TIMEOUT = Integer.parseInt(
        ConfigReader.get("explicit.wait",
            ConfigReader.getFromFrameworkYml("explicit.wait", "15")));

    /** Default polling interval for FluentWait */
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

    private WaitUtils() {}

    // ─────────────────────────────────────────────────────────
    // Core wait factory
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a {@link WebDriverWait} with the given timeout for the current thread's driver.
     *
     * @param timeoutSeconds wait timeout in seconds
     * @return configured WebDriverWait
     */
    public static WebDriverWait getWait(int timeoutSeconds) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Creates a {@link FluentWait} with custom polling interval and ignored exceptions.
     *
     * @param timeoutSeconds wait timeout in seconds
     * @return configured FluentWait
     */
    public static FluentWait<WebDriver> getFluentWait(int timeoutSeconds) {
        return new FluentWait<>(DriverManager.getDriver())
            .withTimeout(Duration.ofSeconds(timeoutSeconds))
            .pollingEvery(POLLING_INTERVAL)
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
    }

    // ─────────────────────────────────────────────────────────
    // Visibility waits
    // ─────────────────────────────────────────────────────────

    /**
     * Waits until the element located by the given locator is visible.
     *
     * @param locator        the element locator
     * @param timeoutSeconds max wait time in seconds
     * @return the visible {@link WebElement}
     */
    public static WebElement waitForVisibility(By locator, int timeoutSeconds) {
        log.debug("Waiting for visibility: {}", locator);
        return getWait(timeoutSeconds).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for element visibility using the default configured timeout.
     *
     * @param locator the element locator
     * @return the visible {@link WebElement}
     */
    public static WebElement waitForVisibility(By locator) {
        return waitForVisibility(locator, DEFAULT_TIMEOUT);
    }

    /**
     * Waits for an already-found WebElement to become visible.
     *
     * @param element        the WebElement
     * @param timeoutSeconds max wait time
     * @return the visible element
     */
    public static WebElement waitForVisibility(WebElement element, int timeoutSeconds) {
        return getWait(timeoutSeconds).until(ExpectedConditions.visibilityOf(element));
    }

    // ─────────────────────────────────────────────────────────
    // Clickability waits
    // ─────────────────────────────────────────────────────────

    /**
     * Waits until the element is clickable (visible and enabled).
     *
     * @param locator        element locator
     * @param timeoutSeconds max wait time
     * @return the clickable element
     */
    public static WebElement waitForClickability(By locator, int timeoutSeconds) {
        log.debug("Waiting for clickability: {}", locator);
        return getWait(timeoutSeconds).until(
            ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForClickability(By locator) {
        return waitForClickability(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickability(WebElement element) {
        return getWait(DEFAULT_TIMEOUT).until(
            ExpectedConditions.elementToBeClickable(element));
    }

    // ─────────────────────────────────────────────────────────
    // Presence & disappearance
    // ─────────────────────────────────────────────────────────

    /**
     * Waits for an element to be present in the DOM (not necessarily visible).
     *
     * @param locator element locator
     * @return the element
     */
    public static WebElement waitForPresence(By locator) {
        return getWait(DEFAULT_TIMEOUT).until(
            ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits for all matching elements to be present in the DOM.
     *
     * @param locator element locator
     * @return list of matching elements
     */
    public static List<WebElement> waitForPresenceOfAll(By locator) {
        return getWait(DEFAULT_TIMEOUT).until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    /**
     * Waits for an element to become invisible or be removed from the DOM.
     *
     * @param locator        element locator
     * @param timeoutSeconds max wait time
     * @return true if element becomes invisible
     */
    public static boolean waitForInvisibility(By locator, int timeoutSeconds) {
        log.debug("Waiting for invisibility: {}", locator);
        return getWait(timeoutSeconds).until(
            ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForInvisibility(By locator) {
        return waitForInvisibility(locator, DEFAULT_TIMEOUT);
    }

    // ─────────────────────────────────────────────────────────
    // Text & URL conditions
    // ─────────────────────────────────────────────────────────

    /**
     * Waits until the element contains the expected text.
     *
     * @param locator      element locator
     * @param expectedText text to wait for
     * @return true when text is present
     */
    public static boolean waitForText(By locator, String expectedText) {
        return getWait(DEFAULT_TIMEOUT).until(
            ExpectedConditions.textToBePresentInElementLocated(locator, expectedText));
    }

    /**
     * Waits until the page URL contains the given fragment.
     *
     * @param urlFragment substring to wait for in the URL
     */
    public static void waitForUrlContains(String urlFragment) {
        getWait(DEFAULT_TIMEOUT).until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Waits until the page title contains the given substring.
     *
     * @param titleFragment expected title substring
     */
    public static void waitForTitleContains(String titleFragment) {
        getWait(DEFAULT_TIMEOUT).until(ExpectedConditions.titleContains(titleFragment));
    }

    // ─────────────────────────────────────────────────────────
    // Alert handling
    // ─────────────────────────────────────────────────────────

    /**
     * Waits for a browser alert to appear.
     *
     * @return the {@link Alert}
     */
    public static Alert waitForAlert() {
        return getWait(DEFAULT_TIMEOUT).until(ExpectedConditions.alertIsPresent());
    }

    // ─────────────────────────────────────────────────────────
    // Frame handling
    // ─────────────────────────────────────────────────────────

    /**
     * Waits for an iFrame to be available and switches to it.
     *
     * @param frameLocator frame locator
     * @return the driver focused on the frame
     */
    public static WebDriver waitForFrameAndSwitch(By frameLocator) {
        return getWait(DEFAULT_TIMEOUT).until(
            ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
    }
}
