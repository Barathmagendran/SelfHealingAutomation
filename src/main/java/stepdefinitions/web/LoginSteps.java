package stepdefinitions.web;

import core.config.YamlDataReader;
import core.config.model.TestData.UserData;
import core.reporting.ExtentReportManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import web.pages.DashboardPage;
import web.pages.LoginPage;

/**
 * Step definitions for Login feature scenarios.
 *
 * <p>Test data is sourced from {@code testData.yml} via {@link YamlDataReader}.
 * Inline Gherkin data (quoted strings) is also supported for scenario outlines.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class LoginSteps {

    private static final Logger log = LogManager.getLogger(LoginSteps.class);

    private LoginPage loginPage;
    private DashboardPage dashboardPage;

    // ─────────────────────────────────────────────────────────
    // Given
    // ─────────────────────────────────────────────────────────

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        log.info("Step: User navigates to login page");
        loginPage = new LoginPage().open();
        ExtentReportManager.getTest().info("Navigated to login page: " +
            core.utils.NavigationUtils.getCurrentUrl());
    }

    @Given("the user navigates to {string} page")
    public void theUserNavigatesToPage(String path) {
        log.info("Step: Navigating to page path: {}", path);
        loginPage = new LoginPage();
        loginPage.launchPage(path);
        ExtentReportManager.getTest().info("Navigated to: " + path);
    }

    @Given("the user opens the application")
    public void theUserOpensTheApplication() {
        log.info("Step: Launching application base URL");
        core.utils.NavigationUtils.launchUrl();
        ExtentReportManager.getTest().info("Application launched: " +
            core.utils.NavigationUtils.getCurrentUrl());
    }

    // ─────────────────────────────────────────────────────────
    // When — inline credentials (Scenario Outlines / quick tests)
    // ─────────────────────────────────────────────────────────

    @When("the user enters email {string} and password {string}")
    public void theUserEntersCredentials(String email, String password) {
        log.info("Step: Entering credentials email={}", email);
        loginPage.enterEmail(email).enterPassword(password);
        ExtentReportManager.getTest().info("Entered credentials: " + email);
    }

    @When("the user clicks the login button")
    public void theUserClicksLoginButton() {
        log.info("Step: Clicking login button");
        loginPage.clickLoginButton();
        ExtentReportManager.getTest().info("Clicked login button");
    }

    @When("the user logs in with email {string} and password {string}")
    public void theUserLogsIn(String email, String password) {
        log.info("Step: Full login — email={}", email);
        loginPage = new LoginPage();
        loginPage.login(email, password);
        ExtentReportManager.getTest().info("Login attempted: " + email);
    }

    // ─────────────────────────────────────────────────────────
    // When — YAML-driven (reads from testData.yml > users)
    // ─────────────────────────────────────────────────────────

    /**
     * Logs in using a named user key from {@code testData.yml > users}.
     *
     * <p>Example Gherkin:
     * <pre>
     *   When the user logs in as "adminUser"
     *   When the user logs in as "viewerUser"
     * </pre>
     *
     * @param userKey key matching a user entry in testData.yml
     */
    @When("the user logs in as {string}")
    public void theUserLogsInAs(String userKey) {
        UserData user = YamlDataReader.getUser(userKey);
        log.info("Step: Logging in as YAML user [{}] email={}", userKey, user.getEmail());
        loginPage = new LoginPage();
        loginPage.login(user.getEmail(), user.getPassword());
        ExtentReportManager.getTest().info(
            "Logged in as [" + userKey + "] email=" + user.getEmail());
    }

    /**
     * Enters credentials from testData.yml without clicking login.
     * Useful when additional actions (e.g., Remember Me) happen before submit.
     *
     * @param userKey key matching a user in testData.yml
     */
    @When("the user enters credentials for {string}")
    public void theUserEntersCredentialsFor(String userKey) {
        UserData user = YamlDataReader.getUser(userKey);
        log.info("Step: Entering YAML credentials for [{}]", userKey);
        loginPage.enterEmail(user.getEmail()).enterPassword(user.getPassword());
        ExtentReportManager.getTest().info("Credentials entered for: " + userKey);
    }

    // ─────────────────────────────────────────────────────────
    // Then
    // ─────────────────────────────────────────────────────────

    @Then("the user should be redirected to the dashboard")
    public void theUserShouldBeRedirectedToDashboard() {
        log.info("Step: Verifying redirect to dashboard");
        dashboardPage = new DashboardPage();
        Assert.assertTrue(dashboardPage.isOnDashboard(),
            "Expected dashboard — profile icon not visible.");
        ExtentReportManager.getTest().pass("User redirected to dashboard.");
    }

    @Then("the welcome banner should contain {string}")
    public void theWelcomeBannerShouldContain(String userName) {
        String actualText = new LoginPage().getWelcomeBannerText();
        Assert.assertTrue(actualText.contains(userName),
            "Welcome banner '" + actualText + "' does not contain '" + userName + "'");
        ExtentReportManager.getTest().pass("Welcome banner verified: " + actualText);
    }

    @Then("an error message {string} should be displayed")
    public void anErrorMessageShouldBeDisplayed(String expectedError) {
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message was not shown.");
        Assert.assertEquals(loginPage.getErrorMessage(), expectedError, "Error message mismatch.");
        ExtentReportManager.getTest().pass("Error message verified: " + expectedError);
    }

    @Then("the user should remain on the login page")
    public void theUserShouldRemainOnLoginPage() {
        Assert.assertTrue(loginPage.isLoginButtonEnabled(),
            "Login button not found — user may have navigated away.");
        ExtentReportManager.getTest().pass("User confirmed on login page.");
    }

    @And("the dashboard heading should be {string}")
    public void theDashboardHeadingShouldBe(String expectedHeading) {
        String actual = new DashboardPage().getPageHeading();
        Assert.assertEquals(actual, expectedHeading, "Dashboard heading mismatch.");
        ExtentReportManager.getTest().pass("Dashboard heading: " + actual);
    }
}
