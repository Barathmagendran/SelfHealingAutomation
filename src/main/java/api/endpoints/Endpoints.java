package api.endpoints;

/**
 * Centralized repository of all API endpoint paths.
 *
 * <p>Endpoint paths are relative to the {@code api.base.url} configured in config.properties.
 * Using constants prevents magic strings and simplifies maintenance when paths change.
 *
 * <p>Usage:
 * <pre>
 *   given(spec).when().post(Endpoints.AUTH_LOGIN);
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public final class Endpoints {

    private Endpoints() {}

    // ─────────────────────────────────────────────────────────
    // Authentication
    // ─────────────────────────────────────────────────────────

    public static final String AUTH_LOGIN   = "/auth/login";
    public static final String AUTH_LOGOUT  = "/auth/logout";
    public static final String AUTH_REFRESH = "/auth/refresh";

    // ─────────────────────────────────────────────────────────
    // Users
    // ─────────────────────────────────────────────────────────

    public static final String USERS         = "/users";
    public static final String USER_BY_ID    = "/users/{id}";
    public static final String USER_PROFILE  = "/users/profile";

    // ─────────────────────────────────────────────────────────
    // Products
    // ─────────────────────────────────────────────────────────

    public static final String PRODUCTS       = "/products";
    public static final String PRODUCT_BY_ID  = "/products/{id}";
    public static final String PRODUCT_SEARCH = "/products/search";

    // ─────────────────────────────────────────────────────────
    // Orders
    // ─────────────────────────────────────────────────────────

    public static final String ORDERS       = "/orders";
    public static final String ORDER_BY_ID  = "/orders/{id}";
    public static final String ORDER_STATUS = "/orders/{id}/status";
}
