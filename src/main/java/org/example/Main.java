package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Main {
    private static WebDriver driver;

    private static String name = "admin_break";

    private static WebDriver newDriver(){
        System.setProperty("webdriver.chrome.driver", "D:\\sdk\\chromedriver-win64\\chromedriver.exe");
        // Create ChromeOptions object to configure the browser
        ChromeOptions options = new ChromeOptions();

        // Disable the "AutomationControlled" flag
        options.addArguments("--disable-blink-features=AutomationControlled");

        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Exclude the "enable-automation" switch
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Turn off the user automation extension
        options.setExperimentalOption("useAutomationExtension", false);

        options.addArguments("--disable-notifications");
        driver = new ChromeDriver(options);
        ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

        return driver;
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        File newDir = new File("D:\\workspace\\work\\vids\\" + name);
        if (!newDir.exists()) {
            newDir.mkdirs();  // Creates the directory if it doesn't exist
        }
        File txtFile = new File(newDir, name + ".txt");
        if (!txtFile.exists()) {
            txtFile.createNewFile();  // Creates the file
        }
        FileWriter writer = new FileWriter(txtFile, true);

        driver = newDriver();
        driver.get("https://www.tiktok.com/@"+name);
        Thread.sleep(2000);
        System.out.println("here");
        // driver.findElement(By.xpath("//div[@class='TUXSegmentedControl-itemTitle' and text()='Popular']")).click();
        int count = 0;

        List<WebElement> links = driver.findElements(By.xpath("//div[contains(@class, 'css-13fa1gi-DivWrapper') and contains(@class, 'e1cg0wnj1')]//a"));
        if(links.isEmpty()){
            System.out.println("can't find");
        }
        for (WebElement link : links) {
            String href = link.getAttribute("href");
            System.out.println(href);
            driver = newDriver();
            driver.get(href);
            Thread.sleep(2000);

            List<WebElement> elements = driver.findElements(By.xpath("//*[contains(@class, 'css-vc3yj-StrongText')]"));


            WebElement videoElement = driver.findElement(By.tagName("video"));
            List<WebElement> sourceElements = videoElement.findElements(By.tagName("source"));
            if(sourceElements.isEmpty()){
                System.out.println("this video get some error: " + count + ": "+ href);
            } else{
                String fileName = "vid_" + count+".mp4";
                writer.write(fileName + "\n");
                writer.write("\t");
                writer.write(driver.findElement(By.xpath("//meta[@name='description']")).getAttribute("content"));
                writer.write("\n");
                writer.flush();
                String videoUrl = sourceElements.get(2).getAttribute("src");
                System.out.println(videoUrl);
                downloadVideo(videoUrl, fileName, newDir);
                Thread.sleep(2000);
                driver.quit();
            }
            count++;
        }
        driver.quit();
    }
    // Method to download video using HttpURLConnection
    public static void downloadVideo(String videoUrl, String fileName, File directory) throws IOException {

        String filePath = directory.getAbsolutePath() + File.separator + fileName;

        // Open the URL connection
        URL url = new URL(videoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Referer", "https://www.tiktok.com/");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Get input stream for the video content
        long bufferSize = 1024 * 1024; // 1 MB buffer size
        long position = 0;

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

            // Create a ReadableByteChannel from the InputStream
            ReadableByteChannel readableByteChannel = Channels.newChannel(in);

            // Loop to transfer data in chunks until the entire file is downloaded
            while (true) {
                // Transfer up to 'bufferSize' bytes from the channel to the output stream
                long transferred = fileOutputStream.getChannel().transferFrom(readableByteChannel, position, bufferSize);

                // Break the loop if no bytes were transferred (end of file reached)
                if (transferred <= 0) break;

                // Move the position forward by the number of bytes transferred
                position += transferred;
            }

            System.out.println("Video downloaded successfully as " + filePath);

        } catch (IOException e) {
            System.err.println("Error downloading video: " + e.getMessage());
            e.printStackTrace();
        }
    }
}