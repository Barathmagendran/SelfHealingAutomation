package core.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed model for {@code browser.yml}.
 *
 * <p>Provides strongly-typed access to all browser capability settings.
 * Access via:
 * <pre>
 *   BrowserConfig cfg = YamlConfigLoader.getBrowser();
 *   List&lt;String&gt; args = cfg.getBrowser().getChrome().getArguments();
 *   boolean headless   = cfg.getBrowser().getDefaults().isHeadless();
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
public class BrowserConfig {

    private BrowserSection browser = new BrowserSection();

    @Data @NoArgsConstructor
    public static class BrowserSection {
        private String active                   = "chrome";
        private DefaultsConfig defaults         = new DefaultsConfig();
        private ChromeConfig chrome             = new ChromeConfig();
        private FirefoxConfig firefox           = new FirefoxConfig();
        private EdgeConfig edge                 = new EdgeConfig();
        private SafariConfig safari             = new SafariConfig();
        private RemoteConfig remote             = new RemoteConfig();
        private WebDriverManagerConfig webdriver_manager = new WebDriverManagerConfig();
    }

    // ─────────────────────────────────────────────────────────
    // Global defaults
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class DefaultsConfig {
        private boolean headless                  = false;
        private String window_size                = "1920x1080";
        private String page_load_strategy         = "normal";
        private boolean accept_insecure_certs     = true;
    }

    // ─────────────────────────────────────────────────────────
    // Chrome
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class ChromeConfig {
        private boolean headless                  = false;
        private String window_size                = "1920x1080";
        private List<String> arguments            = new ArrayList<>();
        private List<String> headless_arguments   = new ArrayList<>();
        private Map<String, Object> preferences   = new HashMap<>();
        private Map<String, Object> experimental_options = new HashMap<>();
        private Map<String, Object> capabilities  = new HashMap<>();
    }

    // ─────────────────────────────────────────────────────────
    // Firefox
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class FirefoxConfig {
        private boolean headless                  = false;
        private String window_size                = "1920x1080";
        private List<String> arguments            = new ArrayList<>();
        private List<String> headless_arguments   = new ArrayList<>();
        private Map<String, Object> preferences   = new HashMap<>();
        private Map<String, Object> capabilities  = new HashMap<>();
    }

    // ─────────────────────────────────────────────────────────
    // Edge
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class EdgeConfig {
        private boolean headless                  = false;
        private String window_size                = "1920x1080";
        private List<String> arguments            = new ArrayList<>();
        private List<String> headless_arguments   = new ArrayList<>();
        private Map<String, Object> capabilities  = new HashMap<>();
    }

    // ─────────────────────────────────────────────────────────
    // Safari
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class SafariConfig {
        private boolean headless                  = false; // always false for Safari
        private String window_size                = "1920x1080";
        private Map<String, Object> capabilities  = new HashMap<>();
        private String notes                      = "";
    }

    // ─────────────────────────────────────────────────────────
    // Selenium Grid / Remote
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class RemoteConfig {
        private boolean enabled                   = false;
        private String hub_url                    = "http://localhost:4444/wd/hub";
        private Map<String, Object> capabilities  = new HashMap<>();
    }

    // ─────────────────────────────────────────────────────────
    // WebDriverManager
    // ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class WebDriverManagerConfig {
        private String chrome_driver_version  = "";
        private String firefox_driver_version = "";
        private String edge_driver_version    = "";
        private String cache_path             = "~/.cache/selenium";
        private String proxy                  = "";
    }
}
