# =============================================================
# Feature: User Login — Web
# Domain: Authentication
# Platform: Web (Selenium)
# =============================================================

@web @authentication @smoke
Feature: User Login - Web

  As a registered user
  I want to log in to the application
  So that I can access my account and use its features

  Background:
    Given the user is on the login page

  # ─────────────────────────────────────────────────────────
  # Happy Path
  # ─────────────────────────────────────────────────────────

  @smoke @regression @test1
  Scenario: Successful login with valid credentials
    When the user enters email "testuser@example.com" and password "Password@123"
    And the user clicks the login button
    Then the user should be redirected to the dashboard
    And the dashboard heading should be "Welcome Back!"

  @regression
  Scenario: Successful login redirects to dashboard with welcome message
    When the user logs in with email "admin@example.com" and password "Admin@123"
    Then the user should be redirected to the dashboard
    And the welcome banner should contain "admin"

  # ─────────────────────────────────────────────────────────
  # Negative / Edge Cases
  # ─────────────────────────────────────────────────────────

  @regression @negative
  Scenario: Login fails with invalid password
    When the user enters email "testuser@example.com" and password "WrongPassword"
    And the user clicks the login button
    Then an error message "Invalid email or password." should be displayed
    And the user should remain on the login page

  @regression @negative
  Scenario: Login fails with unregistered email
    When the user enters email "notregistered@example.com" and password "Password@123"
    And the user clicks the login button
    Then an error message "Invalid email or password." should be displayed

  @regression @negative
  Scenario: Login fails with empty credentials
    When the user enters email "" and password ""
    And the user clicks the login button
    Then an error message "Email and password are required." should be displayed

  # ─────────────────────────────────────────────────────────
  # Data-Driven
  # ─────────────────────────────────────────────────────────

  @regression @data-driven
  Scenario Outline: Login with multiple user roles
    When the user logs in with email "<email>" and password "<password>"
    Then the user should be redirected to the dashboard
    And the dashboard heading should be "<expectedHeading>"

    Examples:
      | email                    | password     | expectedHeading     |
      | admin@example.com        | Admin@123    | Welcome Back!       |
      | manager@example.com      | Manager@123  | Welcome Back!       |
      | viewer@example.com       | Viewer@123   | Welcome Back!       |

  @regression @data-driven
  Scenario: Login using externalized test data file
    When the user logs in with "validUser" credentials from test data
    Then the user should be redirected to the dashboard

  # ─────────────────────────────────────────────────────────
  # YAML Test Data
  # ─────────────────────────────────────────────────────────

  @regression @yaml
  Scenario: Successful login using adminUser from testData.yml
    When the user logs in as "adminUser"
    Then the user should be redirected to the dashboard

  @regression @yaml
  Scenario: Successful login using viewerUser from testData.yml
    When the user logs in as "viewerUser"
    Then the user should be redirected to the dashboard

  @regression @yaml @negative
  Scenario: Login fails for lockedUser defined in testData.yml
    When the user enters credentials for "lockedUser"
    And the user clicks the login button
    Then an error message "Your account has been locked." should be displayed
