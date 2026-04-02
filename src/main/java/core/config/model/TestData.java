package core.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed model for {@code testData.yml}.
 *
 * <p>Provides domain-scoped, strongly-typed access to all test data.
 * Access via {@link core.config.YamlDataReader}:
 * <pre>
 *   UserData admin = YamlDataReader.getUser("adminUser");
 *   String email   = admin.getEmail();
 *
 *   ApiData.UserPayload p = YamlDataReader.getApiUserPayload("createValid");
 *   String role = p.getRole();
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
public class TestData {

    private Map<String, UserData>        users        = new HashMap<>();
    private Map<String, RegistrationData> registration = new HashMap<>();
    private Map<String, ProductData>     products     = new HashMap<>();
    private Map<String, OrderData>       orders       = new HashMap<>();
    private ApiData                      api          = new ApiData();
    private MobileData                   mobile       = new MobileData();
    private SearchData                   search       = new SearchData();
    private ValidationData               validation   = new ValidationData();

    // ─────────────────────────────────────────────────────────
    // User Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class UserData {
        private String  firstName = "";
        private String  lastName  = "";
        private String  email     = "";
        private String  password  = "";
        private String  role      = "";
        private String  phone     = "";
        private boolean isLocked  = false;

        /** Convenience: full name as "firstName lastName" */
        public String getFullName() {
            return (firstName + " " + lastName).trim();
        }
    }

    // ─────────────────────────────────────────────────────────
    // Registration Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class RegistrationData {
        private String  firstName       = "";
        private String  lastName        = "";
        private String  email           = "";
        private String  password        = "";
        private String  confirmPassword = "";
        private String  phone           = "";
        private String  country         = "";
        private boolean agreeToTerms    = false;
    }

    // ─────────────────────────────────────────────────────────
    // Product Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class ProductData {
        private int          id          = 0;
        private String       name        = "";
        private String       category    = "";
        private double       price       = 0.0;
        private String       currency    = "USD";
        private String       sku         = "";
        private boolean      inStock     = true;
        private int          quantity    = 0;
        private String       description = "";
        private List<String> tags        = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────
    // Order Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class OrderData {
        private int             productId       = 0;
        private int             quantity        = 1;
        private ShippingAddress shippingAddress = new ShippingAddress();
        private String          paymentMethod   = "";
        private String          promoCode       = "";

        @Data @NoArgsConstructor
        public static class ShippingAddress {
            private String street  = "";
            private String city    = "";
            private String state   = "";
            private String zipCode = "";
            private String country = "";
        }
    }

    // ─────────────────────────────────────────────────────────
    // API Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class ApiData {
        private AuthData                   auth             = new AuthData();
        private Map<String, HeadersData>   headers          = new HashMap<>();
        private Map<String, UserPayload>   userPayloads     = new HashMap<>();
        private ExpectedResponses          expectedResponses = new ExpectedResponses();

        @Data @NoArgsConstructor
        public static class AuthData {
            private Credentials validCredentials   = new Credentials();
            private Credentials invalidCredentials = new Credentials();

            @Data @NoArgsConstructor
            public static class Credentials {
                private String email    = "";
                private String password = "";
            }
        }

        @Data @NoArgsConstructor
        public static class HeadersData {
            private String Content_Type  = "application/json";
            private String Accept        = "application/json";
            private String Authorization = "";
        }

        @Data @NoArgsConstructor
        public static class UserPayload {
            private String firstName = "";
            private String lastName  = "";
            private String email     = "";
            private String role      = "";
            private String phone     = "";
        }

        @Data @NoArgsConstructor
        public static class ExpectedResponses {
            private int userListMinSize   = 1;
            private int createStatusCode  = 201;
            private int getStatusCode     = 200;
            private int deleteStatusCode  = 204;
            private int notFoundCode      = 404;
            private int conflictCode      = 409;
            private int unauthorizedCode  = 401;
            private int maxResponseTimeMs = 3000;
        }
    }

    // ─────────────────────────────────────────────────────────
    // Mobile Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class MobileData {
        private LoginScreenData             loginScreen   = new LoginScreenData();
        private OnboardingData              onboarding    = new OnboardingData();

        @Data @NoArgsConstructor
        public static class LoginScreenData {
            private MobileUser validUser   = new MobileUser();
            private MobileUser invalidUser = new MobileUser();
            private MobileUser lockedUser  = new MobileUser();

            @Data @NoArgsConstructor
            public static class MobileUser {
                private String username = "";
                private String password = "";
            }
        }

        @Data @NoArgsConstructor
        public static class OnboardingData {
            private NewMobileUser newUser = new NewMobileUser();

            @Data @NoArgsConstructor
            public static class NewMobileUser {
                private String name     = "";
                private String email    = "";
                private String password = "";
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // Search & Filter Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class SearchData {
        private List<String> validKeywords      = new ArrayList<>();
        private String emptyResultKeyword        = "";
        private FiltersData filters              = new FiltersData();

        @Data @NoArgsConstructor
        public static class FiltersData {
            private PriceRange priceRange  = new PriceRange();
            private String category        = "";
            private boolean inStockOnly    = false;
            private String sortBy          = "price_asc";

            @Data @NoArgsConstructor
            public static class PriceRange {
                private double min = 0;
                private double max = 9999;
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // Validation Data
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class ValidationData {
        private EmailValidation   email         = new EmailValidation();
        private PasswordData      password      = new PasswordData();
        private PhoneValidation   phoneNumbers  = new PhoneValidation();

        @Data @NoArgsConstructor
        public static class EmailValidation {
            private List<String> valid   = new ArrayList<>();
            private List<String> invalid = new ArrayList<>();
        }

        @Data @NoArgsConstructor
        public static class PasswordData {
            private String strong    = "";
            private String weak      = "";
            private String noUpper   = "";
            private String noNumber  = "";
            private String noSpecial = "";
            private String tooShort  = "";
        }

        @Data @NoArgsConstructor
        public static class PhoneValidation {
            private List<String> valid   = new ArrayList<>();
            private List<String> invalid = new ArrayList<>();
        }
    }
}
