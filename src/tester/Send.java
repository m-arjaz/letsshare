package tester;

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Send extends BaseWindow {
    private static final int TIMEOUT_MS = AppConstants.SOCKET_TIMEOUT;
    private final JButton connectButton;
    private final JButton browseButton;
    private final JButton exitButton;
    private final JButton disconnectButton;
    private final JButton sendButton;
    private final JTextField ipField;
    private final JTextField fileField;
    private final JProgressBar progressBar;
    private File selectedFile;
    private Socket socket;
    private volatile boolean isTransferring = false;

    public Send() {
        super("Send Data", AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);

        // Initialize components
        JLabel ipLabel = new JLabel("Set IP address:");
        ipLabel.setBounds(20, 20, 150, 25);
        ipLabel.setFont(AppConstants.DEFAULT_FONT);
        add(ipLabel);

        ipField = new JTextField();
        ipField.setBounds(180, 20, 200, 25);
        add(ipField);

        connectButton = new JButton("Connect");
        connectButton.setBounds(390, 20, 100, 25);
        connectButton.setFont(AppConstants.DEFAULT_FONT);
        add(connectButton);

        JLabel fileLabel = new JLabel("Select File:");
        fileLabel.setBounds(20, 70, 150, 25);
        fileLabel.setFont(AppConstants.DEFAULT_FONT);
        add(fileLabel);

        fileField = new JTextField();
        fileField.setBounds(180, 70, 200, 25);
        fileField.setEditable(false);
        add(fileField);

        browseButton = new JButton("Browse");
        browseButton.setBounds(390, 70, 100, 25);
        browseButton.setFont(AppConstants.DEFAULT_FONT);
        add(browseButton);

        progressBar = new JProgressBar();
        progressBar.setBounds(32, 220, 448, 25);
        progressBar.setStringPainted(true);
        add(progressBar);

        sendButton = new JButton("Send");
        sendButton.setBounds(390, 110, 100, 25);
        sendButton.setFont(AppConstants.DEFAULT_FONT);
        add(sendButton);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.setBounds(220, 430, 150, 25);
        disconnectButton.setFont(AppConstants.DEFAULT_FONT);
        add(disconnectButton);

        exitButton = new JButton("Exit");
        exitButton.setBounds(380, 430, 100, 25);
        exitButton.setFont(AppConstants.DEFAULT_FONT);
        add(exitButton);

        // Initial button states
        browseButton.setEnabled(false);
        sendButton.setEnabled(false);
        disconnectButton.setEnabled(false);

        // Add listeners
        connectButton.addActionListener(e -> connectToServer());
        browseButton.addActionListener(e -> handleFileSelection());
        sendButton.addActionListener(e -> startFileTransfer());
        disconnectButton.addActionListener(e -> disconnect());
        exitButton.addActionListener(e -> exit());

        setVisible(true);
    }

    private void connectToServer() {
        String ipAddress = ipField.getText().trim();
        if (ipAddress.isEmpty()) {
            showError("Please enter an IP address!");
            return;
        }
        if (!isValidIpAddress(ipAddress)) {
            showError("Invalid IP address format!");
            return;
        }

        try {
            socket = new Socket();
            socket.setReuseAddress(true);
            socket.connect(new InetSocketAddress(ipAddress, 5000), TIMEOUT_MS);
            socket.setSendBufferSize(AppConstants.BUFFER_SIZE);
            socket.setSoTimeout(TIMEOUT_MS);

            connectButton.setEnabled(false);
            browseButton.setEnabled(true);
            fileField.setEditable(true);
            disconnectButton.setEnabled(true);
            ipField.setEditable(false);
            SwingUtilities.invokeLater(() -> showMessage("Connected succesfully with " + socket.getInetAddress().getHostAddress()));
            progressBar.setValue(0);
            progressBar.setString("0%");
        } catch (IOException e) {
            showError("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isValidIpAddress(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void handleFileSelection() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
            sendButton.setEnabled(true);
        }
    }

    private void startFileTransfer() {
        if (!isTransferring && selectedFile != null) {
            isTransferring = true;
            sendButton.setEnabled(false);
            browseButton.setEnabled(false);
            fileField.setEditable(false);
            new FileTransferTask().execute();
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket=null;
            }
            resetUI();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error during disconnection: " + e.getMessage());
        }
    }

    private void resetUI() {
    	connectButton.setEnabled(true);
        browseButton.setEnabled(false);
        sendButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        ipField.setEditable(true);
        fileField.setText("");
        selectedFile = null;
        isTransferring = false;
    }

    private void exit() {
        disconnect();
        dispose();
        SwingUtilities.invokeLater(() -> new ApplicationGUI().setVisible(true));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private class FileTransferTask extends SwingWorker<Void, Integer> {
        @Override
        protected Void doInBackground() throws Exception {
            try (FileInputStream fis = new FileInputStream(selectedFile);
                 BufferedInputStream bis = new BufferedInputStream(fis, AppConstants.BUFFER_SIZE);
                 OutputStream out = socket.getOutputStream();
                 DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out, AppConstants.BUFFER_SIZE))) {
            	
                // Send handshake and file info
                dataOut.writeUTF(AppConstants.HANDSHAKE_MESSAGE);
                dataOut.writeLong(selectedFile.length());
                dataOut.writeUTF(selectedFile.getName());
                dataOut.flush();

                byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
                int bytesRead;
                long totalBytesSent = 0;
                long fileSize = selectedFile.length();
                long startTime = System.currentTimeMillis();

                while ((bytesRead = bis.read(buffer)) != -1 && !isCancelled()) {
                    dataOut.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;

                    // Flush periodically
                    if (totalBytesSent % (1024 * 1024) == 0) { // Every 1MB
                        dataOut.flush();
                        Thread.sleep(1); // Small delay to prevent overwhelming
                    }

                    updateProgress(totalBytesSent, fileSize, startTime);
                }

                // Final flush
                dataOut.flush();

                // Log the transfer
                String[] data = {
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "Send",
                    socket.getInetAddress().getHostAddress(),
                    selectedFile.getName(),
                    getReadableFileSize(fileSize)
                };
                addLog(data);
                SwingUtilities.invokeLater(() -> showMessage("File sent successfully!"));

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> showError("File transfer failed: " + e.getMessage()));
            } finally {
                isTransferring = false;
                SwingUtilities.invokeLater(() -> {
                	disconnect();
                    browseButton.setEnabled(true);
                });
            }
            return null;
        }

        private void updateProgress(long current,  long total, long startTime) {
            int percentage = (int) (current * 100 / total);
            double speed = current * 1000.0 / (System.currentTimeMillis() - startTime);
            publish(percentage);
            SwingUtilities.invokeLater(() -> {
                progressBar.setString(String.format("%d%% (%s/%s) - %s/s",
                    percentage,
                    getReadableFileSize(current),
                    getReadableFileSize(total),
                    getReadableFileSize((long) speed)));
            });
        }

        @Override
        protected void process(java.util.List<Integer> chunks) {
            int progress = chunks.get(chunks.size() - 1);
            progressBar.setValue(progress);
        }
    }

    public static void addLog(String[] data) {
        try {
            String folderPath = "C:\\Users\\" + System.getProperty("user.name") + "\\OneDrive\\Documents\\LetsShare";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + "\\logs.txt", true))) {
                for (String d : data) {
                    writer.append(d).append("  ");
                }
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getReadableFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Send::new);
    }
}