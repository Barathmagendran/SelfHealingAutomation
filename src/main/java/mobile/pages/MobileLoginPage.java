package mobile.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object for the Mobile Login screen.
 *
 * <p>Uses Appium-specific locator strategies (accessibility ID, XPath for mobile UI).
 * Interacts directly with {@link AppiumDriver} for mobile-specific actions.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class MobileLoginPage {

    private static final Logger log = LogManager.getLogger(MobileLoginPage.class);

    private final AppiumDriver driver;
    private final WebDriverWait wait;

    // Locators — use accessibility IDs where possible (platform-agnostic)
    private static final String USERNAME_FIELD_ID  = "username-input";
    private static final String PASSWORD_FIELD_ID  = "password-input";
    private static final String LOGIN_BUTTON_ID    = "login-button";
    private static final String ERROR_MESSAGE_ID   = "error-message";
    private static final String HOME_SCREEN_ID     = "home-screen";

    public MobileLoginPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /**
     * Enters the username in the username field.
     *
     * @param username the username to enter
     */
    public void enterUsername(String username) {
        log.info("Entering username: {}", username);
        WebElement field = wait.until(
            ExpectedConditions.visibilityOfElementLocated(AppiumBy.accessibilityId(USERNAME_FIELD_ID)));
        field.clear();
        field.sendKeys(username);
    }

    /**
     * Enters the password in the password field.
     *
     * @param password the password to enter
     */
    public void enterPassword(String password) {
        log.info("Entering password: [REDACTED]");
        WebElement field = wait.until(
            ExpectedConditions.visibilityOfElementLocated(AppiumBy.accessibilityId(PASSWORD_FIELD_ID)));
        field.clear();
        field.sendKeys(password);
    }

    /**
     * Taps the login button to submit credentials.
     */
    public void tapLoginButton() {
        log.info("Tapping login button");
        wait.until(ExpectedConditions.elementToBeClickable(
            AppiumBy.accessibilityId(LOGIN_BUTTON_ID))).click();
    }

    /**
     * Checks if the home screen is displayed after login.
     *
     * @return true if the home screen is visible
     */
    public boolean isHomeScreenDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(
                AppiumBy.accessibilityId(HOME_SCREEN_ID))).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the error message text shown on login failure.
     *
     * @return error message string
     */
    public String getErrorMessage() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
            AppiumBy.accessibilityId(ERROR_MESSAGE_ID))).getText();
    }
}
