package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class VideoPanel extends JPanel {

    private static final JLabel urlLabel = new JLabel("Link to video:");
    private static final JTextField urlTextField = new JTextField();
    private static final JLabel locationLabel = new JLabel("Save Location:");
    private static final JTextField locationTextField = new JTextField();
    private static final JButton browseButton = new JButton("Browse");
    private static final JButton downloadButton = new JButton("Download");


    private static VideoPanel videoPanel = null;

    public static VideoPanel getInstance(){
        if(videoPanel == null){
            createVideoPanel();
        }
        return videoPanel;
    }


    private static void createVideoPanel() {
        videoPanel = new VideoPanel();
        videoPanel.setLayout(null);
        setUp();
    }
    private static void setUp(){
        urlLabel.setBounds(50, 120, 100, 30);
        urlTextField.setBounds(150, 120, 400, 30);
        locationLabel.setBounds(50, 180, 100, 30);
        locationTextField.setBounds(150, 180, 250, 30);
        browseButton.setBounds(410, 180, 80, 30);
        downloadButton.setBounds(250, 240, 100, 30);

        addBrowseListener();
        addDownloadListener();

        videoPanel.add(urlLabel);
        videoPanel.add(urlTextField);
        videoPanel.add(locationLabel);
        videoPanel.add(locationTextField);
        videoPanel.add(browseButton);
        videoPanel.add(downloadButton);
    }
    private static void addDownloadListener(){
        downloadButton.addActionListener(e -> {
            if(urlTextField.getText().isBlank()
                    || locationTextField.getText().isBlank()){
                return;
            }
            String url = urlTextField.getText();
            String filename = UUID.randomUUID() + ".mp4";
            String saveLocation = locationTextField.getText();
            Main main = new Main();
            WebDriver webDriver = main.createAndGetDriver(true);
            String videoUrl = main.getVideoUrl(webDriver, url);
            if(videoUrl == null || videoUrl.isEmpty()){
                try {
                    main.loadCookiesFromFile(webDriver);
                    videoUrl = main.getVideoUrl(webDriver, url);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
            String cookies = main.getCookiesString(webDriver);
            try {
                main.downloadVideo(videoUrl, filename, new File(saveLocation), cookies);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            webDriver.quit();
            JOptionPane.showMessageDialog(
                    videoPanel, "download successfully at: " + saveLocation
                    + "\\" + filename);
        });
    }
    private static void addBrowseListener(){
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(videoPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                String selectedFolder = fileChooser.getSelectedFile().getAbsolutePath();
                locationTextField.setText(selectedFolder);
            }
        });
    }
}
