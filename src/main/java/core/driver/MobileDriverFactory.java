package core.driver;

import core.config.ConfigReader;
import core.config.YamlConfigLoader;
import core.config.model.FrameworkConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Factory for creating Appium mobile drivers for Android and iOS.
 *
 * <p>All capabilities are sourced from {@code configs/framework.yml}
 * under the {@code framework.appium} section, loaded via {@link YamlConfigLoader}.
 * Individual capability values can still be overridden at runtime via
 * {@code -Dkey=value} system properties or environment-specific {@code .properties} files.
 *
 * <p>Supports:
 * <ul>
 *   <li>Android via UIAutomator2</li>
 *   <li>iOS via XCUITest</li>
 *   <li>Real devices and emulators/simulators</li>
 * </ul>
 *
 * <pre>
 *   mvn test -Dplatform=android -Dtags=@mobile
 *   mvn test -Dplatform=ios    -Dtags=@mobile
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class MobileDriverFactory {

    private static final Logger log = LogManager.getLogger(MobileDriverFactory.class);

    private MobileDriverFactory() {}

    /**
     * Creates an Appium driver for the given mobile platform.
     *
     * @param driverType {@link DriverType#ANDROID} or {@link DriverType#IOS}
     * @return configured {@link AppiumDriver} cast to {@link WebDriver}
     */
    public static WebDriver createDriver(DriverType driverType) {
        return switch (driverType) {
            case ANDROID -> createAndroidDriver();
            case IOS     -> createIOSDriver();
            default -> throw new IllegalArgumentException(
                "Not a mobile driver type: " + driverType);
        };
    }

    // ─────────────────────────────────────────────────────────
    // Android Driver
    // ─────────────────────────────────────────────────────────

    /**
     * Creates an {@link AndroidDriver} with UIAutomator2 capabilities.
     * Capability values resolved from: system props → framework.yml appium.android section.
     */
    private static WebDriver createAndroidDriver() {
        log.info("Creating Android Driver...");

        // Load Android config from framework.yml
        FrameworkConfig.AppiumConfig.AndroidConfig androidCfg =
            YamlConfigLoader.getFramework().getFramework().getAppium().getAndroid();

        UiAutomator2Options options = new UiAutomator2Options();

        options.setPlatformName("Android");
        options.setPlatformVersion(
            ConfigReader.get("android.platform.version", androidCfg.getPlatform_version()));
        options.setDeviceName(
            ConfigReader.get("android.device.name", androidCfg.getDevice_name()));
        options.setAutomationName(
            ConfigReader.get("android.automation.name", androidCfg.getAutomation_name()));

        // UDID — only set if non-blank (blank = use emulator)
        String udid = ConfigReader.get("android.udid", androidCfg.getUdid());
        if (udid != null && !udid.isBlank()) {
            options.setUdid(udid);
            log.info("Android UDID: {}", udid);
        }

        // App — prefer app path (installs APK), fall back to package/activity
        String appPath = ConfigReader.get("android.app.path", androidCfg.getApp_path());
        if (appPath != null && !appPath.isBlank()) {
            options.setApp(appPath);
            log.info("Android app path: {}", appPath);
        } else {
            String pkg      = ConfigReader.get("android.app.package", androidCfg.getApp_package());
            String activity = ConfigReader.get("android.app.activity", androidCfg.getApp_activity());
            if (!pkg.isBlank()) options.setAppPackage(pkg);
            if (!activity.isBlank()) options.setAppActivity(activity);
            log.info("Android package={}, activity={}", pkg, activity);
        }

        options.setAutoGrantPermissions(
            Boolean.parseBoolean(ConfigReader.get("android.auto.grant.permissions",
                String.valueOf(androidCfg.isAuto_grant_permissions()))));
        options.setNoReset(
            Boolean.parseBoolean(ConfigReader.get("android.no.reset",
                String.valueOf(androidCfg.isNo_reset()))));
        options.setFullReset(
            Boolean.parseBoolean(ConfigReader.get("android.full.reset",
                String.valueOf(androidCfg.isFull_reset()))));

        long newCmdTimeout = Long.parseLong(ConfigReader.get("appium.new.command.timeout",
            String.valueOf(YamlConfigLoader.getFramework().getFramework().getAppium().getNew_command_timeout())));
        options.setNewCommandTimeout(Duration.ofSeconds(newCmdTimeout));

        AndroidDriver driver = new AndroidDriver(getAppiumServerUrl(), options);
        configureImplicitWait(driver);

        log.info("Android Driver created successfully.");
        return driver;
    }

    // ─────────────────────────────────────────────────────────
    // iOS Driver
    // ─────────────────────────────────────────────────────────

    /**
     * Creates an {@link IOSDriver} with XCUITest capabilities.
     * Capability values resolved from: system props → framework.yml appium.ios section.
     */
    private static WebDriver createIOSDriver() {
        log.info("Creating iOS Driver...");

        FrameworkConfig.AppiumConfig.IosConfig iosCfg =
            YamlConfigLoader.getFramework().getFramework().getAppium().getIos();

        XCUITestOptions options = new XCUITestOptions();

        options.setPlatformName("iOS");
        options.setPlatformVersion(
            ConfigReader.get("ios.platform.version", iosCfg.getPlatform_version()));
        options.setDeviceName(
            ConfigReader.get("ios.device.name", iosCfg.getDevice_name()));

        String udid = ConfigReader.get("ios.udid", iosCfg.getUdid());
        if (udid != null && !udid.isBlank()) {
            options.setUdid(udid);
            log.info("iOS UDID: {}", udid);
        }

        String appPath = ConfigReader.get("ios.app.path", iosCfg.getApp_path());
        if (appPath != null && !appPath.isBlank()) {
            options.setApp(appPath);
            log.info("iOS app path: {}", appPath);
        } else {
            String bundleId = ConfigReader.get("ios.bundle.id", iosCfg.getBundle_id());
            if (!bundleId.isBlank()) options.setBundleId(bundleId);
            log.info("iOS bundleId={}", bundleId);
        }

        options.setNoReset(
            Boolean.parseBoolean(ConfigReader.get("ios.no.reset",
                String.valueOf(iosCfg.isNo_reset()))));
        options.setAutoAcceptAlerts(
            Boolean.parseBoolean(ConfigReader.get("ios.auto.accept.alerts",
                String.valueOf(iosCfg.isAuto_accept_alerts()))));

        long newCmdTimeout = Long.parseLong(ConfigReader.get("appium.new.command.timeout",
            String.valueOf(YamlConfigLoader.getFramework().getFramework().getAppium().getNew_command_timeout())));
        options.setNewCommandTimeout(Duration.ofSeconds(newCmdTimeout));

        IOSDriver driver = new IOSDriver(getAppiumServerUrl(), options);
        configureImplicitWait(driver);

        log.info("iOS Driver created successfully.");
        return driver;
    }

    // ─────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────

    /**
     * Builds the Appium server URL.
     * Resolved from: system props → framework.yml appium.server section.
     */
    private static URL getAppiumServerUrl() {
        FrameworkConfig.AppiumConfig.AppiumServerConfig serverCfg =
            YamlConfigLoader.getFramework().getFramework().getAppium().getServer();

        String host = ConfigReader.get("appium.server.host", serverCfg.getHost());
        String port = ConfigReader.get("appium.server.port", String.valueOf(serverCfg.getPort()));
        String urlStr = "http://" + host + ":" + port;

        try {
            log.info("Appium server URL: {}", urlStr);
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium server URL: " + urlStr, e);
        }
    }

    /** Applies implicit wait from config to the Appium driver. */
    private static void configureImplicitWait(AppiumDriver driver) {
        int implicitWait = Integer.parseInt(ConfigReader.get("implicit.wait", "0"));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        log.debug("Mobile implicit wait: {}s", implicitWait);
    }
}
