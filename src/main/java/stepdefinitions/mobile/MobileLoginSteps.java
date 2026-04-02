package stepdefinitions.mobile;

import core.driver.DriverManager;
import core.reporting.ExtentReportManager;
import io.appium.java_client.AppiumDriver;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import mobile.pages.MobileLoginPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

/**
 * Step definitions for Mobile Login scenarios.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class MobileLoginSteps {

    private static final Logger log = LogManager.getLogger(MobileLoginSteps.class);

    private MobileLoginPage loginPage;

    @Given("the mobile app is launched")
    public void theMobileAppIsLaunched() {
        log.info("Step: Mobile app is launched");
        loginPage = new MobileLoginPage((AppiumDriver) DriverManager.getDriver());
        ExtentReportManager.getTest().info("Mobile app launched.");
    }

    @When("the mobile user enters username {string} and password {string}")
    public void theMobileUserEntersCredentials(String username, String password) {
        log.info("Step: Entering mobile credentials for user={}", username);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        ExtentReportManager.getTest().info("Credentials entered on mobile login screen.");
    }

    @When("the mobile user taps the login button")
    public void theMobileUserTapsLoginButton() {
        log.info("Step: Tapping mobile login button");
        loginPage.tapLoginButton();
    }

    @Then("the mobile home screen should be displayed")
    public void theMobileHomeScreenShouldBeDisplayed() {
        Assert.assertTrue(loginPage.isHomeScreenDisplayed(),
            "Home screen was not displayed after mobile login.");
        ExtentReportManager.getTest().pass("Mobile home screen displayed successfully.");
    }

    @Then("the mobile error message {string} should be shown")
    public void theMobileErrorMessageShouldBeShown(String expectedError) {
        String actual = loginPage.getErrorMessage();
        Assert.assertEquals(actual, expectedError, "Mobile error message mismatch.");
        ExtentReportManager.getTest().pass("Mobile error message verified: " + actual);
    }
}
