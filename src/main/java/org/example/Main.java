package org.example;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Main {
    private static WebDriver classDriver;
    private static final int NUMBER_OF_THREADS = 5;
    private String name;
    private String saveLocation;

    public Main(String name, String saveLocation) {
        this.name = name;
        this.saveLocation = saveLocation;
    }


    private static WebDriver newDriver(Boolean isHeadlessMode) throws URISyntaxException {
        // Load chromedriver from the resources directory
        String driverPath = new File(Main.class.getClassLoader().getResource("chromedriver.exe").toURI()).getPath();

// Set the system property for the ChromeDriver
        System.setProperty("webdriver.chrome.driver", driverPath);
        // Create ChromeOptions object to configure the browser
        ChromeOptions options = new ChromeOptions();
        if(isHeadlessMode){
            options.addArguments("--headless");
        }
        // Disable the "AutomationControlled" flag
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        //options.addArguments("--disable-blink-features=AutomationControlled");
        //options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Turn off the user automation extension
        options.setExperimentalOption("useAutomationExtension", false);

        options.addArguments("--disable-notifications");
        WebDriver webDriver = new ChromeDriver(options);

        return webDriver;
    }
    public void main(List<WebElement> links) throws IOException, InterruptedException, URISyntaxException {
        File newDir = new File(saveLocation + "\\" + name);
        if (!newDir.exists()) {
            newDir.mkdirs();  // Creates the directory if it doesn't exist
        }
        File errorFile = new File(newDir,  "error.txt");
        FileWriter error = new FileWriter(errorFile);
        File downloadedFile = new File(newDir,  "downloaded.txt");
        FileWriter downloaded = new FileWriter(downloadedFile);
        // driver.findElement(By.xpath("//div[@class='TUXSegmentedControl-itemTitle' and text()='Popular']")).click();
        if(classDriver == null || links == null){
            classDriver = newDriver(true);
            classDriver.get("https://www.tiktok.com/@" + name);
            links = getTotalVideos();
        }
        int total = links.size();
        List<WebElement> viewList = classDriver.findElements(By.cssSelector("strong[data-e2e]"));
        List<VideoInfo> info = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

        for (int i = 0; i < total; i++) {
            String href = links.get(i).getAttribute("href");
            String view = viewList.get(i).getText();
            System.out.println(view);
            int index = i+1; // Capture the current index
            System.out.println(href);
            executor.submit(() -> {
                try {
                    processDownloadVideo(info, href, index, newDir, view, error, downloaded);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        // Shutdown the executor after submitting all tasks
        executor.shutdown();

        // Wait for all tasks to finish
        try {
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                System.out.println("Tasks did not finish in time");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Collections.sort(info, Comparator.comparing(VideoInfo :: getFilename));
        exportExcel(info);
        System.out.println("done");
        classDriver.quit();
    }
    public static void openAndSolveCaptcha(String username) throws URISyntaxException {
        classDriver = newDriver(false);
        classDriver.get("https://www.tiktok.com/@" + username);
        classDriver.navigate().refresh();
        classDriver.navigate().refresh();
        classDriver.navigate().refresh();
        // solve the captcha by rice :)
    }
    public static List<WebElement> getTotalVideos(){
        try {
            long lastHeight = (long) ((JavascriptExecutor) classDriver).executeScript("return document.body.scrollHeight");
            while (true) {
                // Scroll down
                ((JavascriptExecutor) classDriver).executeScript("window.scrollBy(0, document.body.scrollHeight);");

                // Wait for content to load
                Thread.sleep(1000); // Adjust delay if needed

                // Get the new height
                long newHeight = (long) ((JavascriptExecutor) classDriver).executeScript("return document.body.scrollHeight");
                System.out.println("New Height: " + newHeight);

                // Break the loop if no new content is loaded
                if (newHeight == lastHeight) {
                    System.out.println("Reached the bottom of the page.");
                    break;
                }
                lastHeight = newHeight;
            }
        } catch (InterruptedException e) {
            // Handle the exception
            Thread.currentThread().interrupt(); // Restore interrupt status
            e.printStackTrace();
        }
        List<WebElement> links = classDriver.findElements(By.xpath("//div[contains(@class, 'css-13fa1gi-DivWrapper') and contains(@class, 'e1cg0wnj1')]//a"));
        if(links.isEmpty()){
            System.out.println("can't find");
        }
        return links;
    }

    private void processDownloadVideo(List<VideoInfo> info, String href, int count, File newDir, String view, FileWriter error, FileWriter dowloaded) throws InterruptedException, IOException {
        if (href.isBlank()) {
            return;
        }
        final int MAX_RETRIES = 5;
        WebDriver webDriver = null;
        String fileName = "vid_" + count + ".mp4";

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                webDriver = newDriver(true);
                webDriver.get(href);
                WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
                WebElement videoElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("video")));
                List<WebElement> sourceElements = videoElement.findElements(By.tagName("source"));
                // if sourceElements is empty then the video url is in src attribute of video tag and required cookies to download
                String videoUrl = sourceElements.isEmpty() ? videoElement.getAttribute("src") :
                                        sourceElements.get(sourceElements.size()-1).getAttribute("src");
                if (videoUrl != null && !videoUrl.isBlank()) {
                    String cookies = sourceElements.isEmpty() ? getCookiesString(webDriver) : "";
                    VideoInfo videoInfo = createInfo(webDriver, fileName, href, view);
                    info.add(videoInfo);
                    logSuccess(videoInfo, dowloaded);
                    downloadVideo(videoUrl, fileName, newDir, cookies);
                    System.out.println("Attempt " + (i + 1) + " success!");
                    break; // Exit the loop on success
                }
                else {
                    System.err.println("No video found. Attempt " + (i + 1));
                }

            } catch (Exception e) {
                System.err.println("Error on attempt " + (i + 1) + ": " + e.getMessage());
            } finally {
                if (webDriver != null) {
                    webDriver.quit();
                }
            }

            if (i == MAX_RETRIES - 1) {
                logError(fileName, error, href);
            }
        }
    }
    private String getCookiesString(WebDriver webDriver){
        StringBuilder cookiesString = new StringBuilder();
        for (Cookie cookie : webDriver.manage().getCookies()) {
            cookiesString.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
        }

        if (cookiesString.length() > 0) {
            cookiesString.setLength(cookiesString.length() - 2);
        }
        return cookiesString.toString();
    }
    private void logSuccess(VideoInfo videoInfo, FileWriter writer) throws IOException {
        writer.write(videoInfo.toString() + "\n");
        writer.flush();
    }

    private void logError(String fileName, FileWriter writer, String href) throws IOException {
        System.out.println("this video get some error: " + href);
        writer.write(fileName + ": "+ href + "\n");
        writer.flush();
    }
    private VideoInfo createInfo(WebDriver driver, String fileName, String link, String view){
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
        WebElement musicElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".css-pvx3oa-DivMusicText.epjbyn3")));
        String music = musicElement.getText();
        String hashtags = "";
        List<WebElement> hashtagElement = driver.findElements(By.cssSelector("a.css-sbcvet-StyledCommonLink strong"));
        if(hashtagElement.isEmpty()){
            hashtagElement = driver.findElements(By.cssSelector("a.css-sbcvet-StyledCommonLink strong"));
        }
        for(WebElement e : hashtagElement){
            hashtags += e.getText() + " ";
        }
        List<WebElement> titlesElement = null;
        String title = "";
        try{
            titlesElement = driver.findElements(By.cssSelector("span[data-e2e='new-desc-span']"));
            title = titlesElement.get(0).getText();
        }catch (Exception e){

        }
        String like;
        String comment;
        try {
            like = driver.findElement(By.cssSelector("strong[data-e2e='browse-like-count']")).getText();
            comment = driver.findElement(By.cssSelector("strong[data-e2e='browse-comment-count']")).getText();
        } catch (NoSuchElementException e) {
            try{
                like = driver.findElement(By.cssSelector("strong[data-e2e='like-count']")).getText();
                comment = driver.findElement(By.cssSelector("strong[data-e2e='comment-count']")).getText();
            }catch (NoSuchElementException ex){
                like = "";
                comment = "";
            };
        }
        return new VideoInfo(fileName, link, view, like, comment, title, hashtags, music);
    }


    private void exportExcel(List<VideoInfo> info) throws IOException {
        File file = new File(saveLocation + "\\" + name + "\\info.xlsx");
        Workbook workbook;
        if (file.exists()) {
            // Load existing workbook
            try (FileInputStream fis = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fis);
            }
        } else {
            // Create new workbook and sheet if file doesn't exist
            workbook = new XSSFWorkbook();
            workbook.createSheet("Info");
        }

        // Access the first sheet (or create a new one if it's missing)
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            sheet = workbook.createSheet("Info");
        }

        if (sheet.getPhysicalNumberOfRows() == 0) {
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("File Name");
            headerRow.createCell(1).setCellValue("Link");
            headerRow.createCell(2).setCellValue("Views");
            headerRow.createCell(3).setCellValue("Likes");
            headerRow.createCell(4).setCellValue("Comments");
            headerRow.createCell(5).setCellValue("Title");
            headerRow.createCell(6).setCellValue("Hashtags");
            headerRow.createCell(7).setCellValue("Music");
        }

        // Write data rows to the sheet
        for (int i = 0; i < info.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(info.get(i).getFilename());
            row.createCell(1).setCellValue(info.get(i).getLink());
            row.createCell(2).setCellValue(info.get(i).getView());
            row.createCell(3).setCellValue(info.get(i).getLike());
            row.createCell(4).setCellValue(info.get(i).getComment());
            row.createCell(5).setCellValue(info.get(i).getTitle());
            row.createCell(6).setCellValue(info.get(i).getHashtags());
            row.createCell(7).setCellValue(info.get(i).getMusic());
        }

        // Write the changes back to the file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        } finally {
            workbook.close();
        }
    }

    // Method to download video using HttpURLConnection
    public void downloadVideo(String videoUrl, String fileName, File directory, String cookies) throws IOException, InterruptedException {

        String filePath = directory.getAbsolutePath() + File.separator + fileName;

        URL url = new URL(videoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Referer", "https://www.tiktok.com/");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        if(!cookies.isBlank())
            connection.setRequestProperty("Cookie", cookies);


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
    /*public String getJsonString(String filename)throws IOException, InterruptedException {
        // Load cookies from JSON file located in resources folder
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(filename);

        if (inputStream == null) {
            System.out.println("file not found");
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();

        String jsonContent = stringBuilder.toString();
        return jsonContent;
    }
    public void addCookiesToDriver(WebDriver driver) throws IOException, InterruptedException {
        JSONArray cookiesArray = new JSONArray(getJsonString("cookies.json"));

        // Add cookies to the WebDriver
        for (int i = 0; i < cookiesArray.length(); i++) {
            JSONObject cookieJson = cookiesArray.getJSONObject(i);
            String name = cookieJson.getString("name");
            String value = cookieJson.getString("value");

            // Create a Selenium Cookie object
            Cookie cookie = new Cookie(name, value);
            driver.manage().addCookie(cookie);
        }
    }*/
}