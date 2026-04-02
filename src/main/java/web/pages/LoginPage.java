package web.pages;

import core.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the Login page.
 *
 * <p>Encapsulates all interactions with the login UI.
 * Follows the Page Object Model (POM) pattern — no assertions here,
 * only page actions and state getters.
 *
 * <p>URL launch strategy:
 * <ul>
 *   <li>Use {@link #open()} to explicitly navigate to the login page.</li>
 *   <li>Use {@link #open(String)} to navigate to a custom login path.</li>
 *   <li>The default path {@code /login} is appended to the configured base URL.</li>
 * </ul>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class LoginPage extends BasePage {

    /** Default path for the login page relative to base URL */
    private static final String LOGIN_PATH = "/login";
    private static final By password = By.id("password");

    // ─────────────────────────────────────────────────────────
    // Launch methods
    // ─────────────────────────────────────────────────────────

    /**
     * Navigates to the default login page path ({@code /login}) relative to the
     * configured base URL.
     *
     * <pre>
     *   // In a Cucumber Given step:
     *   new LoginPage().open();
     *
     *   // Resolved URL: https://yourapp.qa.example.com/login
     * </pre>
     *
     * @return this {@link LoginPage} instance for fluent chaining
     */
    public LoginPage open() {
        log.info("Opening login page: {}", LOGIN_PATH);
        launchPage(LOGIN_PATH);
        return this;
    }

    /**
     * Navigates to a custom login path — useful when the login page lives at
     * a non-standard URL such as {@code /auth/sign-in} or {@code /sso/login}.
     *
     * @param customPath custom path relative to base URL (e.g., "/auth/sign-in")
     * @return this {@link LoginPage} instance for fluent chaining
     */
    public LoginPage open(String customPath) {
        log.info("Opening login page with custom path: {}", customPath);
        launchPage(customPath);
        return this;
    }

    /**
     * Navigates directly to the base URL (application root), then allows the
     * application's own redirect to send the user to the login page.
     * Use when the app redirects unauthenticated users to login automatically.
     *
     * @return this {@link LoginPage} instance
     */
    public LoginPage openViaBaseUrl() {
        log.info("Opening login page via base URL redirect");
        launchUrl();
        return this;
    }

    // ─────────────────────────────────────────────────────────
    // Locators — defined as constants for reusability
    // ─────────────────────────────────────────────────────────

    private static final By EMAIL_FIELD        = By.id("email");
    private static final By PASSWORD_FIELD     = password;
    private static final By LOGIN_BUTTON       = By.id("login-btn");
    private static final By ERROR_MESSAGE      = By.cssSelector(".error-message");
    private static final By WELCOME_BANNER     = By.cssSelector(".welcome-banner");
    private static final By FORGOT_PWD_LINK    = By.linkText("Forgot Password?");
    private static final By REMEMBER_ME_CHECKBOX = By.id("remember-me");

    // PageFactory annotation example (alternative approach)
    @FindBy(css = ".loading-spinner")
    private WebElement loadingSpinner;

    // ─────────────────────────────────────────────────────────
    // Page Actions
    // ─────────────────────────────────────────────────────────

    /**
     * Enters the user's email address in the email input field.
     *
     * @param email the email address to enter
     * @return this page instance (fluent builder pattern)
     */
    public LoginPage enterEmail(String email) {
        log.info("Entering email: {}", email);
        type(EMAIL_FIELD, email);
        return this;
    }

    /**
     * Enters the user's password in the password input field.
     *
     * @param password the password to enter
     * @return this page instance
     */
    public LoginPage enterPassword(String password) {
        log.info("Entering password: [REDACTED]");
        type(PASSWORD_FIELD, password);
        return this;
    }

    /**
     * Clicks the Login button to submit credentials.
     *
     * @return this page instance
     */
    public LoginPage clickLoginButton() {
        log.info("Clicking login button");
        click(LOGIN_BUTTON);
        return this;
    }

    /**
     * Toggles the "Remember Me" checkbox.
     *
     * @return this page instance
     */
    public LoginPage checkRememberMe() {
        log.info("Checking 'Remember Me' checkbox");
        click(REMEMBER_ME_CHECKBOX);
        return this;
    }

    /**
     * Clicks the "Forgot Password?" link.
     *
     * @return this page instance
     */
    public LoginPage clickForgotPassword() {
        log.info("Clicking 'Forgot Password?' link");
        click(FORGOT_PWD_LINK);
        return this;
    }

    /**
     * Performs a complete login flow: enters credentials and submits.
     *
     * @param email    user's email address
     * @param password user's password
     * @return this page instance
     */
    public LoginPage login(String email, String password) {
        return enterEmail(email)
            .enterPassword(password)
            .clickLoginButton();
    }

    // ─────────────────────────────────────────────────────────
    // State Getters (for assertions in step definitions)
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the error message text displayed on login failure.
     *
     * @return error message string
     */
    public String getErrorMessage() {
        return getText(ERROR_MESSAGE);
    }

    /**
     * Returns the welcome banner text shown after successful login.
     *
     * @return welcome banner text
     */
    public String getWelcomeBannerText() {
        return getText(WELCOME_BANNER);
    }

    /**
     * Checks whether the error message is currently visible.
     *
     * @return true if the error is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isDisplayed(ERROR_MESSAGE);
    }

    /**
     * Checks whether the welcome banner is visible (indicates successful login).
     *
     * @return true if the welcome banner is shown
     */
    public boolean isLoginSuccessful() {
        return isDisplayed(WELCOME_BANNER);
    }

    /**
     * Checks whether the login button is currently enabled.
     *
     * @return true if the button is enabled
     */
    public boolean isLoginButtonEnabled() {
        return isEnabled(LOGIN_BUTTON);
    }
}
