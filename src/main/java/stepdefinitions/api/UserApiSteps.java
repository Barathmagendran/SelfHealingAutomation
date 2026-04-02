package stepdefinitions.api;

import api.clients.AuthApiClient;
import api.clients.UserApiClient;
import api.models.UserRequest;
import api.models.UserResponse;
import core.config.YamlDataReader;
import core.config.model.TestData.ApiData;
import core.reporting.ExtentReportManager;
import core.utils.TestDataLoader;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.Map;

/**
 * Step definitions for API test scenarios.
 *
 * <p>Covers authentication and user CRUD operations.
 * Each step logs details to Extent Reports for traceability.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class UserApiSteps {

    private static final Logger log = LogManager.getLogger(UserApiSteps.class);

    private final AuthApiClient authClient = new AuthApiClient();
    private final UserApiClient userClient = new UserApiClient();

    /** Stores the last API response for assertion steps */
    private Response lastResponse;

    /** Stores a created user's ID across steps */
    private int createdUserId;

    // ─────────────────────────────────────────────────────────
    // Given Steps
    // ─────────────────────────────────────────────────────────

    @Given("the API is available")
    public void theApiIsAvailable() {
        log.info("Step: Assuming API is available (base URL configured)");
        ExtentReportManager.getTest().info("API availability assumed from config.");
    }

    @Given("the user is authenticated with email {string} and password {string}")
    public void theUserIsAuthenticated(String email, String password) {
        log.info("Step: Authenticating via API as {}", email);
        lastResponse = authClient.login(email, password);
        Assert.assertEquals(lastResponse.statusCode(), 200,
            "Authentication failed with status: " + lastResponse.statusCode());
        ExtentReportManager.getTest().pass("Authentication successful for: " + email);
    }

    /**
     * Authenticates using YAML-defined credentials from testData.yml > api > auth.
     * Example: Given the user is authenticated using "validCredentials"
     */
    @Given("the user is authenticated using {string} credentials from test data")
    public void theUserIsAuthenticatedFromYaml(String credentialKey) {
        ApiData.AuthData.Credentials creds;
        if ("validCredentials".equals(credentialKey)) {
            creds = YamlDataReader.getValidApiCredentials();
        } else if ("invalidCredentials".equals(credentialKey)) {
            creds = YamlDataReader.getInvalidApiCredentials();
        } else {
            throw new IllegalArgumentException("Unknown credential key: " + credentialKey);
        }
        log.info("Step: API auth from YAML [{}] email={}", credentialKey, creds.getEmail());
        lastResponse = authClient.login(creds.getEmail(), creds.getPassword());
        ExtentReportManager.getTest().info("Auth via YAML [" + credentialKey + "]");
    }

    // ─────────────────────────────────────────────────────────
    // When Steps
    // ─────────────────────────────────────────────────────────

    @When("a GET request is made to retrieve all users")
    public void aGetRequestIsMadeToRetrieveAllUsers() {
        log.info("Step: GET /users");
        lastResponse = userClient.getAllUsers();
        ExtentReportManager.getTest().info("GET /users executed.");
        logResponseToReport();
    }

    @When("a GET request is made to retrieve user with id {int}")
    public void aGetRequestIsMadeToRetrieveUserById(int userId) {
        log.info("Step: GET /users/{}", userId);
        lastResponse = userClient.getUserById(userId);
        ExtentReportManager.getTest().info("GET /users/" + userId + " executed.");
        logResponseToReport();
    }

    @When("a POST request is made to create a user with the following data:")
    public void aPostRequestIsMadeToCreateUser(io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        UserRequest request = UserRequest.builder()
            .firstName(data.get("firstName"))
            .lastName(data.get("lastName"))
            .email(data.get("email"))
            .role(data.get("role"))
            .build();

        log.info("Step: POST /users — creating user: {}", request.getEmail());
        lastResponse = userClient.createUser(request);
        ExtentReportManager.getTest().info("POST /users executed for: " + request.getEmail());
        logResponseToReport();

        if (lastResponse.statusCode() == 201) {
            createdUserId = lastResponse.jsonPath().getInt("id");
            log.info("Created user ID: {}", createdUserId);
        }
    }

    @When("a POST request is made to create a user from test data file {string}")
    public void aPostRequestIsMadeToCreateUserFromTestData(String dataFile) {
        Map<String, String> data = TestDataLoader.loadJsonAsMap("json/" + dataFile);
        UserRequest request = UserRequest.builder()
            .firstName(data.get("firstName"))
            .lastName(data.get("lastName"))
            .email(data.get("email"))
            .role(data.get("role"))
            .build();

        log.info("Step: Creating user from JSON test data: {}", dataFile);
        lastResponse = userClient.createUser(request);
        logResponseToReport();
    }

    /**
     * Creates a user using a named payload from testData.yml > api > userPayloads.
     * Example: When a POST request is made using "createValid" user payload from YAML
     */
    @When("a POST request is made using {string} user payload from YAML")
    public void aPostRequestMadeFromYamlPayload(String payloadKey) {
        ApiData.UserPayload p = YamlDataReader.getApiUserPayload(payloadKey);
        UserRequest request = UserRequest.builder()
            .firstName(p.getFirstName())
            .lastName(p.getLastName())
            .email(p.getEmail())
            .role(p.getRole())
            .phone(p.getPhone())
            .build();

        log.info("Step: POST /users using YAML payload [{}] email={}", payloadKey, p.getEmail());
        lastResponse = userClient.createUser(request);
        logResponseToReport();

        if (lastResponse.statusCode() == 201) {
            createdUserId = lastResponse.jsonPath().getInt("id");
        }
    }

    /**
     * Asserts the response time is within the YAML-defined max.
     * Reads from testData.yml > api > expectedResponses.maxResponseTimeMs
     */
    @And("the response time should be within the configured limit")
    public void theResponseTimeShouldBeWithinConfiguredLimit() {
        int maxMs = YamlDataReader.getApiExpectedResponses().getMaxResponseTimeMs();
        long actual = lastResponse.time();
        Assert.assertTrue(actual < maxMs,
            "Response time " + actual + "ms exceeded YAML limit " + maxMs + "ms");
        ExtentReportManager.getTest().pass("Response time OK: " + actual + "ms < " + maxMs + "ms");
    }

    @When("a DELETE request is made to delete user with id {int}")
    public void aDeleteRequestIsMadeToDeleteUser(int userId) {
        log.info("Step: DELETE /users/{}", userId);
        lastResponse = userClient.deleteUser(userId);
        logResponseToReport();
    }

    // ─────────────────────────────────────────────────────────
    // Then Steps
    // ─────────────────────────────────────────────────────────

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        log.info("Step: Verifying status code = {}", expectedStatus);
        Assert.assertEquals(lastResponse.statusCode(), expectedStatus,
            "Response status code mismatch. Body: " + lastResponse.body().asString());
        ExtentReportManager.getTest().pass("Status code verified: " + expectedStatus);
    }

    @Then("the response should contain a list of users")
    public void theResponseShouldContainAListOfUsers() {
        Assert.assertFalse(lastResponse.jsonPath().getList("$").isEmpty(),
            "Expected a non-empty list of users, but got empty response.");
        ExtentReportManager.getTest().pass("User list verified as non-empty.");
    }

    @Then("the response body should contain user email {string}")
    public void theResponseBodyShouldContainUserEmail(String expectedEmail) {
        String actualEmail = lastResponse.jsonPath().getString("email");
        Assert.assertEquals(actualEmail, expectedEmail,
            "Email mismatch in response body.");
        ExtentReportManager.getTest().pass("Email verified: " + actualEmail);
    }

    @Then("the response should match schema {string}")
    public void theResponseShouldMatchSchema(String schemaFile) {
        log.info("Step: Validating response against schema: {}", schemaFile);
        lastResponse.then().assertThat()
            .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/" + schemaFile));
        ExtentReportManager.getTest().pass("Schema validation passed: " + schemaFile);
    }

    @And("the response time should be less than {int} milliseconds")
    public void theResponseTimeShouldBeLessThan(int maxMs) {
        long actualTime = lastResponse.time();
        Assert.assertTrue(actualTime < maxMs,
            "Response time " + actualTime + "ms exceeded threshold of " + maxMs + "ms");
        ExtentReportManager.getTest().pass("Response time OK: " + actualTime + "ms < " + maxMs + "ms");
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void logResponseToReport() {
        if (ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().info(
                "<pre>Status: " + lastResponse.statusCode() +
                "\nBody: " + lastResponse.body().asPrettyString() + "</pre>"
            );
        }
    }
}
