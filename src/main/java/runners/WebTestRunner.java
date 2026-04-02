package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Main Cucumber TestNG runner for Web scenarios.
 *
 * <p>Executes all scenarios tagged with {@code @web}.
 * Supports parallel execution via TestNG DataProvider.
 *
 * <p>Run from Maven:
 * <pre>
 *   mvn test -Dplatform=web -Dtags=@smoke -Dbrowser=chrome
 *   mvn test -Dplatform=web -Dtags=@regression -Dheadless=true
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
@CucumberOptions(
    features  = "src/test/java/features/web",
    glue      = {"stepdefinitions.web", "hooks"},  // NavigationSteps is in stepdefinitions.web
    tags      = "@test1",
    plugin    = {
        "pretty",
        "html:target/reports/cucumber/web-report.html",
        "json:target/reports/cucumber/web-report.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    },
    monochrome = true,
    publish    = false
)
public class WebTestRunner extends AbstractTestNGCucumberTests {

    /**
     * Enables parallel scenario execution.
     * Thread count is controlled by {@code maven-surefire-plugin} configuration.
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
