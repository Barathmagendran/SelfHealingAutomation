package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Cucumber TestNG runner for the cross-platform Smoke test suite.
 *
 * <p>Runs all scenarios tagged with {@code @smoke} regardless of platform.
 * Tags for specific platform filtering are applied at runtime.
 *
 * <p>Run from Maven:
 * <pre>
 *   mvn test -Dtags="@smoke and @web"
 *   mvn test -Dtags="@smoke and @api"
 *   mvn test -Dtags="@smoke"     (runs all smoke tests)
 * </pre>
 *
 * @author Enterprise QA Team
 */
@CucumberOptions(
    features  = "src/test/java/features",
    glue      = {"stepdefinitions", "hooks"},
    tags      = "@smoke",
    plugin    = {
        "pretty",
        "html:target/reports/cucumber/smoke-report.html",
        "json:target/reports/cucumber/smoke-report.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    },
    monochrome = true
)
public class SmokeTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
