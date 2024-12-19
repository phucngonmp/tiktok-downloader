package org.example;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class GUI {

    private static List<WebElement> videoElements;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tiktok downloader by username");

        // Set the size of the JFrame
        frame.setSize(600, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Set layout manager
        frame.setLayout(null);

        // Username label and text field
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(50, 20, 100, 30);
        JTextField usernameTextField = new JTextField();
        usernameTextField.setBounds(150, 20, 150, 30);
        JButton openURL = new JButton("Open");
        openURL.setBounds(350, 20, 80, 30);
        openURL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!usernameTextField.getText().isBlank()) {
                    try {
                        Main.openAndSolveCaptcha(usernameTextField.getText());
                    } catch (URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        // Sort label and dropdown
        JLabel percentLabel = new JLabel("Download %: ");
        percentLabel.setBounds(50, 60, 100, 30);
        JComboBox<String> percentDropdown = new JComboBox<>(new String[]{"25%", "50%", "100%"});
        percentDropdown.setBounds(150, 60, 150, 30);

        // Percent download label and dropdown
        JLabel captchaLabel = new JLabel("Solve Captcha");
        captchaLabel.setBounds(50, 100, 100, 30);
        JLabel totalVideosLabel = new JLabel("Total Videos:");
        totalVideosLabel.setBounds(300, 100, 100, 30);
        JCheckBox captchaCheckbox = new JCheckBox();
        captchaCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (captchaCheckbox.isSelected()) {
                    videoElements = Main.getTotalVideos();
                    totalVideosLabel.setText("Total Videos: " + videoElements.size());
                }
            }
        });
        captchaCheckbox.setBounds(150, 100, 20, 30);


        JLabel locationLabel = new JLabel("Save Location:");
        locationLabel.setBounds(50, 140, 100, 30);
        JTextField locationTextField = new JTextField();
        locationTextField.setBounds(150, 140, 250, 30);
        JButton openButton = new JButton("Browse");
        openButton.setBounds(410, 140, 80, 30);

        // Thêm sự kiện cho nút "Browse"
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Tạo JFileChooser
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Chỉ cho phép chọn thư mục

                // Hiển thị hộp thoại
                int result = fileChooser.showOpenDialog(frame);

                // Xử lý kết quả người dùng
                if (result == JFileChooser.APPROVE_OPTION) {
                    // Lấy đường dẫn thư mục đã chọn
                    String selectedFolder = fileChooser.getSelectedFile().getAbsolutePath();
                    // Hiển thị đường dẫn trong JTextField
                    locationTextField.setText(selectedFolder);
                }
            }
        });

        // Buttons at the bottom
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        loginButton.setBounds(150, 200, 100, 30);
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!usernameTextField.getText().isBlank() && !locationTextField.getText().isBlank()){
                    Main main = new Main(usernameTextField.getText(), locationTextField.getText());
                    try {
                        main.main(videoElements);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    } catch (URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        downloadButton.setBounds(300, 200, 100, 30);

        // Add components to the frame
        frame.add(usernameLabel);
        frame.add(usernameTextField);
        frame.add(openURL);
        frame.add(percentLabel);
        frame.add(percentDropdown);
        frame.add(captchaLabel);
        frame.add(captchaCheckbox);
        frame.add(totalVideosLabel);
        frame.add(locationLabel);
        frame.add(locationTextField);
        frame.add(openButton);
        frame.add(loginButton);
        frame.add(downloadButton);

        // Make the frame visible
        frame.setVisible(true);
    }

    public static void login(){
        Login.run();
    }
}
