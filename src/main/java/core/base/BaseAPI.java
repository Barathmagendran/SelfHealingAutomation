package core.base;

import core.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Base class for all API client classes.
 *
 * <p>Provides:
 * <ul>
 *   <li>Pre-configured {@link RequestSpecification} with base URL and auth headers</li>
 *   <li>Reusable {@link ResponseSpecification} for common validations</li>
 *   <li>Request/response logging captured for Extent Report attachment</li>
 * </ul>
 *
 * <p>API client classes extend this and call {@link #getRequestSpec()} to build requests:
 * <pre>
 *   given(getRequestSpec())
 *       .body(payload)
 *   .when()
 *       .post(Endpoints.LOGIN)
 *   .then()
 *       .spec(getResponseSpec(200));
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public abstract class BaseAPI {

    protected static final Logger log = LogManager.getLogger(BaseAPI.class);

    /** Shared stream for capturing RestAssured logs */
    protected final ByteArrayOutputStream logCapture = new ByteArrayOutputStream();
    protected final PrintStream logStream = new PrintStream(logCapture, true);

    static {
        // Set global base URI from config
        RestAssured.baseURI = ConfigReader.get("api.base.url",
            ConfigReader.get("base.url", "http://localhost:8080"));
        log.info("RestAssured baseURI set to: {}", RestAssured.baseURI);
    }

    /**
     * Builds a base {@link RequestSpecification} with common headers and auth.
     * <p>Override in subclasses to add endpoint-specific configurations.
     *
     * @return configured RequestSpecification
     */
    protected RequestSpecification getRequestSpec() {
        String authToken = ConfigReader.get("api.auth.token", "");
        String contentType = ConfigReader.get("api.content.type", "application/json");

        RequestSpecBuilder builder = new RequestSpecBuilder()
            .setContentType(contentType)
            .setAccept(ContentType.JSON)
            .log(LogDetail.ALL)
            .addFilter(new RequestLoggingFilter(logStream))
            .addFilter(new ResponseLoggingFilter(logStream));

        // Attach bearer token if configured
        if (!authToken.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }

        return builder.build();
    }

    /**
     * Builds a {@link RequestSpecification} with Basic authentication.
     *
     * @param username basic auth username
     * @param password basic auth password
     * @return configured RequestSpecification
     */
    protected RequestSpecification getRequestSpecWithBasicAuth(String username, String password) {
        return new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setAuth(RestAssured.basic(username, password))
            .log(LogDetail.ALL)
            .build();
    }

    /**
     * Builds a {@link ResponseSpecification} validating the expected HTTP status code.
     *
     * @param expectedStatusCode expected HTTP response status
     * @return configured ResponseSpecification
     */
    protected ResponseSpecification getResponseSpec(int expectedStatusCode) {
        return new ResponseSpecBuilder()
            .expectStatusCode(expectedStatusCode)
            .log(LogDetail.ALL)
            .build();
    }

    /**
     * Returns the captured request/response log as a string.
     * Useful for attaching API logs to test reports.
     *
     * @return captured log string
     */
    protected String getCapturedLogs() {
        return logCapture.toString();
    }

    /**
     * Resets the log capture buffer. Call between test steps if needed.
     */
    protected void resetLogCapture() {
        logCapture.reset();
    }
}
