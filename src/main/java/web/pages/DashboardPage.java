package web.pages;

import core.base.BasePage;
import org.openqa.selenium.By;

/**
 * Page Object for the Dashboard page (post-login landing page).
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class DashboardPage extends BasePage {

    private static final String DASHBOARD_PATH = "/dashboard";

    private static final By PAGE_HEADING       = By.cssSelector("h1.dashboard-title");
    private static final By USER_PROFILE_ICON  = By.id("user-profile-icon");
    private static final By LOGOUT_BUTTON      = By.id("logout-btn");
    private static final By NAV_MENU           = By.cssSelector("nav.main-menu");
    private static final By NOTIFICATION_BADGE = By.cssSelector(".notification-badge");

    // ─────────────────────────────────────────────────────────
    // Launch methods
    // ─────────────────────────────────────────────────────────

    /**
     * Navigates directly to the dashboard page.
     * Use when a test needs to land on the dashboard without going through login
     * (e.g., when a session cookie is pre-set in a hook).
     *
     * @return this {@link DashboardPage} instance
     */
    public DashboardPage open() {
        log.info("Opening dashboard: {}", DASHBOARD_PATH);
        launchPage(DASHBOARD_PATH);
        return this;
    }

    // ─────────────────────────────────────────────────────────
    // Page actions & state getters
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the dashboard page heading text.
     *
     * @return page heading
     */
    public String getPageHeading() {
        return getText(PAGE_HEADING);
    }

    /**
     * Checks if the user is on the dashboard (profile icon visible).
     *
     * @return true if profile icon is displayed
     */
    public boolean isOnDashboard() {
        return isDisplayed(USER_PROFILE_ICON);
    }

    /**
     * Logs out the current user by clicking the logout button.
     */
    public void logout() {
        log.info("Logging out");
        click(LOGOUT_BUTTON);
    }

    /**
     * Returns the notification count from the badge.
     *
     * @return notification count as integer, 0 if badge not visible
     */
    public int getNotificationCount() {
        if (!isDisplayed(NOTIFICATION_BADGE)) return 0;
        try {
            return Integer.parseInt(getText(NOTIFICATION_BADGE));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
