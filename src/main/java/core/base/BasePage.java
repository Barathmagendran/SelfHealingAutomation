package core.base;

import core.driver.DriverManager;
import core.utils.NavigationUtils;
import core.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * Abstract base class for all Page Object classes.
 *
 * <p>Provides reusable, low-level browser interaction methods that wrap
 * Selenium's native API with logging, waiting, and error handling.
 *
 * <p>All page objects extend this class and use its methods instead of calling
 * Selenium directly, promoting consistency and reducing code duplication.
 *
 * <p>PageFactory is initialized in the constructor for {@code @FindBy} support.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());
    protected WebDriver driver;

    /**
     * Constructor initializes the page's WebDriver from DriverManager
     * and sets up PageFactory annotations.
     */
    public BasePage() {
        this.driver = DriverManager.getDriver();
        PageFactory.initElements(driver, this);
    }

    // ─────────────────────────────────────────────────────────
    // Navigation  (delegates to NavigationUtils)
    // ─────────────────────────────────────────────────────────

    /**
     * Launches the configured application base URL for the active environment.
     * URL is resolved from: {@code -Dbase.url} → {@code {env}.properties}
     * → {@code config.properties} → {@code framework.yml urls.web}.
     *
     * <p>Use this in page constructors or Given steps when a test must start
     * from the application root regardless of where the browser currently is.
     */
    protected void launchUrl() {
        NavigationUtils.launchUrl();
    }

    /**
     * Navigates to a page relative to the application base URL.
     *
     * <pre>
     *   launchPage("/login");        // → https://app.qa.com/login
     *   launchPage("/admin/users");  // → https://app.qa.com/admin/users
     * </pre>
     *
     * @param path relative path (e.g., "/login", "/dashboard")
     */
    public void launchPage(String path) {
        NavigationUtils.launchPage(path);
    }

    /**
     * Navigates to an absolute URL, bypassing the configured base URL.
     *
     * @param absoluteUrl full URL including scheme
     */
    protected void launchAbsoluteUrl(String absoluteUrl) {
        NavigationUtils.launchAbsoluteUrl(absoluteUrl);
    }

    /**
     * Navigates the browser to the given URL (raw driver.get).
     * Prefer {@link #launchPage(String)} for relative paths and
     * {@link #launchUrl()} for the base URL.
     *
     * @param url the full URL to navigate to
     */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    /**
     * Navigates the browser back one step in history.
     */
    protected void navigateBack() {
        NavigationUtils.back();
    }

    /**
     * Navigates the browser forward one step in history.
     */
    protected void navigateForward() {
        NavigationUtils.forward();
    }

    /**
     * Refreshes the current page.
     */
    protected void refreshPage() {
        NavigationUtils.refresh();
    }

    /**
     * Returns the current browser URL.
     *
     * @return current URL string
     */
    protected String getCurrentUrl() {
        return NavigationUtils.getCurrentUrl();
    }

    /**
     * Returns the current page title.
     *
     * @return page title string
     */
    protected String getPageTitle() {
        return NavigationUtils.getPageTitle();
    }

    /**
     * Checks whether the current URL contains the given path fragment.
     *
     * @param pathFragment URL substring to check (e.g., "/login")
     * @return true if the current URL contains the fragment
     */
    protected boolean isOnPage(String pathFragment) {
        return NavigationUtils.isOnPage(pathFragment);
    }

    /**
     * Waits until the browser URL contains the given fragment.
     *
     * @param urlFragment expected URL substring
     */
    protected void waitForUrl(String urlFragment) {
        NavigationUtils.waitForUrlContains(urlFragment);
    }

    /**
     * Waits until the page title contains the given text.
     *
     * @param titleFragment expected title substring
     */
    protected void waitForTitle(String titleFragment) {
        NavigationUtils.waitForTitleContains(titleFragment);
    }

    // ─────────────────────────────────────────────────────────
    // Click actions
    // ─────────────────────────────────────────────────────────

    /**
     * Waits for an element to be clickable, then clicks it.
     *
     * @param locator the element locator
     */
    protected void click(By locator) {
        log.debug("Clicking: {}", locator);
        WaitUtils.waitForClickability(locator).click();
    }

    /**
     * Clicks an element and ignores stale element reference retries.
     *
     * @param element the WebElement to click
     */
    protected void click(WebElement element) {
        WaitUtils.waitForClickability(element).click();
    }

    /**
     * Performs a JavaScript click — useful when standard click fails due to overlapping elements.
     *
     * @param locator the element locator
     */
    protected void jsClick(By locator) {
        log.debug("JS click on: {}", locator);
        WebElement element = WaitUtils.waitForPresence(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    // ─────────────────────────────────────────────────────────
    // Input actions
    // ─────────────────────────────────────────────────────────

    /**
     * Clears an input field and types the given text.
     *
     * @param locator the input field locator
     * @param text    text to type
     */
    protected void type(By locator, String text) {
        log.debug("Typing '{}' into: {}", text, locator);
        WebElement element = WaitUtils.waitForVisibility(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Types into an element using keyboard key constants.
     *
     * @param locator the element locator
     * @param keys    keys to send
     */
    protected void sendKeys(By locator, Keys keys) {
        WaitUtils.waitForVisibility(locator).sendKeys(keys);
    }

    // ─────────────────────────────────────────────────────────
    // Read actions
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the visible text of an element.
     *
     * @param locator the element locator
     * @return trimmed text content
     */
    protected String getText(By locator) {
        return WaitUtils.waitForVisibility(locator).getText().trim();
    }

    /**
     * Returns the value of the given attribute for an element.
     *
     * @param locator       element locator
     * @param attributeName attribute to read
     * @return attribute value, or empty string if not present
     */
    protected String getAttribute(By locator, String attributeName) {
        WebElement element = WaitUtils.waitForPresence(locator);
        String value = element.getAttribute(attributeName);
        return value != null ? value : "";
    }

    // ─────────────────────────────────────────────────────────
    // Visibility checks
    // ─────────────────────────────────────────────────────────

    /**
     * Checks whether an element is currently visible on the page.
     *
     * @param locator element locator
     * @return true if visible
     */
    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Checks whether an element is enabled (interactable).
     *
     * @param locator element locator
     * @return true if enabled
     */
    protected boolean isEnabled(By locator) {
        try {
            return driver.findElement(locator).isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────
    // Dropdowns
    // ─────────────────────────────────────────────────────────

    /**
     * Selects an option from an HTML {@code <select>} dropdown by visible text.
     *
     * @param locator      dropdown locator
     * @param visibleText  the visible option text
     */
    protected void selectByVisibleText(By locator, String visibleText) {
        log.debug("Selecting '{}' from dropdown: {}", visibleText, locator);
        new Select(WaitUtils.waitForVisibility(locator)).selectByVisibleText(visibleText);
    }

    /**
     * Selects an option from a dropdown by its value attribute.
     *
     * @param locator dropdown locator
     * @param value   option value
     */
    protected void selectByValue(By locator, String value) {
        new Select(WaitUtils.waitForVisibility(locator)).selectByValue(value);
    }

    // ─────────────────────────────────────────────────────────
    // Advanced interactions
    // ─────────────────────────────────────────────────────────

    /**
     * Performs a mouse hover over the given element.
     *
     * @param locator element to hover on
     */
    protected void hoverOver(By locator) {
        WebElement element = WaitUtils.waitForVisibility(locator);
        new Actions(driver).moveToElement(element).perform();
    }

    /**
     * Scrolls the page until the element is in view using JavaScript.
     *
     * @param locator element to scroll to
     */
    protected void scrollToElement(By locator) {
        WebElement element = WaitUtils.waitForPresence(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /**
     * Drags an element and drops it onto a target.
     *
     * @param sourceLocator locator for the element to drag
     * @param targetLocator locator for the drop target
     */
    protected void dragAndDrop(By sourceLocator, By targetLocator) {
        WebElement source = WaitUtils.waitForVisibility(sourceLocator);
        WebElement target = WaitUtils.waitForVisibility(targetLocator);
        new Actions(driver).dragAndDrop(source, target).perform();
    }

    // ─────────────────────────────────────────────────────────
    // JavaScript helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Executes arbitrary JavaScript in the browser context.
     *
     * @param script  the JS to execute
     * @param args    arguments to pass to the script
     * @return script return value
     */
    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    // ─────────────────────────────────────────────────────────
    // Window / Frame management
    // ─────────────────────────────────────────────────────────

    /** Switches to the most recently opened window/tab. */
    protected void switchToNewWindow() {
        String original = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(original)) {
                driver.switchTo().window(handle);
                break;
            }
        }
    }

    /** Accepts the current browser alert dialog. */
    protected void acceptAlert() {
        WaitUtils.waitForAlert().accept();
    }

    /** Dismisses the current browser alert dialog. */
    protected void dismissAlert() {
        WaitUtils.waitForAlert().dismiss();
    }

    /**
     * Finds all elements matching the given locator.
     *
     * @param locator element locator
     * @return list of matched elements (empty if none)
     */
    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }
}
