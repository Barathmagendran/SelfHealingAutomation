package api.clients;

import api.endpoints.Endpoints;
import api.models.UserRequest;
import api.models.UserResponse;
import core.base.BaseAPI;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.restassured.RestAssured.given;

/**
 * API client for User-related endpoints.
 *
 * <p>Wraps RestAssured calls into semantic, reusable methods.
 * Each method corresponds to a distinct API operation.
 * HTTP status validation is intentionally left to the caller (step definitions)
 * to allow both positive and negative test scenarios.
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class UserApiClient extends BaseAPI {

    private static final Logger log = LogManager.getLogger(UserApiClient.class);

    // ─────────────────────────────────────────────────────────
    // READ operations
    // ─────────────────────────────────────────────────────────

    /**
     * Retrieves all users.
     *
     * @return the API response
     */
    public Response getAllUsers() {
        log.info("GET {}", Endpoints.USERS);
        return given(getRequestSpec())
            .when()
                .get(Endpoints.USERS)
            .then()
                .log().all()
                .extract().response();
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the user's ID
     * @return the API response
     */
    public Response getUserById(int userId) {
        log.info("GET {} [id={}]", Endpoints.USER_BY_ID, userId);
        return given(getRequestSpec())
            .pathParam("id", userId)
            .when()
                .get(Endpoints.USER_BY_ID)
            .then()
                .log().all()
                .extract().response();
    }

    /**
     * Retrieves the authenticated user's profile.
     *
     * @return the API response
     */
    public Response getUserProfile() {
        log.info("GET {}", Endpoints.USER_PROFILE);
        return given(getRequestSpec())
            .when()
                .get(Endpoints.USER_PROFILE)
            .then()
                .extract().response();
    }

    // ─────────────────────────────────────────────────────────
    // WRITE operations
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a new user with the provided payload.
     *
     * @param userRequest the user creation request body
     * @return the API response
     */
    public Response createUser(UserRequest userRequest) {
        log.info("POST {} — creating user: {}", Endpoints.USERS, userRequest.getEmail());
        return given(getRequestSpec())
            .body(userRequest)
            .when()
                .post(Endpoints.USERS)
            .then()
                .log().all()
                .extract().response();
    }

    /**
     * Updates an existing user by ID.
     *
     * @param userId      the user's ID
     * @param userRequest the update request body
     * @return the API response
     */
    public Response updateUser(int userId, UserRequest userRequest) {
        log.info("PUT {} [id={}]", Endpoints.USER_BY_ID, userId);
        return given(getRequestSpec())
            .pathParam("id", userId)
            .body(userRequest)
            .when()
                .put(Endpoints.USER_BY_ID)
            .then()
                .log().all()
                .extract().response();
    }

    /**
     * Partially updates a user (PATCH).
     *
     * @param userId      the user's ID
     * @param userRequest partial update payload
     * @return the API response
     */
    public Response patchUser(int userId, UserRequest userRequest) {
        log.info("PATCH {} [id={}]", Endpoints.USER_BY_ID, userId);
        return given(getRequestSpec())
            .pathParam("id", userId)
            .body(userRequest)
            .when()
                .patch(Endpoints.USER_BY_ID)
            .then()
                .log().all()
                .extract().response();
    }

    /**
     * Deletes a user by ID.
     *
     * @param userId the user's ID
     * @return the API response
     */
    public Response deleteUser(int userId) {
        log.info("DELETE {} [id={}]", Endpoints.USER_BY_ID, userId);
        return given(getRequestSpec())
            .pathParam("id", userId)
            .when()
                .delete(Endpoints.USER_BY_ID)
            .then()
                .log().all()
                .extract().response();
    }

    // ─────────────────────────────────────────────────────────
    // Response helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Deserializes a response body into a {@link UserResponse} object.
     *
     * @param response the API response
     * @return deserialized UserResponse
     */
    public UserResponse extractUserResponse(Response response) {
        return response.as(UserResponse.class);
    }
}
