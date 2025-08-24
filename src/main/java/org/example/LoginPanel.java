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
    private static final LoginPanel LOGIN_PANEL = new LoginPanel();
    private static final JLabel note = new JLabel("Use this when you need to download restricted users or refresh cookies");
    private static final JLabel startLabel = new JLabel("Click Start Login to proceed to login manually");
    private static final JLabel endLabel = new JLabel("When finished, click Done to save the cookies");
    private static final JButton startButton = new JButton("Start Login");
    private static final JButton doneButton = new JButton("Done");
    public static LoginPanel getInstance() {
        setUp();
        return LOGIN_PANEL;
    }
    private LoginPanel() {
        this.setLayout(null);
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
            JOptionPane.showMessageDialog(LOGIN_PANEL, "cookies saved");
        });

        LOGIN_PANEL.add(note);
        LOGIN_PANEL.add(startLabel);
        LOGIN_PANEL.add(startButton);
        LOGIN_PANEL.add(endLabel);
        LOGIN_PANEL.add(doneButton);
    }
    private static void saveCookiesToFile(WebDriver driver, String filePath) throws IOException {
        Set<Cookie> cookies = driver.manage().getCookies();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(cookies);
        }
    }
}
