# =============================================================
# Feature: Mobile App Login
# Domain: Authentication
# Platform: Mobile (Appium - Android/iOS)
# =============================================================

@mobile @authentication @smoke
Feature: Mobile App Login

  As a mobile app user
  I want to log in to the mobile application
  So that I can access my account on my device

  Background:
    Given the mobile app is launched

  @smoke @android @ios
  Scenario: Successful login on mobile with valid credentials
    When the mobile user enters username "mobileuser" and password "Mobile@123"
    And the mobile user taps the login button
    Then the mobile home screen should be displayed

  @regression @negative @android @ios
  Scenario: Mobile login fails with wrong password
    When the mobile user enters username "mobileuser" and password "WrongPass"
    And the mobile user taps the login button
    Then the mobile error message "Invalid credentials. Please try again." should be shown
