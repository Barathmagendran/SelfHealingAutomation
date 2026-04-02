package core.driver;

/**
 * Enum representing supported driver/platform types.
 * Used to control which driver factory is invoked at runtime.
 *
 * <p>Usage: Configured via {@code config.properties} or Maven system property {@code -Dplatform=web}
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public enum DriverType {

    /** Selenium WebDriver for browser-based automation */
    WEB,

    /** Appium driver for Android mobile automation */
    ANDROID,

    /** Appium driver for iOS mobile automation */
    IOS,

    /** No driver required — API-only test execution */
    API;

    /**
     * Parses a string value (case-insensitive) into a {@link DriverType}.
     *
     * @param value the string representation (e.g., "web", "android", "ios", "api")
     * @return matching {@link DriverType}
     * @throws IllegalArgumentException if value does not match any known type
     */
    public static DriverType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DriverType value cannot be null or blank");
        }
        try {
            return DriverType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown driver type: '" + value + "'. Valid values: WEB, ANDROID, IOS, API", e
            );
        }
    }

    /** @return true if this type requires a WebDriver instance (web or mobile) */
    public boolean requiresDriver() {
        return this != API;
    }

    /** @return true if this is a mobile driver type */
    public boolean isMobile() {
        return this == ANDROID || this == IOS;
    }
}
