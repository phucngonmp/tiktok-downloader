package org.example;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class UserPanel extends JPanel {
    public static UserPanel userPanel = null;
    public static boolean isScanning = false;
    public static boolean isDownloading = false;
    private static boolean isFirstScan = true;
    private static final JTextField usernameTextField = new JTextField();
    private static final JComboBox<String> downloadOptionJComboBox = new JComboBox<>(new String[]{
            "20 videos", "50 videos", "100 videos", "20%", "50%", "100%"});
    private static final JComboBox<String> sortDropdown = new JComboBox<>(new String[]{"latest", "popular", "oldest"});
    private static final JButton scanButton = new JButton("Scan");
    private static final JButton browseButton = new JButton("Browse");
    private static final JButton downloadButton = new JButton("Download");

    private static final JTextField locationTextField = new JTextField();
    private static JLabel totalVideosLabel = new JLabel("Total Videos:");
    private static JLabel downloadVideoLabel = new JLabel("Download Videos:");
    private static final JSpinner spinner = new JSpinner(new SpinnerNumberModel(5, 1, 15, 1));
    private static JProgressBar progressBar = new JProgressBar(0, 100);

    public static UserPanel getInstance() {
        if (userPanel == null) {
            createUserPanel();
        }
        return userPanel;
    }
    private static void createUserPanel() {
        userPanel = new UserPanel();
        userPanel.setLayout(null);
        setUp();
    }

    private static void setUp(){
        Main main = new Main();

        JLabel note = new JLabel("Thread: ");
        note.setBounds(50, 60, 250, 30);
        spinner.setBounds(150, 60, 35, 30);
        JComponent editor = spinner.getEditor();
        JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
        textField.setEditable(false);
        JButton resetButton = new JButton("reset");
        resetButton.setBounds(450, 60, 100, 30);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(50, 120, 100, 30);
        usernameTextField.setBounds(150, 120, 250, 30);
        JLabel sortLabel = new JLabel("Sort: ");
        sortLabel.setBounds(50, 160, 100, 30);
        sortDropdown.setBounds(150, 160, 150, 30);

        scanButton.setBounds(150, 240, 150, 30);
        totalVideosLabel.setBounds(320, 240, 200, 30);


        JLabel downloadOptionLabel = new JLabel("Option: ");
        downloadOptionLabel.setBounds(50, 300, 100, 30);
        downloadOptionJComboBox.setBounds(150, 300, 150, 30);
        downloadVideoLabel.setBounds(320, 300, 200, 30);
        JLabel locationLabel = new JLabel("Save Location:");
        locationLabel.setBounds(50, 340, 100, 30);
        locationTextField.setBounds(150, 340, 250, 30);
        browseButton.setBounds(410, 340, 80, 30);
        downloadButton.setBounds(250, 400, 100, 30);

        progressBar.setStringPainted(true);
        progressBar.setBounds(50, 460, 500, 20);


        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int threads = (int) spinner.getValue();
                main.setThreads(threads);
            }
        });
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(isFirstScan){
                    setEnable(true);
                    main.setIsHeadlessMode(true);
                    main.setName(usernameTextField.getText().trim());
                    try {
                        main.firstScan(getSortType());
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    isFirstScan = false;
                }
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
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(userPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String selectedFolder = fileChooser.getSelectedFile().getAbsolutePath();
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
                        main.setName(usernameTextField.getText().trim());
                        try {
                            main.firstScan(getSortType());
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                        isFirstScan = false;
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
                            isFirstScan = true;
                            setEnable(true);
                            JOptionPane.showMessageDialog(userPanel, "Process complete!");
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
        userPanel.add(resetButton);
        userPanel.add(spinner);
        userPanel.add(note);
        userPanel.add(downloadVideoLabel);
        userPanel.add(usernameLabel);
        userPanel.add(usernameTextField);
        userPanel.add(downloadOptionLabel);
        userPanel.add(downloadOptionJComboBox);
        userPanel.add(sortLabel);
        userPanel.add(sortDropdown);
        userPanel.add(totalVideosLabel);
        userPanel.add(locationLabel);
        userPanel.add(locationTextField);
        userPanel.add(browseButton);
        userPanel.add(downloadButton);
        userPanel.add(progressBar);
        userPanel.add(scanButton);
    }
    private static void reset(Main main){
        usernameTextField.setText("");
        totalVideosLabel.setText("Total Videos: ");
        downloadVideoLabel.setText("Download Videos: ");
        locationTextField.setText("");
        progressBar.setValue(0);
        isScanning = false;
        isDownloading = false;
        isFirstScan = true;
        setEnable(true);
        main.reset();
    }
    private static void setEnable(boolean isEnable){
        usernameTextField.setEnabled(isFirstScan);
        browseButton.setEnabled(isEnable);
        scanButton.setEnabled(!isDownloading);
        downloadButton.setEnabled(!isScanning);
        sortDropdown.setEnabled(isFirstScan);
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
