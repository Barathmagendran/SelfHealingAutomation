package stepdefinitions.web;

import core.reporting.ExtentReportManager;
import core.utils.NavigationUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

/**
 * Reusable Cucumber step definitions for browser navigation.
 *
 * <p>All navigation steps delegate to {@link NavigationUtils}, which resolves
 * URLs from the configured environment (base URL, relative paths, or absolute URLs).
 *
 * <p>These steps are shared across all web feature files and do not belong
 * to any single page domain.
 *
 * <p>Example feature file usage:
 * <pre>
 *   Given the application is launched
 *   Given the user navigates to "/login"
 *   When the user navigates to absolute URL "https://other.example.com"
 *   When the user navigates back
 *   When the user refreshes the page
 *   Then the current URL should contain "/dashboard"
 *   Then the page title should contain "Welcome"
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class NavigationSteps {

    private static final Logger log = LogManager.getLogger(NavigationSteps.class);

    // ─────────────────────────────────────────────────────────
    // Given — launch steps
    // ─────────────────────────────────────────────────────────

    /**
     * Launches the application base URL for the active environment.
     * Equivalent to opening the browser and typing the configured base URL.
     */
    @Given("the application is launched")
    public void theApplicationIsLaunched() {
        log.info("Step: Launching application base URL");
        NavigationUtils.launchUrl();
        String currentUrl = NavigationUtils.getCurrentUrl();
        ExtentReportManager.getTest().info("Application launched. URL: " + currentUrl);
    }

    /**
     * Navigates to a relative path under the application base URL.
     *
     * <pre>
     *   Given the user navigates to "/login"
     *   Given the user navigates to "/admin/users"
     * </pre>
     *
     * @param path page path relative to base URL
     */
    @Given("the user navigates to {string}")
    public void theUserNavigatesTo(String path) {
        log.info("Step: Navigating to path: [{}]", path);
        NavigationUtils.launchPage(path);
        ExtentReportManager.getTest().info("Navigated to: " + path
            + " → " + NavigationUtils.getCurrentUrl());
    }

    // ─────────────────────────────────────────────────────────
    // When — browser actions
    // ─────────────────────────────────────────────────────────

    /**
     * Navigates to an absolute URL, bypassing the configured base URL.
     *
     * @param absoluteUrl full URL including scheme
     */
    @When("the user navigates to absolute URL {string}")
    public void theUserNavigatesToAbsoluteUrl(String absoluteUrl) {
        log.info("Step: Navigating to absolute URL: [{}]", absoluteUrl);
        NavigationUtils.launchAbsoluteUrl(absoluteUrl);
        ExtentReportManager.getTest().info("Navigated to: " + absoluteUrl);
    }

    /**
     * Navigates the browser back one step in history.
     */
    @When("the user navigates back")
    public void theUserNavigatesBack() {
        log.info("Step: Browser back");
        NavigationUtils.back();
        ExtentReportManager.getTest().info("Navigated back. URL: "
            + NavigationUtils.getCurrentUrl());
    }

    /**
     * Navigates the browser forward one step in history.
     */
    @When("the user navigates forward")
    public void theUserNavigatesForward() {
        log.info("Step: Browser forward");
        NavigationUtils.forward();
        ExtentReportManager.getTest().info("Navigated forward. URL: "
            + NavigationUtils.getCurrentUrl());
    }

    /**
     * Refreshes the current page.
     */
    @When("the user refreshes the page")
    public void theUserRefreshesThePage() {
        log.info("Step: Refreshing page");
        NavigationUtils.refresh();
        ExtentReportManager.getTest().info("Page refreshed. URL: "
            + NavigationUtils.getCurrentUrl());
    }

    // ─────────────────────────────────────────────────────────
    // Then — URL & title assertions
    // ─────────────────────────────────────────────────────────

    /**
     * Asserts the current URL contains the expected fragment.
     *
     * @param expectedFragment URL substring expected in the current URL
     */
    @Then("the current URL should contain {string}")
    public void theCurrentUrlShouldContain(String expectedFragment) {
        log.info("Step: Asserting URL contains [{}]", expectedFragment);
        String currentUrl = NavigationUtils.getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains(expectedFragment),
            "Expected URL to contain [" + expectedFragment + "] but got: [" + currentUrl + "]"
        );
        ExtentReportManager.getTest().pass(
            "URL verified: [" + currentUrl + "] contains [" + expectedFragment + "]");
    }

    /**
     * Asserts the current URL exactly equals the expected value.
     *
     * @param expectedUrl the exact URL expected
     */
    @Then("the current URL should be {string}")
    public void theCurrentUrlShouldBe(String expectedUrl) {
        log.info("Step: Asserting URL equals [{}]", expectedUrl);
        String currentUrl = NavigationUtils.getCurrentUrl();
        Assert.assertEquals(currentUrl, expectedUrl,
            "URL mismatch: expected [" + expectedUrl + "] got [" + currentUrl + "]");
        ExtentReportManager.getTest().pass("URL verified: " + currentUrl);
    }

    /**
     * Asserts the page title contains the expected text.
     *
     * @param expectedTitle expected substring in the page title
     */
    @Then("the page title should contain {string}")
    public void thePageTitleShouldContain(String expectedTitle) {
        log.info("Step: Asserting title contains [{}]", expectedTitle);
        String actualTitle = NavigationUtils.getPageTitle();
        Assert.assertTrue(
            actualTitle.contains(expectedTitle),
            "Expected title to contain [" + expectedTitle + "] but got: [" + actualTitle + "]"
        );
        ExtentReportManager.getTest().pass(
            "Title verified: [" + actualTitle + "] contains [" + expectedTitle + "]");
    }

    /**
     * Waits for the URL to contain the given fragment before asserting.
     * Useful for pages that redirect after an action.
     *
     * @param urlFragment URL substring to wait for
     */
    @Then("the user should be redirected to a URL containing {string}")
    public void theUserShouldBeRedirectedToUrlContaining(String urlFragment) {
        log.info("Step: Waiting for redirect to URL containing [{}]", urlFragment);
        NavigationUtils.waitForUrlContains(urlFragment);
        ExtentReportManager.getTest().pass(
            "Redirect verified. Current URL: " + NavigationUtils.getCurrentUrl());
    }
}
