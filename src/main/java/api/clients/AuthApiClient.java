package api.clients;

import api.endpoints.Endpoints;
import api.models.LoginRequest;
import api.models.LoginResponse;
import core.base.BaseAPI;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.restassured.RestAssured.given;

/**
 * API client for Authentication endpoints.
 *
 * <p>Handles login, logout, and token refresh operations.
 * Stores the auth token in-memory for reuse across test steps within the same scenario.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class AuthApiClient extends BaseAPI {

    private static final Logger log = LogManager.getLogger(AuthApiClient.class);

    /** Holds the bearer token after a successful login */
    private String authToken;

    /**
     * Authenticates with email and password, stores the returned token.
     *
     * @param email    user email
     * @param password user password
     * @return the raw API response (for status/body assertion in step definitions)
     */
    public Response login(String email, String password) {
        log.info("POST {} — logging in as: {}", Endpoints.AUTH_LOGIN, email);

        LoginRequest request = new LoginRequest(email, password);

        Response response = given(getRequestSpec())
            .body(request)
            .when()
                .post(Endpoints.AUTH_LOGIN)
            .then()
                .log().all()
                .extract().response();

        // Cache token if login succeeded
        if (response.statusCode() == 200) {
            LoginResponse loginResponse = response.as(LoginResponse.class);
            this.authToken = loginResponse.getToken();
            log.info("Login successful — token cached.");
        }

        return response;
    }

    /**
     * Logs out the current user.
     *
     * @return the API response
     */
    public Response logout() {
        log.info("POST {}", Endpoints.AUTH_LOGOUT);
        return given(getRequestSpec())
            .when()
                .post(Endpoints.AUTH_LOGOUT)
            .then()
                .log().all()
                .extract().response();
    }

    /**
     * Returns the stored authentication token.
     *
     * @return Bearer token string, or null if not authenticated
     */
    public String getAuthToken() {
        return authToken;
    }
}
