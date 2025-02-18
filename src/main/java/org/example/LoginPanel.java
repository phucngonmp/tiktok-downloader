package org.example;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;

public class LoginPanel extends JPanel {
    private static WebDriver driver = null;
    private static LoginPanel loginPanel = null;
    private static final JLabel note = new JLabel("Use this when you need to download restricted users or refresh cookies");
    private static final JLabel startLabel = new JLabel("Click Start Login to proceed to login manually");
    private static final JLabel endLabel = new JLabel("When finished, click Done to save the cookies");
    private static final JButton startButton = new JButton("Start Login");
    private static final JButton doneButton = new JButton("Done");
    public static LoginPanel getInstance() {
        if (loginPanel == null) {
            createUserPanel();
        }
        return loginPanel;
    }
    private static void createUserPanel() {
        loginPanel = new LoginPanel();
        loginPanel.setLayout(null);
        setUp();
    }

    private static void setUp(){
        note.setBounds(90, 60, 450, 30);
        startLabel.setBounds(150, 140, 300, 30);
        startButton.setBounds(150, 170, 250, 30);
        endLabel.setBounds(150, 280, 300, 30);
        doneButton.setBounds(150, 310, 250, 30);

        startButton.addActionListener(e -> {
            Main main = new Main();
            driver = main.createAndGetDriver(false);
            driver.manage().window().maximize();
            driver.get("https://www.tiktok.com/login");
        });
        doneButton.addActionListener(e -> {
            try {
                saveCookiesToFile(driver, "user_cookies.dat");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            driver.quit();
            JOptionPane.showMessageDialog(loginPanel, "cookies saved");
        });

        loginPanel.add(note);
        loginPanel.add(startLabel);
        loginPanel.add(startButton);
        loginPanel.add(endLabel);
        loginPanel.add(doneButton);
    }
    private static void saveCookiesToFile(WebDriver driver, String filePath) throws IOException {
        Set<Cookie> cookies = driver.manage().getCookies();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(cookies);
        }
    }
}
