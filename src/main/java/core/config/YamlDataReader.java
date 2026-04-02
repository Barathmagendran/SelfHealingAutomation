package core.config;

import core.config.model.TestData;
import core.config.model.TestData.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Clean, domain-scoped accessor for all test data in {@code testData.yml}.
 *
 * <p>Acts as a facade over {@link YamlConfigLoader#getTestData()} to provide
 * short, readable method names for use directly in step definitions and page objects.
 *
 * <p><b>Usage examples:</b>
 * <pre>
 *   // Get a specific user
 *   UserData admin = YamlDataReader.getUser("adminUser");
 *   String email   = admin.getEmail();
 *   String role    = admin.getRole();
 *
 *   // Login in step definition
 *   UserData user = YamlDataReader.getUser("validUser");
 *   loginPage.login(user.getEmail(), user.getPassword());
 *
 *   // API payloads
 *   ApiData.UserPayload payload = YamlDataReader.getApiUserPayload("createValid");
 *
 *   // Product data
 *   ProductData laptop = YamlDataReader.getProduct("laptop");
 *   Assert.assertEquals(laptop.getPrice(), 1299.99);
 *
 *   // Validation sets
 *   List&lt;String&gt; validEmails = YamlDataReader.getValidEmails();
 *
 *   // Mobile credentials
 *   String mobilePass = YamlDataReader.getMobileUser("validUser").getPassword();
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public final class YamlDataReader {

    private static final Logger log = LogManager.getLogger(YamlDataReader.class);

    private YamlDataReader() {}

    // ─────────────────────────────────────────────────────────
    // Users
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the user data for the given key from {@code testData.yml > users}.
     *
     * @param userKey key (e.g., "validUser", "adminUser", "lockedUser")
     * @return UserData for the key
     * @throws RuntimeException if the key does not exist
     */
    public static UserData getUser(String userKey) {
        UserData user = YamlConfigLoader.getTestData().getUsers().get(userKey);
        if (user == null) {
            throw new RuntimeException(
                "Test data not found for user key: [" + userKey + "]. " +
                "Check testData.yml > users section.");
        }
        log.debug("Loaded user data for key: [{}]", userKey);
        return user;
    }

    /**
     * Returns all user entries as a map.
     *
     * @return map of userKey → UserData
     */
    public static Map<String, UserData> getAllUsers() {
        return YamlConfigLoader.getTestData().getUsers();
    }

    // ─────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────

    /**
     * Returns registration data for the given key.
     *
     * @param key e.g., "validRegistration", "duplicateEmail"
     * @return RegistrationData
     */
    public static RegistrationData getRegistrationData(String key) {
        RegistrationData data = YamlConfigLoader.getTestData().getRegistration().get(key);
        if (data == null) {
            throw new RuntimeException(
                "Registration data not found for key: [" + key + "]");
        }
        return data;
    }

    // ─────────────────────────────────────────────────────────
    // Products
    // ─────────────────────────────────────────────────────────

    /**
     * Returns product test data for the given key.
     *
     * @param productKey e.g., "laptop", "smartphone", "outOfStockItem"
     * @return ProductData
     */
    public static ProductData getProduct(String productKey) {
        ProductData product = YamlConfigLoader.getTestData().getProducts().get(productKey);
        if (product == null) {
            throw new RuntimeException(
                "Product data not found for key: [" + productKey + "]. " +
                "Check testData.yml > products section.");
        }
        return product;
    }

    // ─────────────────────────────────────────────────────────
    // Orders
    // ─────────────────────────────────────────────────────────

    /**
     * Returns order test data for the given key.
     *
     * @param orderKey e.g., "standardOrder", "bulkOrder"
     * @return OrderData
     */
    public static OrderData getOrder(String orderKey) {
        OrderData order = YamlConfigLoader.getTestData().getOrders().get(orderKey);
        if (order == null) {
            throw new RuntimeException(
                "Order data not found for key: [" + orderKey + "]");
        }
        return order;
    }

    // ─────────────────────────────────────────────────────────
    // API Data
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the full API test data section.
     *
     * @return ApiData object
     */
    public static ApiData getApiData() {
        return YamlConfigLoader.getTestData().getApi();
    }

    /**
     * Returns valid API auth credentials.
     *
     * @return valid Credentials
     */
    public static ApiData.AuthData.Credentials getValidApiCredentials() {
        return YamlConfigLoader.getTestData().getApi().getAuth().getValidCredentials();
    }

    /**
     * Returns invalid API auth credentials (for negative tests).
     *
     * @return invalid Credentials
     */
    public static ApiData.AuthData.Credentials getInvalidApiCredentials() {
        return YamlConfigLoader.getTestData().getApi().getAuth().getInvalidCredentials();
    }

    /**
     * Returns a user payload for API creation/update tests.
     *
     * @param payloadKey e.g., "createValid", "createAdmin", "updatePayload", "duplicateEmail"
     * @return UserPayload
     */
    public static ApiData.UserPayload getApiUserPayload(String payloadKey) {
        ApiData.UserPayload payload = YamlConfigLoader.getTestData()
            .getApi().getUserPayloads().get(payloadKey);
        if (payload == null) {
            throw new RuntimeException(
                "API user payload not found for key: [" + payloadKey + "]. " +
                "Check testData.yml > api > userPayloads section.");
        }
        return payload;
    }

    /**
     * Returns the expected response codes and limits for API tests.
     *
     * @return ExpectedResponses
     */
    public static ApiData.ExpectedResponses getApiExpectedResponses() {
        return YamlConfigLoader.getTestData().getApi().getExpectedResponses();
    }

    // ─────────────────────────────────────────────────────────
    // Mobile Data
    // ─────────────────────────────────────────────────────────

    /**
     * Returns mobile login screen credentials for the given user type.
     *
     * @param userType "validUser" | "invalidUser" | "lockedUser"
     * @return mobile user credentials
     */
    public static MobileData.LoginScreenData.MobileUser getMobileUser(String userType) {
        MobileData.LoginScreenData loginScreen =
            YamlConfigLoader.getTestData().getMobile().getLoginScreen();
        return switch (userType) {
            case "validUser"   -> loginScreen.getValidUser();
            case "invalidUser" -> loginScreen.getInvalidUser();
            case "lockedUser"  -> loginScreen.getLockedUser();
            default -> throw new RuntimeException(
                "Unknown mobile user type: [" + userType + "]. " +
                "Valid: validUser, invalidUser, lockedUser");
        };
    }

    // ─────────────────────────────────────────────────────────
    // Search Data
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the list of valid search keywords.
     *
     * @return list of keyword strings
     */
    public static List<String> getValidSearchKeywords() {
        return YamlConfigLoader.getTestData().getSearch().getValidKeywords();
    }

    /**
     * Returns a keyword guaranteed to return no search results.
     *
     * @return empty-result keyword string
     */
    public static String getEmptyResultKeyword() {
        return YamlConfigLoader.getTestData().getSearch().getEmptyResultKeyword();
    }

    // ─────────────────────────────────────────────────────────
    // Validation Data
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the list of valid email addresses for positive tests.
     */
    public static List<String> getValidEmails() {
        return YamlConfigLoader.getTestData().getValidation().getEmail().getValid();
    }

    /**
     * Returns the list of invalid email addresses for negative tests.
     */
    public static List<String> getInvalidEmails() {
        return YamlConfigLoader.getTestData().getValidation().getEmail().getInvalid();
    }

    /**
     * Returns all password test data (strong, weak, missing fields, etc.).
     */
    public static ValidationData.PasswordData getPasswordData() {
        return YamlConfigLoader.getTestData().getValidation().getPassword();
    }

    /**
     * Returns the list of valid phone numbers.
     */
    public static List<String> getValidPhoneNumbers() {
        return YamlConfigLoader.getTestData().getValidation().getPhoneNumbers().getValid();
    }
}
