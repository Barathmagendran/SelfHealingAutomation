package core.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Typed model for {@code framework.yml}.
 *
 * <p>SnakeYAML maps the YAML tree directly into this object graph.
 * All fields mirror the YAML structure exactly (camelCase ↔ snake_case handled by SnakeYAML).
 *
 * <p>Access via:
 * <pre>
 *   YamlConfigLoader.getFramework().getTimeouts().getExplicit()
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
public class FrameworkConfig {

    private FrameworkSection framework = new FrameworkSection();

    @Data
    @NoArgsConstructor
    public static class FrameworkSection {
        private String environment    = "qa";
        private String platform       = "web";
        private UrlsConfig urls       = new UrlsConfig();
        private TimeoutsConfig timeouts = new TimeoutsConfig();
        private RetryConfig retry     = new RetryConfig();
        private ReportingConfig reporting = new ReportingConfig();
        private LoggingConfig logging = new LoggingConfig();
        private ParallelConfig parallel = new ParallelConfig();
        private TestDataConfig testdata = new TestDataConfig();
        private AppiumConfig appium   = new AppiumConfig();
        private ApiConfig api         = new ApiConfig();
    }

    @Data @NoArgsConstructor
    public static class UrlsConfig {
        private String web = "";
        private String api = "";
    }

    @Data @NoArgsConstructor
    public static class TimeoutsConfig {
        private int implicit    = 0;
        private int explicit    = 15;
        private int page_load   = 30;
        private int script      = 30;
        private int fluent_poll = 500;
    }

    @Data @NoArgsConstructor
    public static class RetryConfig {
        private int count    = 1;
        private int delay_ms = 1000;
    }

    @Data @NoArgsConstructor
    public static class ReportingConfig {
        private ExtentConfig extent = new ExtentConfig();
        private ScreenshotConfig screenshots = new ScreenshotConfig();

        @Data @NoArgsConstructor
        public static class ExtentConfig {
            private boolean enabled   = true;
            private String output     = "target/reports/ExtentReport.html";
            private String title      = "Enterprise QA Automation Report";
            private String name       = "Test Execution Report";
            private String theme      = "DARK";
            private String timestamp  = "MMM dd, yyyy HH:mm:ss";
        }

        @Data @NoArgsConstructor
        public static class ScreenshotConfig {
            private boolean on_failure = true;
            private String output_dir  = "target/screenshots";
            private String format      = "png";
        }
    }

    @Data @NoArgsConstructor
    public static class LoggingConfig {
        private String level      = "INFO";
        private String output_dir = "target/logs";
        private int max_files     = 10;
        private int max_size_mb   = 20;
    }

    @Data @NoArgsConstructor
    public static class ParallelConfig {
        private boolean enabled      = true;
        private int thread_count     = 2;
    }

    @Data @NoArgsConstructor
    public static class TestDataConfig {
        private String base_path  = "src/test/resources/testdata";
        private String yaml_path  = "src/test/resources/testdata/yaml";
        private String json_path  = "src/test/resources/testdata/json";
        private String csv_path   = "src/test/resources/testdata/csv";
        private String excel_path = "src/test/resources/testdata/excel";
    }

    @Data @NoArgsConstructor
    public static class AppiumConfig {
        private AppiumServerConfig server   = new AppiumServerConfig();
        private int new_command_timeout     = 60;
        private AndroidConfig android       = new AndroidConfig();
        private IosConfig ios               = new IosConfig();

        @Data @NoArgsConstructor
        public static class AppiumServerConfig {
            private String host = "127.0.0.1";
            private int port    = 4723;
        }

        @Data @NoArgsConstructor
        public static class AndroidConfig {
            private String platform_version         = "13";
            private String device_name              = "Android Emulator";
            private String automation_name          = "UIAutomator2";
            private String app_package              = "";
            private String app_activity             = "";
            private String udid                     = "";
            private String app_path                 = "";
            private boolean auto_grant_permissions  = true;
            private boolean no_reset                = false;
            private boolean full_reset              = false;
        }

        @Data @NoArgsConstructor
        public static class IosConfig {
            private String platform_version         = "17.0";
            private String device_name              = "iPhone 15";
            private String bundle_id                = "";
            private String udid                     = "";
            private String app_path                 = "";
            private boolean no_reset                = false;
            private boolean auto_accept_alerts      = true;
        }
    }

    @Data @NoArgsConstructor
    public static class ApiConfig {
        private String content_type        = "application/json";
        private String auth_type           = "bearer";
        private int connection_timeout     = 10000;
        private int read_timeout           = 30000;
        private boolean log_request        = true;
        private boolean log_response       = true;
    }
}
