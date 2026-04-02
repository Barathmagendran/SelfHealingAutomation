# ==============================================================
# Feature: Browser Navigation
# Domain: Navigation / Core
# Platform: Web
# ==============================================================
# Demonstrates all NavigationUtils-backed steps.
# These scenarios test the navigation layer itself and also
# serve as living documentation for how to use navigation steps.
# ==============================================================

@web @navigation @smoke
Feature: Browser Navigation

  # ─────────────────────────────────────────────────────────
  # Base URL Launch
  # ─────────────────────────────────────────────────────────

  @smoke
  Scenario: Application launches at the configured base URL
    Given the application is launched
    Then the current URL should contain "example.com"

  @regression
  Scenario: Navigating to the login page via relative path
    Given the application is launched
    When the user navigates to "/login"
    Then the current URL should contain "/login"
    And the page title should contain "Login"

  @regression
  Scenario: Login page opens directly via LoginPage.open()
    Given the user is on the login page
    Then the current URL should contain "/login"

  # ─────────────────────────────────────────────────────────
  # Relative Page Navigation
  # ─────────────────────────────────────────────────────────

  @regression
  Scenario: Navigate to multiple pages sequentially
    Given the application is launched
    When the user navigates to "/login"
    Then the current URL should contain "/login"
    When the user logs in with email "admin@example.com" and password "Admin@123"
    Then the user should be redirected to a URL containing "/dashboard"

  # ─────────────────────────────────────────────────────────
  # Browser History
  # ─────────────────────────────────────────────────────────

  @regression
  Scenario: Browser back navigation works correctly
    Given the application is launched
    When the user navigates to "/login"
    And the user navigates back
    Then the current URL should contain "example.com"

  @regression
  Scenario: Page refresh retains the current URL
    Given the user is on the login page
    When the user refreshes the page
    Then the current URL should contain "/login"

  # ─────────────────────────────────────────────────────────
  # YAML-driven navigation
  # ─────────────────────────────────────────────────────────

  @regression @yaml
  Scenario: Authenticated user lands on dashboard after YAML login
    Given the application is launched
    When the user navigates to "/login"
    And the user logs in as "adminUser"
    Then the user should be redirected to a URL containing "/dashboard"
    And the page title should contain "Dashboard"
