package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Cucumber TestNG runner for Mobile scenarios (Android & iOS).
 *
 * <p>Run from Maven:
 * <pre>
 *   mvn test -Dplatform=android -Dtags=@mobile
 *   mvn test -Dplatform=ios -Dtags=@mobile
 * </pre>
 *
 * @author Enterprise QA Team
 */
@CucumberOptions(
    features  = "src/test/java/features/mobile",
    glue      = {"stepdefinitions.mobile", "hooks"},
    tags      = "@mobile",
    plugin    = {
        "pretty",
        "html:target/reports/cucumber/mobile-report.html",
        "json:target/reports/cucumber/mobile-report.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    },
    monochrome = true
)
public class MobileTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = false) // Mobile runs sequentially (one device at a time)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
