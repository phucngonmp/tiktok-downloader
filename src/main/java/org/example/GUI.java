package org.example;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class GUI {
    public static boolean isScanning = false;
    public static boolean isDownloading = false;
    public static boolean isOpenAndSolvedCaptcha = false;
    private static JTextField usernameTextField = new JTextField();
    private static JComboBox<String> downloadOptionJComboBox = new JComboBox<>(new String[]{
            "20 videos", "50 videos", "100 videos", "20%", "50%", "100%"});
    private static JComboBox<String> sortDropdown = new JComboBox<>(new String[]{"latest", "popular", "oldest"});
    private static JButton openURL = new JButton("Solve Captcha");
    private static JButton scanButton = new JButton("Scan");
    private static JButton browseButton = new JButton("Browse");
    private static JButton downloadButton = new JButton("Download");

    private static JTextField locationTextField = new JTextField();
    private static JLabel totalVideosLabel = new JLabel("Total Videos:");
    private static JLabel downloadVideoLabel = new JLabel("Download Videos:");
    private static JSpinner spinner = new JSpinner(new SpinnerNumberModel(5, 1, 15, 1));
    private static JProgressBar progressBar = new JProgressBar(0, 100);

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tiktok Downloader");
        Main main = new Main();

        // Set the size of the JFrame
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Set layout manager
        frame.setLayout(null);

        JLabel note = new JLabel("Thread: ");
        note.setBounds(50, 20, 250, 30);
        spinner.setBounds(150, 20, 35, 30);
        JComponent editor = spinner.getEditor();
        JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
        textField.setEditable(false);
        JButton resetButton = new JButton("reset");
        resetButton.setBounds(450, 20, 100, 30);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(50, 80, 100, 30);
        usernameTextField.setBounds(150, 80, 250, 30);
        JLabel sortLabel = new JLabel("Sort: ");
        sortLabel.setBounds(50, 120, 100, 30);
        sortDropdown.setBounds(150, 120, 150, 30);
        openURL.setBounds(320, 120, 120, 30);

        scanButton.setBounds(150, 180, 150, 30);
        totalVideosLabel.setBounds(320, 180, 200, 30);


        JLabel downloadOptionLabel = new JLabel("Option: ");
        downloadOptionLabel.setBounds(50, 240, 100, 30);
        downloadOptionJComboBox.setBounds(150, 240, 150, 30);
        downloadVideoLabel.setBounds(320, 240, 200, 30);
        JLabel locationLabel = new JLabel("Save Location:");
        locationLabel.setBounds(50, 280, 100, 30);
        locationTextField.setBounds(150, 280, 250, 30);
        browseButton.setBounds(410, 280, 80, 30);
        downloadButton.setBounds(250, 340, 100, 30);

        progressBar.setStringPainted(true);
        progressBar.setBounds(50, 400, 500, 20);


        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int threads = (int) spinner.getValue();
                main.setThreads(threads);
            }
        });
        openURL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!usernameTextField.getText().isBlank()) {
                    isOpenAndSolvedCaptcha = true;
                    setEnable(true);
                    main.setClassDriver(false);
                    main.setName(usernameTextField.getText());
                    main.openAndSolveCaptcha(getSortType());
                }
            }
        });
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isScanning) {
                    scanButton.setText("Stop");
                    isScanning = true;
                    setEnable(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            if(!usernameTextField.getText().isBlank() && main.getName() != null)
                                main.scanVideo(totalVideosLabel);
                            return null;
                        }
                        @Override
                        protected void done() {
                            isScanning = false;
                            setEnable(true);
                            scanButton.setText("Scan");
                        }
                    };
                    worker.execute();

                } else{
                    isScanning = false;
                    setEnable(true);
                    scanButton.setText("Scan");
                }
            }
        });
        browseButton.addActionListener(new ActionListener() {
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
        downloadOptionJComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(main.getViewList() != null && !isDownloading){
                    double downloadOption = getDownloadOption();
                    downloadVideoLabel.setText("Download Videos: " + main.getTotalDownloadVideos(downloadOption));
                }
            }
        });
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!usernameTextField.getText().isBlank() && !locationTextField.getText().isBlank()){
                    isDownloading = true;
                    if(main.getName() == null){
                        main.setName(usernameTextField.getText());
                        isOpenAndSolvedCaptcha = true;
                    }
                    main.setSaveLocation(locationTextField.getText());
                    setEnable(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {

                                double downloadOption = getDownloadOption();
                                main.main(downloadOption, progressBar);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            isDownloading = false;
                            isOpenAndSolvedCaptcha = false;
                            setEnable(true);
                            JOptionPane.showMessageDialog(frame, "Process complete!");
                            reset(main);
                        }
                    };
                    worker.execute();
                }
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset(main);
            }
        });

        // Add components to the frame
        frame.add(resetButton);
        frame.add(spinner);
        frame.add(note);
        frame.add(downloadVideoLabel);
        frame.add(usernameLabel);
        frame.add(usernameTextField);
        frame.add(openURL);
        frame.add(downloadOptionLabel);
        frame.add(downloadOptionJComboBox);
        frame.add(sortLabel);
        frame.add(sortDropdown);
        frame.add(totalVideosLabel);
        frame.add(locationLabel);
        frame.add(locationTextField);
        frame.add(browseButton);
        frame.add(downloadButton);
        frame.add(progressBar);
        frame.add(scanButton);
        // Make the frame visible
        frame.setVisible(true);
    }
    private static void reset(Main main){
        usernameTextField.setText("");
        totalVideosLabel.setText("Total Videos: ");
        downloadVideoLabel.setText("Download Videos: ");
        locationTextField.setText("");
        progressBar.setValue(0);
        isScanning = false;
        isDownloading = false;
        isOpenAndSolvedCaptcha = false;
        setEnable(true);
        main.reset();
    }


    private static void setEnable(boolean isEnable){
        usernameTextField.setEnabled(!isOpenAndSolvedCaptcha);
        browseButton.setEnabled(isEnable);
        openURL.setEnabled(!isOpenAndSolvedCaptcha);
        scanButton.setEnabled(!isDownloading);
        downloadButton.setEnabled(!isScanning);
        sortDropdown.setEnabled(!isOpenAndSolvedCaptcha);
        downloadButton.setEnabled(isEnable);
        locationTextField.setEnabled(isEnable);
        downloadOptionJComboBox.setEnabled(isEnable);
        spinner.setEnabled(!isDownloading);
    }

    private static String getSortType(){
        return (String) sortDropdown.getSelectedItem();
    }
    private static double getDownloadOption(){
        String option = (String) downloadOptionJComboBox.getSelectedItem();
        // percent options
        if(option.endsWith("%")){
            return Double.parseDouble(option.substring(0, option.length()-1)) * 0.01;
        }
        // video options
        return Integer.parseInt(option.split(" ")[0]);
    }

}
