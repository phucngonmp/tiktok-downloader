package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class Main {
    private WebDriver classDriver;
    private int threads = 5;
    private String name;
    private String saveLocation;
    private List<WebElement> links = null;
    private List<WebElement> viewList = null;
    public Main(){

    }
    public WebDriver createAndGetDriver(Boolean isHeadlessMode){
        classDriver = newDriver(isHeadlessMode);
        return classDriver;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName(){
        return this.name;
    }
    public List<WebElement> getViewList(){
        return this.viewList;
    }
    public void setSaveLocation(String saveLocation) {
        this.saveLocation = saveLocation;
    }
    public void setClassDriver(Boolean isHeadlessMode) {
        this.classDriver = newDriver(isHeadlessMode);
    }
    public void setThreads(int threads){this.threads = threads;}
    public void reset(){
        name = null;
        if(classDriver != null){
            classDriver.quit();
            classDriver = null;
        }
    }

    private WebDriver newDriver(Boolean isHeadlessMode) {
        WebDriverManager.chromedriver().setup();
        // Create ChromeOptions object to configure the browser
        ChromeOptions options = new ChromeOptions();
        if(isHeadlessMode){
            options.addArguments("--headless");
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
        WebDriver webDriver = new ChromeDriver(options);

        return webDriver;
    }
    public void main(double downloadOption, JProgressBar progressBar) throws IOException, InterruptedException {
        File newDir = new File(saveLocation + "\\" + name);
        if (!newDir.exists()) {
            newDir.mkdirs();  // Creates the directory if it doesn't exist
        }
        File errorFile = new File(newDir,  "error.txt");
        FileWriter error = new FileWriter(errorFile);
        File downloadedFile = new File(newDir,  "downloaded.txt");
        FileWriter downloaded = new FileWriter(downloadedFile);
        int total = getTotalDownloadVideos(downloadOption);

        System.out.println("total videos: " + total);
        List<VideoInfo> info = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger done = new AtomicInteger(0);
        for (int i = 0; i < total; i++) {
            String href = links.get(i).getAttribute("href");
            System.out.println(href);
            String view = viewList.get(i).getText();
            System.out.println(view);
            int index = i + 1;
            double finalTotal = total;

            executor.submit(() -> {
                try {
                    processDownloadVideo(info, href, index, newDir, view, error, downloaded);
                    int completed = done.incrementAndGet();
                    progressBar.setValue((int) (completed * 100 / finalTotal));
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
        Collections.sort(info,Comparator.comparing((VideoInfo v) -> extractNumber(v.getView())).reversed());
        exportExcel(info);
        System.out.println("done");
        downloaded.close();
        error.close();
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
    public int getTotalDownloadVideos(double downloadOption){
        double total = links.size();
        if(downloadOption <= 1){
            total = Math.ceil(total * downloadOption);
        } else if(downloadOption < total){
            total = downloadOption;
        }
        return (int) total;
    }

    // first scan mean make webdriver find sort by popular or latest or oldest and do one scan
    public void firstScan(String sortOption) throws InterruptedException {
        if(classDriver == null){
            classDriver = newDriver(true);
            classDriver.get("https://www.tiktok.com/@" + name);
        } else{
            classDriver.get("https://www.tiktok.com/@" + name);
        }
        WebDriverWait wait = new WebDriverWait(classDriver, Duration.ofSeconds(5));
        if(sortOption.equals("popular")){
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='TUXSegmentedControl-itemTitle' and text()='Popular']"))).click();
            System.out.println("clicked popular");
            Thread.sleep(3000);
        } else if(sortOption.equals("latest")){
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='TUXSegmentedControl-itemTitle' and text()='Latest']"))).click();
            System.out.println("clicked latest");
        } else{
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='TUXSegmentedControl-itemTitle' and text()='Oldest']"))).click();
            Thread.sleep(3000);
        }
        Thread.sleep(2000);
        links = classDriver.findElements(
                By.xpath("//div[contains(@class, 'css-1j167yi-DivWrapper') " +
                    "and contains(@class, 'e1cg0wnj1')]//a"));
        viewList = classDriver.findElements(By.cssSelector("strong[data-e2e='video-views']"));
        // if links is empty that mean the user is restricted make sure login with properly account
        if(links.isEmpty()){
            try {
                loadCookiesFromFile(classDriver);
                System.out.println("loaded your cookies");
                firstScan(sortOption);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void scanVideo(JLabel totalVideoLabel){
        if(classDriver == null){
            return;
        }
        try {
            links = classDriver.findElements(
                    By.xpath("//div[contains(@class, 'css-1j167yi-DivWrapper') " +
                            "and contains(@class, 'e1cg0wnj1')]//a"));
            viewList = classDriver.findElements(By.cssSelector("strong[data-e2e='video-views']"));
            long lastHeight = (long) ((JavascriptExecutor) classDriver).executeScript("return document.body.scrollHeight");
            int waitCount = 0;
            while (UserPanel.isScanning) {
                ((JavascriptExecutor) classDriver).executeScript("window.scrollBy(0, document.body.scrollHeight);");

                // Wait for content to load
                Thread.sleep(1500); // Adjust delay if needed
                links = classDriver.findElements(
                        By.xpath("//div[contains(@class, 'css-1j167yi-DivWrapper') " +
                                "and contains(@class, 'e1cg0wnj1')]//a"));
                viewList = classDriver.findElements(By.cssSelector("strong[data-e2e='video-views']"));
                // Get the new height
                long newHeight = (long) ((JavascriptExecutor) classDriver).executeScript("return document.body.scrollHeight");
                System.out.println("New Height: " + newHeight);

                // Break the loop if no new content is loaded
                if (newHeight == lastHeight) {
                    waitCount++;
                }
                if(waitCount == 3){
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
        if(links.isEmpty()){
            System.out.println("can't find videos");
        }
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
                String videoUrl = getVideoUrl(webDriver);
                String cookies = getCookiesString(webDriver);
                VideoInfo videoInfo = createInfo(webDriver, fileName, href, view);
                webDriver.quit();
                if (videoUrl != null && !videoUrl.isBlank()) {
                    try{
                        downloadVideo(videoUrl, fileName, newDir, cookies);
                        System.out.println("Attempt " + (i + 1) + " success!");
                        info.add(videoInfo);
                        logSuccess(videoInfo, dowloaded);
                        break; // Exit the loop on success
                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket Timeout error in downloading video: " + videoUrl);
                    } catch (IOException e2) {
                        System.out.println("I/O error in downloading video: " + videoUrl);
                    }
                }
                else {
                    System.err.println("No video found. Attempt " + (i + 1));
                }

            } catch (Exception e) {
                System.err.println("Error on attempt " + (i + 1) + ": " + e.getMessage());
            }
            if (i == MAX_RETRIES - 1) {
                logError(fileName, error, href);
            }
        }
    }
    public String getVideoUrl(WebDriver webDriver) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        WebElement videoElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("video")));
        List<WebElement> sourceElements = videoElement.findElements(By.tagName("source"));
        return sourceElements.isEmpty() ? videoElement.getAttribute("src") :
                    sourceElements.get(0).getAttribute("src");
    }
    public String getCookiesString(WebDriver webDriver){
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
        String hashtags = "";
        String music = "";
        try{
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
            WebElement musicElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".css-pvx3oa-DivMusicText.epjbyn3")));
            music = musicElement.getText();
        } catch (Exception me){
            System.out.println("can't located music element");
        }
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
    public void downloadVideo(String videoUrl, String fileName, File directory, String cookies) throws IOException {

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
    public void loadCookiesFromFile(WebDriver driver) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("user_cookies.dat"))) {
            Set<Cookie> cookies = (Set<Cookie>) ois.readObject();
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
        }
    }
}