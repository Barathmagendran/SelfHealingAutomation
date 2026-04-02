# Enterprise Test Automation Framework

A scalable, production-ready BDD automation framework built with **Java + Cucumber + Maven** supporting **Web**, **Mobile**, and **API** test automation.

---

## Architecture Overview

```
enterprise-test-automation/
├── src/main/java/
│   ├── core/
│   │   ├── driver/          # DriverManager (ThreadLocal), WebDriverFactory, MobileDriverFactory
│   │   ├── config/          # ConfigReader (env-aware property resolution)
│   │   ├── utils/           # WaitUtils, JsonUtils, TestDataLoader, ScreenshotUtils
│   │   ├── base/            # BaseTest, BasePage, BaseAPI
│   │   ├── reporting/       # ExtentReportManager
│   │   ├── retry/           # RetryAnalyzer
│   │   └── listeners/       # SuiteListener, RetryListener (TestNG)
│   ├── web/
│   │   └── pages/           # LoginPage, DashboardPage (Page Object Model)
│   ├── mobile/
│   │   └── pages/           # MobileLoginPage (Appium)
│   ├── api/
│   │   ├── clients/         # AuthApiClient, UserApiClient
│   │   ├── models/          # Request/Response POJOs
│   │   └── endpoints/       # Endpoints constants
│   ├── stepdefinitions/
│   │   ├── web/             # LoginSteps
│   │   ├── mobile/          # MobileLoginSteps
│   │   └── api/             # UserApiSteps
│   ├── hooks/               # Cucumber lifecycle hooks
│   └── runners/             # WebTestRunner, ApiTestRunner, MobileTestRunner, SmokeTestRunner
│
├── src/test/java/
│   └── features/
│       ├── web/             # login.feature
│       ├── mobile/          # mobile_login.feature
│       └── api/             # user_api.feature
│
├── src/test/resources/
│   ├── testdata/
│   │   ├── json/            # login_data.json, new_user.json
│   │   ├── csv/             # users.csv
│   │   └── excel/           # (Excel test data files)
│   ├── configs/             # qa.properties, uat.properties
│   ├── schemas/             # user_schema.json (JSON schema validation)
│   ├── extent.properties    # Extent Reports adapter config
│   └── extent-config.xml    # Report theme/styling
│
├── src/main/resources/
│   ├── config.properties    # Base configuration
│   └── log4j2.xml           # Logging configuration
│
├── testng.xml               # TestNG suite definition
├── pom.xml                  # Maven build + all dependencies
└── .github/workflows/       # GitHub Actions CI/CD pipeline
```

---

## Quick Start

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 17+ | JAVA_HOME must be set |
| Maven | 3.8+ | mvn on PATH |
| Chrome | Latest | Auto-managed by WebDriverManager |
| Appium Server | 2.x | Only for mobile tests |
| Node.js | 18+ | Required to run Appium server |

### Clone & Build

```bash
git clone https://github.com/your-org/enterprise-test-automation.git
cd enterprise-test-automation
mvn clean compile -q
```

---

## Running Tests

### Web Tests

```bash
# Chrome (default)
mvn test -Dplatform=web -Dtags=@smoke

# Firefox headless
mvn test -Dplatform=web -Dbrowser=firefox -Dheadless=true -Dtags=@regression

# Specific environment
mvn test -Dplatform=web -Denv=uat -Dtags=@smoke
```

### API Tests

```bash
# All API smoke tests
mvn test -Dplatform=api -Dtags="@api and @smoke"

# Full API regression
mvn test -Dplatform=api -Dtags="@api and @regression"
```

### Mobile Tests

```bash
# Start Appium server first:
appium --address 127.0.0.1 --port 4723

# Android
mvn test -Dplatform=android -Dtags="@mobile and @android"

# iOS
mvn test -Dplatform=ios -Dtags="@mobile and @ios"
```

### Parallel Execution

```bash
# Run with 4 parallel threads
mvn test -P parallel -Dthreads=4 -Dtags=@regression
```

### Full Suite via Maven Profile

```bash
mvn test -P qa          # QA environment
mvn test -P uat         # UAT environment
mvn test -P headless    # Headless browser
```

---

## Configuration

All settings are in `src/main/resources/config.properties`.  
Environment-specific overrides in `src/test/resources/configs/{env}.properties`.  
Runtime overrides via `-D` flags take highest priority.

**Resolution order:**  
`-Dkey=value` → `configs/{env}.properties` → `config.properties` → hardcoded default

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `platform` | `web` | `web`, `android`, `ios`, `api` |
| `browser` | `chrome` | `chrome`, `firefox`, `edge`, `safari` |
| `headless` | `false` | Run browser without UI |
| `env` | `qa` | `qa`, `uat`, `prod` |
| `base.url` | _(set per env)_ | Application base URL |
| `api.base.url` | _(set per env)_ | API base URL |
| `explicit.wait` | `15` | WebDriverWait timeout (seconds) |
| `retry.count` | `1` | Failed test retry attempts |
| `screenshots.on.failure` | `true` | Auto-screenshot on failure |

---

## Writing New Tests

### 1. Add a Feature File

`src/test/java/features/web/my_feature.feature`

```gherkin
@web @smoke
Feature: My Feature

  Scenario: My scenario description
    Given the user is on the login page
    When the user logs in with email "user@example.com" and password "pass"
    Then the user should be redirected to the dashboard
```

### 2. Create a Page Object

```java
// src/main/java/web/pages/MyPage.java
public class MyPage extends BasePage {
    private static final By MY_ELEMENT = By.id("element-id");

    public String getElementText() {
        return getText(MY_ELEMENT);
    }
}
```

### 3. Add Step Definitions

```java
// src/main/java/stepdefinitions/web/MySteps.java
public class MySteps {
    @When("I perform my action")
    public void iPerformMyAction() {
        new MyPage().clickSomething();
    }
}
```

### 4. Add Test Data

```json
// src/test/resources/testdata/json/my_data.json
{
  "username": "testuser",
  "password": "testpass"
}
```

Load in steps:
```java
Map<String, String> data = TestDataLoader.loadJsonAsMap("json/my_data.json");
```

---

## Test Tagging Strategy

| Tag | Purpose |
|-----|---------|
| `@smoke` | Critical path, runs on every build |
| `@regression` | Full regression suite |
| `@web` | Web/browser tests only |
| `@api` | API tests only |
| `@mobile` | Mobile tests only |
| `@android` | Android-specific |
| `@ios` | iOS-specific |
| `@negative` | Negative/error path tests |
| `@data-driven` | Parameterized/data-driven tests |
| `@schema` | JSON schema validation tests |

---

## Reporting

After test execution, reports are generated in `target/reports/`:

- **Extent Report** → `target/reports/ExtentReport.html` (rich HTML, dark theme)
- **Cucumber HTML** → `target/reports/cucumber/{platform}-report.html`
- **Screenshots** → `target/screenshots/` (auto-captured on failure)
- **Logs** → `target/logs/automation-{timestamp}.log`

---

## CI/CD

The included GitHub Actions workflow (`.github/workflows/automation-ci.yml`) runs automatically on:
- Push to `main` / `develop`
- Pull requests to `main` / `develop`
- Nightly schedule (2 AM UTC)
- Manual dispatch (choose environment, platform, tags)

### Jenkins Pipeline (Jenkinsfile)

```groovy
pipeline {
    agent any
    parameters {
        choice(name: 'ENV', choices: ['qa', 'uat'], description: 'Target environment')
        string(name: 'TAGS', defaultValue: '@smoke', description: 'Cucumber tags')
    }
    stages {
        stage('API Tests') {
            steps {
                sh "mvn clean test -Dplatform=api -Denv=${params.ENV} -Dtags='${params.TAGS} and @api'"
            }
        }
        stage('Web Tests') {
            steps {
                sh "mvn clean test -Dplatform=web -Dbrowser=chrome -Dheadless=true -Denv=${params.ENV} -Dtags='${params.TAGS} and @web'"
            }
        }
    }
    post {
        always {
            publishHTML([
                reportDir: 'target/reports',
                reportFiles: 'ExtentReport.html',
                reportName: 'Automation Report'
            ])
        }
    }
}
```

---

## Framework Design Principles

- **SOLID principles** — single responsibility per class, open/closed via extension
- **Page Object Model (POM)** — UI interactions encapsulated in page classes
- **Thread Safety** — `ThreadLocal<WebDriver>` enables safe parallel execution
- **Layered configuration** — env properties → base config → system properties
- **Zero hardcoding** — all environment-specific values externalized
- **DRY utilities** — `WaitUtils`, `TestDataLoader`, `ScreenshotUtils` shared across all platforms
- **Fail-fast logging** — structured Log4j2 logs to both console and rolling file

---

## Project Dependencies Summary

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Selenium Java | 4.16.1 | Web browser automation |
| Appium Java Client | 9.1.0 | Mobile automation |
| RestAssured | 5.4.0 | API testing |
| Cucumber Java | 7.15.0 | BDD framework |
| TestNG | 7.9.0 | Test runner & parallel execution |
| ExtentReports | 5.1.1 | HTML test reports |
| WebDriverManager | 5.7.0 | Auto driver binary management |
| Log4j2 | 2.22.1 | Structured logging |
| Jackson | 2.16.1 | JSON parsing |
| Apache POI | 5.2.5 | Excel test data |
| OpenCSV | 5.9 | CSV test data |
| Lombok | 1.18.30 | Boilerplate reduction |
