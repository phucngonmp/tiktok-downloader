package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v137.fetch.model.RequestId;
import org.openqa.selenium.devtools.v139.network.Network;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class Main {

    private static final String XPATH_GET_LINK_TO_POST_1 = "//*[contains(@id, 'column-item-video-container')]//a";
    private static final String XPATH_GET_LINK_TO_POST_2 = "//div[contains(@id, 'grid-item-container')]//a";

    private static String selectedXPath = null;
    private static final String CSS_GET_VIEW_COUNT = "strong[data-e2e='video-views']";
    private static final String XPATH_LATEST_BUTTON = "//button[@aria-label='Latest']";
    private static final String XPATH_POPULAR_BUTTON = "//button[@aria-label='Popular']";
    private static final String XPATH_OLDEST_BUTTON = "//button[@aria-label='Oldest']";


    private WebDriver classDriver;
    private int threads = 5;
    private String name;
    private String rootLocation;
    private File saveDirectory;
    private List<WebElement> links = null;
    private List<WebElement> viewList = null;

    public Main() {

    }

    public WebDriver createAndGetDriver(Boolean isHeadlessMode) {
        classDriver = newDriver(isHeadlessMode);
        return classDriver;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public List<WebElement> getViewList() {
        return this.viewList;
    }

    public void setRootLocation(String rootLocation) {
        this.rootLocation = rootLocation;
    }

    public void setIsHeadlessMode(Boolean isHeadlessMode) {
        this.classDriver = newDriver(isHeadlessMode);
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void reset() {
        name = null;
        if (classDriver != null) {
            classDriver.quit();
            classDriver = null;
        }
    }

    private WebDriver newDriver(Boolean isHeadlessMode) {
        WebDriverManager.chromedriver().setup();
        // Create ChromeOptions object to configure the browser
        ChromeOptions options = new ChromeOptions();
        if (isHeadlessMode) {
            options.addArguments("--headless=new");
        }
        // Disable the "AutomationControlled" flag
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--mute-audio");
        //options.addArguments("--disable-blink-features=AutomationControlled");
        //options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Turn off the user automation extension
        options.setExperimentalOption("useAutomationExtension", false);

        options.addArguments("--disable-notifications");
        return new ChromeDriver(options);
    }

    public void main(double downloadOption, JProgressBar progressBar) throws IOException, InterruptedException {
        saveDirectory = new File(rootLocation + "\\" + name);
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();  // Creates the directory if it doesn't exist
        }
        int total = getTotalDownloadVideos(downloadOption);

        System.out.println("total videos: " + total);
        List<VideoInfo> info = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger done = new AtomicInteger(0);
        for (int i = 0; i < total; i++) {
            String postUrl = links.get(i).getAttribute("href");
            System.out.println(postUrl);
            String view = viewList.get(i).getText();
            System.out.println(view);
            int index = i + 1;
            executor.submit(() -> {
                try {
                    processDownloadVideos(info, postUrl, index, view);
                    int completed = done.incrementAndGet();
                    progressBar.setValue((int) (completed * 100 / total));
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Shutdown the executor after all tasks are submitted
        executor.shutdown();


        // Wait for all tasks to finish
        try {
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                System.out.println("Tasks did not finish in time");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // sort by views
        info.sort(Comparator.comparing((VideoInfo v) -> extractNumber(v.getView())).reversed());
        exportExcel(info);
        System.out.println("done");
        classDriver.quit();
        classDriver = null;
    }

    private int extractNumber(String view) {
        try {
            if (view.endsWith("K")) {
                return (int) (Double.parseDouble(view.substring(0, view.length() - 1)) * 1000);
            } else if (view.endsWith("M")) {
                return (int) (Double.parseDouble(view.substring(0, view.length() - 1)) * 1000000);
            } else {
                return Integer.parseInt(view);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing view at : " + view);
            return 0;
        }
    }

    public int getTotalDownloadVideos(double downloadOption) {
        double total = links.size();
        if (downloadOption <= 1) {
            total = Math.ceil(total * downloadOption);
        } else if (downloadOption < total) {
            total = downloadOption;
        }
        return (int) total;
    }

    // first scan mean make webdriver find sort by popular or latest or oldest and do one scan
    public void firstScan(String sortOption) throws InterruptedException {
        if (classDriver == null) {
            classDriver = newDriver(true);
        }
        classDriver.get("https://www.tiktok.com/@" + name);
        loadCookiesFromFile(classDriver);
        classDriver.navigate().refresh();
        WebDriverWait wait = new WebDriverWait(classDriver, Duration.ofSeconds(10));
        if (sortOption.equals("popular")) {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(XPATH_POPULAR_BUTTON))).click();
            System.out.println("clicked popular");
        } else if (sortOption.equals("latest")) {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(XPATH_LATEST_BUTTON))).click();
            System.out.println("clicked latest");
        } else {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(XPATH_OLDEST_BUTTON))).click();
        }
        Thread.sleep(3000);
        links = classDriver.findElements(By.xpath(XPATH_GET_LINK_TO_POST_1));
        selectedXPath = XPATH_GET_LINK_TO_POST_1;
        if (links.isEmpty()) {
            links = classDriver.findElements(By.xpath(XPATH_GET_LINK_TO_POST_2));
            selectedXPath = XPATH_GET_LINK_TO_POST_2;
        }
        viewList = classDriver.findElements(By.cssSelector(CSS_GET_VIEW_COUNT));
    }

    public void scanVideo(JLabel totalVideoLabel) {
        if (classDriver == null) {
            return;
        }
        try {
            links = classDriver.findElements(By.xpath(selectedXPath));
            viewList = classDriver.findElements(By.cssSelector("strong[data-e2e='video-views']"));
            long lastHeight = (long) ((JavascriptExecutor) classDriver).executeScript("return document.body.scrollHeight");
            int waitCount = 0;
            while (UserPanel.isScanning) {
                ((JavascriptExecutor) classDriver).executeScript("window.scrollBy(0, document.body.scrollHeight);");

                // Wait for content to load
                Thread.sleep(1500); // Adjust delay if needed
                links = classDriver.findElements(By.xpath(selectedXPath));
                viewList = classDriver.findElements(By.cssSelector("strong[data-e2e='video-views']"));
                // Get the new height
                long newHeight = (long) ((JavascriptExecutor) classDriver).executeScript("return document.body.scrollHeight");
                System.out.println("New Height: " + newHeight);

                // Break the loop if no new content is loaded
                if (newHeight == lastHeight) {
                    waitCount++;
                }
                if (waitCount == 3) {
                    System.out.println("Reached the bottom of the page.");
                    break;
                }
                lastHeight = newHeight;
                totalVideoLabel.setText("Total Videos: " + links.size());
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            e.printStackTrace();
        }
        if (links.isEmpty()) {
            System.out.println("can't find videos");
        }
    }

    private boolean isPostUrlPhotoType(String postUrl) {
        return postUrl.split("/")[4].equals("photo");
    }

    public void processDownloadPhoto(WebDriver webDriver, File photoDirectory) throws IOException {
        try {
            List<WebElement> imageElements = webDriver.findElements(By.xpath("//div[contains(@class, 'swiper-slide')]//img"));
            List<String> imageUrls = imageElements.stream().map(element -> element.getAttribute("src")).toList();
            // download images
            for (int i = 0; i < imageUrls.size() / 3; i++) {
                downloadImage(imageUrls.get(i), new File(photoDirectory, "photo_" + i + ".jpg"));
                System.out.println("downloaded image " + imageUrls.get(i));
            }
            // download audio
            String audioUrl = webDriver.findElement(By.xpath("//audio")).getAttribute("src");
            downloadVideo(audioUrl, "audio.mp3", photoDirectory, "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void processDownloadVideos(List<VideoInfo> info, String postUrl, int count, String view) throws InterruptedException, IOException {
        // files contain log
        File errorFile = new File(saveDirectory, "error.txt");
        FileWriter error = new FileWriter(errorFile, true);
        File downloadedFile = new File(saveDirectory, "downloaded.txt");
        FileWriter downloaded = new FileWriter(downloadedFile, true);
        if (postUrl.isBlank()) {
            return;
        }
        WebDriver webDriver = null;
        String fileName = "vid_" + count + ".mp4";
        try {
            webDriver = newDriver(true);
            VideoInfo videoInfo;
            if (isPostUrlPhotoType(postUrl)) {
                webDriver.get(postUrl);
                fileName = "photo_" + count;
                videoInfo = createInfo(webDriver, fileName, view);
                try{
                    processDownloadPhoto(webDriver, new File(saveDirectory, fileName));
                } catch (Exception e){
                    logError(fileName, error, postUrl);
                }
            } else {
                String videoUrl = getVideoUrl(webDriver, postUrl);
                videoInfo = createInfo(webDriver, fileName, view);
                String cookies = getCookiesString(webDriver);
                try {
                    downloadVideo(videoUrl, fileName, saveDirectory, cookies);
                } catch (Exception e) {
                    logError(fileName, error, postUrl);
                }
            }
            info.add(videoInfo);
            logSuccess(videoInfo, downloaded);
        } finally {
            downloaded.close();
            error.close();
            if (webDriver != null) {
                webDriver.quit();
            }
        }

    }

    public String getVideoUrl(WebDriver webDriver, String postUrl) {
        DevTools devTools = ((HasDevTools) webDriver).getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.send(Network.setBlockedURLs(
                Collections.singletonList("https://www.tiktok.com/api/*")
        ));
        CompletableFuture<String> videoUrlFuture = new CompletableFuture<>();
        devTools.addListener(Network.requestWillBeSent(), rr -> {
            String url = rr.getRequest().getUrl();

            if (url.startsWith("https://v16-webapp-prime.tiktok.com/") ||
                    url.startsWith("https://v19-webapp-prime.tiktok.com/")) {
                videoUrlFuture.complete(url);
            }
        });

        webDriver.get(postUrl);
        try {
            return videoUrlFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("Video URL not found within timeout.");
            return null;
        } finally {
            devTools.clearListeners();
            devTools.send(Network.disable());
        }
    }

    public String getCookiesString(WebDriver webDriver) {
        StringBuilder cookiesString = new StringBuilder();
        for (Cookie cookie : webDriver.manage().getCookies()) {
            cookiesString.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
        }

        if (!cookiesString.isEmpty()) {
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
        writer.write(fileName + ": " + href + "\n");
        writer.flush();
    }

    private String tryGetText(WebDriver driver, String xpath1, String xpath2) {
        try {
            String text = driver.findElement(By.xpath(xpath1)).getText().trim();
            if (!text.isEmpty()) return text;
        } catch (Exception ignored) {}

        try {
            String text = driver.findElement(By.xpath(xpath2)).getText().trim();
            if (!text.isEmpty()) return text;
        } catch (Exception ignored) {}
        return "";
    }

    private VideoInfo createInfo(WebDriver driver, String fileName, String view) {
        String music = tryGetText(
                driver,
                "//p[contains(@class, 'StyledMusicText ')]",
                "//h4[@data-e2e='browse-music']//a//div[contains(@class, 'DivMusicText')]");
        String title = tryGetText(
                driver,
                "//span[@data-e2e='desc-span-0_-1']",
                "//span[@data-e2e='new-desc-span']");
        String like = tryGetText(
                driver,
                "//strong[@data-e2e='like-count']",
                "//strong[@data-e2e='browse-like-count']");
        String comment = tryGetText(
                driver,
                "//strong[@data-e2e='comment-count']",
                "//strong[@data-e2e='browse-comment-count']");
        String link = driver.getCurrentUrl();
        String hashtags = "";
        List<WebElement> list1 = driver.findElements(By.xpath("//a[@data-e2e='search-common-link']//p"));
        List<WebElement> list2 = driver.findElements(By.xpath("//a[@data-e2e='search-common-link']//strong"));
        if (!list1.isEmpty()) {
            for (WebElement e : list1) hashtags += e.getText() + " ";
        } else if (!list2.isEmpty()) {
            for (WebElement e : list2) hashtags += e.getText() + " ";
        }
        return new VideoInfo(fileName, link, view, like, comment, title, hashtags, music);
    }


    private void exportExcel(List<VideoInfo> info) throws IOException {
        File file = new File(rootLocation + "\\" + name + "\\info.xlsx");
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

    private void downloadImage(String imageUrl, File destinationFile) {
        try {
            URL url = new URL(imageUrl);

            // 1. Open the connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 2. CRITICAL: Fake a browser User-Agent to avoid 403 Forbidden errors
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.setReadTimeout(5000);

            // 3. Check if the request was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                // 4. Ensure the parent directory exists before writing
                File parentDir = destinationFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // 5. Stream the data to the file
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                System.out.println("[ERROR] Server returned code: " + responseCode + " for URL: " + imageUrl);
            }

        } catch (IOException e) {
            System.out.println("[EXCEPTION] Failed to download image: " + e.getMessage());
        }
    }

    // Method to download video using HttpURLConnection
    public void downloadVideo(String videoUrl, String fileName, File saveDirectory, String cookies) throws IOException {

        String filePath = saveDirectory.getAbsolutePath() + File.separator + fileName;

        URL url = new URL(videoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Referer", "https://www.tiktok.com/");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        if (!cookies.isBlank())
            connection.setRequestProperty("Cookie", cookies);


        // Get input stream for the video content
        long bufferSize = 1024 * 1024; // 1 MB buffer size
        long position = 0;

        InputStream in = new BufferedInputStream(connection.getInputStream());
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);

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
    }

    public void loadCookiesFromFile(WebDriver driver) {
        File file = new File("user_cookies.dat");

        if (!file.exists()) {
            System.out.println("cookie file not found, creating empty file...");
            try {
                file.createNewFile();  // creates empty file
            } catch (IOException e) {
                System.out.println("Failed to create cookie file");
            }
            return; // nothing to load
        }

        System.out.println("loaded your cookies");

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Set<Cookie> cookies = (Set<Cookie>) ois.readObject();
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
        } catch (Exception e) {
            System.out.println("Error loading cookies: " + e.getMessage());
        }
    }

}