package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public class Login {
    private static WebDriver driver;

    public static void run(){
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
        driver = new ChromeDriver(options);
        System.setProperty("webdriver.chrome.driver", "\\chromedriver.exe");
        driver.get("https://www.tiktok.com");
        // Maximize the browser window
        driver.manage().window().maximize();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(3));
        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".css-kmm27i-StyledMessageIcon.en5j2390")));
        element.click();


        JSONArray cookiesJsonArray = new JSONArray();
        for (Cookie cookie : driver.manage().getCookies()){
            JSONObject cookieJson = new JSONObject();
            cookieJson.put("name", cookie.getName());
            cookieJson.put("value", cookie.getValue());
            cookieJson.put("domain", cookie.getDomain());
            cookieJson.put("path", cookie.getPath());
            cookieJson.put("expires", cookie.getExpiry() != null ? cookie.getExpiry().toString() : null);
            cookieJson.put("sameSite", cookie.getSameSite());
            cookiesJsonArray.put(cookieJson);
        }

        // Save JSON to file
        File resourcesDir = new File("src/main/resources");
        File jsonFile = new File(resourcesDir, "cookies.json");
        if (jsonFile.exists()) {
            jsonFile.delete(); // Deletes the existing file
            System.out.println("Old cookies.json deleted.");
        }
        try (FileWriter fileWriter = new FileWriter(jsonFile)) {
            fileWriter.write(cookiesJsonArray.toString(4)); // Indented for readability
            System.out.println("Cookies saved to " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Close the driver
        driver.quit();
    }
}
