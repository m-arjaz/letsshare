package tester;

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Receive extends BaseWindow {
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final JButton exitButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private volatile boolean isListening = false;

    public Receive() {
        super("Receive Data", AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);

        // Initialize components
        connectButton = createButton("Connect");
        disconnectButton = createButton("Disconnect");
        exitButton = createButton("Exit");
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        
        statusLabel = new JLabel("Status: Not Connected");
        statusLabel.setFont(AppConstants.DEFAULT_FONT);

        setupUI();
        setupListeners();
        
        // Initial button states
        disconnectButton.setEnabled(false);
        
        setVisible(true);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(AppConstants.DEFAULT_FONT);
        button.setFocusable(false);
        return button;
    }

    private void setupUI() {
        // Connect Button
        connectButton.setBounds(180, 70, 150, 25);
        add(connectButton);

        // Status Label
        statusLabel.setBounds(32, 120, 448, 25);
        add(statusLabel);

        // Progress Bar
        progressBar.setBounds(32, 220, 448, 25);
        add(progressBar);

        // Disconnect and Exit buttons
        disconnectButton.setBounds(220, 430, 150, 25);
        add(disconnectButton);

        exitButton.setBounds(380, 430, 100, 25);
        add(exitButton);
    }

    private void setupListeners() {
        connectButton.addActionListener(e -> startServer());
        disconnectButton.addActionListener(e -> disconnect());
        exitButton.addActionListener(e -> exit());
    }
    
    private boolean isPortAvailable(int port) {
        try (ServerSocket testSocket = new ServerSocket(port)) {
            testSocket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void startServer() {
        if (isListening) {
            showError("Server is already running!");
            return;
        }

        if (!isPortAvailable(5000)) {
            showError("Port 5000 is already in use. Please wait a moment and try again.");
            return;
        }
        progressBar.setValue(0);
        progressBar.setString("0%");
        new ServerTask().execute();
    }

    private void disconnect() {
    	try {
            isListening = false;
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
            resetUI();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error during disconnection: " + e.getMessage());
        }
    }

    private void resetUI() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            statusLabel.setText("Status: Not Connected");
            isListening = false;
        });
    }

    @Override
    public void dispose() {
        disconnect();
        super.dispose();
    }

    private void exit() {
        disconnect();
        
        // Small delay to ensure resources are released
        Timer timer = new Timer(100, e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new ApplicationGUI().setVisible(true));
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private class ServerTask extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() throws Exception {
            try {
            	if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
            	}
            	
                // Add firewall rules
                addFirewallRules();

                // Create and configure new server socket
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(5000));
                
                isListening = true;
                publish("Status: Waiting for connection...");
                connectButton.setEnabled(false);

                clientSocket = serverSocket.accept();
                clientSocket.setReceiveBufferSize(AppConstants.BUFFER_SIZE);
                clientSocket.setSoTimeout(AppConstants.SOCKET_TIMEOUT);

                publish("Status: Connected with " + clientSocket.getInetAddress().getHostAddress());
                SwingUtilities.invokeLater(() -> showMessage("Connected succesfully with " + clientSocket.getInetAddress().getHostAddress()));
                SwingUtilities.invokeLater(() -> {
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                });

                // Start receiving file
                new FileReceiveTask(clientSocket).execute();

            } catch (Exception e) {
                e.printStackTrace();
                publish("Status: Connection failed");
                SwingUtilities.invokeLater(() -> {
                    resetUI();
                    showError("Connection failed: " + e.getMessage());
                });
                throw e;
            } finally {
                // Remove firewall rules
                removeFirewallRules();
            }
            return null;
        }

        @Override
        protected void process(java.util.List<String> chunks) {
            String latestStatus = chunks.get(chunks.size() - 1);
            statusLabel.setText(latestStatus);
        }
    }

    private class FileReceiveTask extends SwingWorker<Void, Integer> {
        private final Socket socket;

        public FileReceiveTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                DataInputStream dataIn = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream(), AppConstants.BUFFER_SIZE));

                // Wait for handshake
                while (true) {
                    if (dataIn.readUTF().equals(AppConstants.HANDSHAKE_MESSAGE))
                        break;
                }

                // Receive file info
                long fileSize = dataIn.readLong();
                String fileName = dataIn.readUTF();

                String saveDir = System.getProperty("user.home") + "/Downloads";
                File saveFile = new File(saveDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(saveFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos, AppConstants.BUFFER_SIZE)) {

                    byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long startTime = System.currentTimeMillis();

                    while (totalBytesRead < fileSize && (bytesRead = dataIn.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (totalBytesRead % (1024 * 1024) == 0) { // Every 1MB
                            bos.flush();
                        }

                        updateProgress(totalBytesRead, fileSize, startTime);
                    }

                    bos.flush();

                    // Verify file size
                    if (totalBytesRead != fileSize) {
                        throw new IOException("Incomplete file transfer");
                    }

                    // Log the transfer
                    String[] data = {
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        "Receive",
                        socket.getInetAddress().getHostAddress(),
                        fileName,
                        getReadableFileSize(fileSize)
                    };
                    addLog(data);
                }

                SwingUtilities.invokeLater(() -> showMessage("File received successfully!"));

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> showError("File transfer failed: " + e.getMessage()));
            } finally {
                disconnect();
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

    private void addFirewallRules() throws IOException, InterruptedException {
        // Add inbound rule
        @SuppressWarnings("deprecation")
		Process inboundProcess = Runtime.getRuntime().exec(
            "cmd /c netsh advfirewall firewall add rule name=\"Server Inbound\" dir=in action=allow protocol=TCP localport=5000"
        );
        inboundProcess.waitFor();

        // Add outbound rule
        @SuppressWarnings("deprecation")
		Process outboundProcess = Runtime.getRuntime().exec(
            "cmd /c netsh advfirewall firewall add rule name=\"Server Outbound\" dir=out action=allow protocol=TCP localport=5000"
        );
        outboundProcess.waitFor();
    }

    private void removeFirewallRules() throws IOException, InterruptedException {
        // Remove inbound rule
        @SuppressWarnings("deprecation")
		Process deleteInbound = Runtime.getRuntime().exec(
            "cmd /c netsh advfirewall firewall delete rule name=\"Server Inbound\""
        );
        deleteInbound.waitFor();

        // Remove outbound rule
        @SuppressWarnings("deprecation")
		Process deleteOutbound = Runtime.getRuntime().exec(
            "cmd /c netsh advfirewall firewall delete rule name=\"Server Outbound\""
        );
        deleteOutbound.waitFor();
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
        SwingUtilities.invokeLater(Receive::new);
    }
}