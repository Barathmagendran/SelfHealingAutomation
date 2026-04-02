# =============================================================
# Feature: User API — CRUD Operations
# Domain: Users
# Platform: API (RestAssured)
# =============================================================

@api @users @smoke
Feature: User API - CRUD Operations

  As an API consumer
  I want to manage users via the REST API
  So that I can create, read, update, and delete user resources

  Background:
    Given the API is available
    And the user is authenticated with email "admin@example.com" and password "Admin@123"

  # ─────────────────────────────────────────────────────────
  # GET Users
  # ─────────────────────────────────────────────────────────

  @smoke @regression
  Scenario: Retrieve all users returns 200 with a non-empty list
    When a GET request is made to retrieve all users
    Then the response status code should be 200
    And the response should contain a list of users
    And the response time should be less than 3000 milliseconds

  @regression
  Scenario: Retrieve a specific user by valid ID
    When a GET request is made to retrieve user with id 1
    Then the response status code should be 200
    And the response body should contain user email "admin@example.com"

  @regression @negative
  Scenario: Retrieve a user with a non-existent ID returns 404
    When a GET request is made to retrieve user with id 99999
    Then the response status code should be 404

  # ─────────────────────────────────────────────────────────
  # POST — Create User
  # ─────────────────────────────────────────────────────────

  @regression
  Scenario: Create a new user with valid data returns 201
    When a POST request is made to create a user with the following data:
      | firstName | John            |
      | lastName  | Doe             |
      | email     | john.doe@qa.com |
      | role      | viewer          |
    Then the response status code should be 201
    And the response body should contain user email "john.doe@qa.com"

  @regression @schema
  Scenario: Create user response matches JSON schema
    When a POST request is made to create a user with the following data:
      | firstName | Schema          |
      | lastName  | Test            |
      | email     | schema@qa.com   |
      | role      | viewer          |
    Then the response status code should be 201
    And the response should match schema "user_schema.json"

  @regression @negative
  Scenario: Create user with duplicate email returns 409
    When a POST request is made to create a user with the following data:
      | firstName | Duplicate       |
      | lastName  | User            |
      | email     | admin@example.com |
      | role      | viewer          |
    Then the response status code should be 409

  # ─────────────────────────────────────────────────────────
  # DELETE
  # ─────────────────────────────────────────────────────────

  @regression
  Scenario: Delete an existing user returns 204
    When a DELETE request is made to delete user with id 5
    Then the response status code should be 204

  # ─────────────────────────────────────────────────────────
  # Data-Driven
  # ─────────────────────────────────────────────────────────

  @regression @data-driven
  Scenario: Create user from external test data file
    When a POST request is made to create a user from test data file "new_user.json"
    Then the response status code should be 201

  # ─────────────────────────────────────────────────────────
  # YAML Test Data — credentials and payloads from testData.yml
  # ─────────────────────────────────────────────────────────

  @regression @yaml
  Scenario: Authenticate using YAML-defined valid credentials
    Given the user is authenticated using "validCredentials" credentials from test data
    When a GET request is made to retrieve all users
    Then the response status code should be 200

  @regression @yaml
  Scenario: Create user using YAML-defined payload
    When a POST request is made using "createValid" user payload from YAML
    Then the response status code should be 201
    And the response body should contain user email "api.created@qa.example.com"

  @regression @yaml
  Scenario: Create admin user using YAML-defined payload
    When a POST request is made using "createAdmin" user payload from YAML
    Then the response status code should be 201

  @regression @yaml @negative
  Scenario: Create user with duplicate email using YAML payload returns 409
    When a POST request is made using "duplicateEmail" user payload from YAML
    Then the response status code should be 409

  @regression @yaml
  Scenario: Response time within YAML-configured limit
    When a GET request is made to retrieve all users
    Then the response status code should be 200
    And the response time should be within the configured limit
