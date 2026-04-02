package stepdefinitions.web;


import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.openqa.selenium.io.FileHandler;

public class practice {
    private WebDriver driver;
    private WebDriverWait wait;
    private static int waitTime = 10;
    @FindBy(xpath = "//*[@role='button']")
    private WebElement closeBtn;
    @FindBy(xpath = "//*[contains(text(),'True Wireless')]")
    private WebElement trueWireLess;


    public practice(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitTime));
        PageFactory.initElements(driver, this);
    }


    public static WebDriver startDriver(String url) {
        WebDriver driver = new ChromeDriver();
        driver.get(url);
        driver.manage().window().maximize();
        return driver;
    }

    public void clickBtn(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        element.click();
    }

    public void takeScreenShot() throws IOException {
        File folder = new File(System.getProperty("user.dir") + "/test-output/screenshots/");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String filePath = folder + "/screenshot.png";
        TakesScreenshot tk = (TakesScreenshot) driver;
        File src = tk.getScreenshotAs(OutputType.FILE);
        File des = new File(filePath);
        FileHandler.copy(src, des);
        System.out.println("ScreeN Shot taken");
    }

    public static void main(String[] args) throws IOException {
        WebDriver driver = startDriver("https://www.flipkart.com/");
        practice practice = new practice(driver);
        practice.clickBtn(practice.closeBtn);
        practice.scrollToElement(practice.trueWireLess);
        practice.takeScreenShot();
        practice.clickBtn(practice.trueWireLess);
        driver.quit();
    }

    public void scrollToElement(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }


}