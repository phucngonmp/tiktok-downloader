package org.example;

import javax.swing.*;
import java.awt.*;

public class GUI {
    private static final JPanel userPanel = UserPanel.getInstance();
    private static final JPanel videoPanel = VideoPanel.getInstance();
    private static final JPanel loginPanel = LoginPanel.getInstance();
    private static final JPanel cardPanel = new JPanel(new CardLayout());


    public static void main(String[] args) {
        JFrame frame = new JFrame("Tiktok Downloader");

        // Set the size of the JFrame
        frame.setSize(600, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Set layout manager
        userPanel.setLayout(null);
        videoPanel.setLayout(null);
        loginPanel.setLayout(null);


        // Add navigation buttons to each panel
        addNavigatorsToPanel(userPanel);
        addNavigatorsToPanel(videoPanel);
        addNavigatorsToPanel(loginPanel);
        // Add panels to CardLayout
        cardPanel.add(userPanel, "User Download");
        cardPanel.add(videoPanel, "Video Download");
        cardPanel.add(loginPanel, "Login");
        frame.add(cardPanel);
        frame.setVisible(true);
    }
    private static void addNavigatorsToPanel(JPanel panel) {
        CardLayout cl = (CardLayout) cardPanel.getLayout();

        JButton toUser = new JButton("User Download");
        toUser.setBounds(25, 10, 163, 30);
        JButton toVideo = new JButton("Video Download");
        toVideo.setBounds(213, 10, 163, 30);
        JButton toLogin = new JButton("Login");
        toLogin.setBounds(400, 10, 163, 30);

        toUser.addActionListener(e -> cl.show(cardPanel, "User Download"));
        toVideo.addActionListener(e -> cl.show(cardPanel, "Video Download"));
        toLogin.addActionListener(e -> cl.show(cardPanel, "Login"));

        panel.add(toUser);
        panel.add(toVideo);
        panel.add(toLogin);
    }

}
