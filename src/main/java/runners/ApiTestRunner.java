package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Cucumber TestNG runner for API scenarios.
 *
 * <p>Run from Maven:
 * <pre>
 *   mvn test -Dplatform=api -Dtags=@api
 * </pre>
 *
 * @author Enterprise QA Team
 */
@CucumberOptions(
    features  = "src/test/java/features/api",
    glue      = {"stepdefinitions.api", "hooks"},
    tags      = "@api",
    plugin    = {
        "pretty",
        "html:target/reports/cucumber/api-report.html",
        "json:target/reports/cucumber/api-report.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    },
    monochrome = true
)
public class ApiTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
