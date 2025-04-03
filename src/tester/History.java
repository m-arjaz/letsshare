package tester;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class History extends BaseWindow {
    private JTable historyTable;
    private JButton refreshButton;
    private JButton exitButton;
    private JLabel dateTimeLabel;
    private JLabel userLabel;
    private Timer timeUpdateTimer;

    public History() {
        super("History", 1080, 512);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize components
        historyTable = new JTable();
        refreshButton = new JButton("Refresh");
        refreshButton.setFont(AppConstants.DEFAULT_FONT);
        exitButton = new JButton("Exit");
        exitButton.setFont(AppConstants.DEFAULT_FONT);

        // Initialize date/time and user labels
        dateTimeLabel = new JLabel();
        userLabel = new JLabel("Current User's Login: " + System.getProperty("user.name"));
        updateDateTime();

        // Start timer to update date/time
        timeUpdateTimer = new Timer(1000, e -> updateDateTime());
        timeUpdateTimer.start();

        setupUI();
        setupListeners();
        refreshHistory();
        setVisible(true);
    }

    private void updateDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dateTimeLabel.setText("Current Date and Time (UTC): " + LocalDateTime.now().format(formatter));
    }

    private void setupUI() {
        // Info Panel (Date/Time and User)
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        dateTimeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        userLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(dateTimeLabel);
        infoPanel.add(userLabel);
        add(infoPanel, BorderLayout.NORTH);

        // Create table model with column titles
        String[] columnNames = {"Time", "Operation", "From / To", "File Name", "File Size"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // This makes the table read-only
            }
        };
        historyTable.setModel(tableModel);

        // Customize column widths
        TableColumn timeColumn = historyTable.getColumnModel().getColumn(0);
        TableColumn operationColumn = historyTable.getColumnModel().getColumn(1);
        TableColumn fromToColumn = historyTable.getColumnModel().getColumn(2);
        TableColumn fileNameColumn = historyTable.getColumnModel().getColumn(3);
        TableColumn fileSizeColumn = historyTable.getColumnModel().getColumn(4);

        timeColumn.setPreferredWidth(150);
        operationColumn.setPreferredWidth(80);
        fromToColumn.setPreferredWidth(80);
        fileNameColumn.setPreferredWidth(500);
        fileSizeColumn.setPreferredWidth(80);

        // Center align data in each column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set row height to make cells taller
        historyTable.setRowHeight(30);

        // Customize table header
        JTableHeader tableHeader = historyTable.getTableHeader();
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) tableHeader.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableHeader.setFont(new Font("Arial", Font.BOLD, 16));
        tableHeader.setBackground(new Color(220, 220, 220));

        // Add striping to the table rows for a better look
        historyTable.setFillsViewportHeight(true);
        historyTable.setGridColor(Color.LIGHT_GRAY);
        historyTable.setShowGrid(true);
        historyTable.setIntercellSpacing(new Dimension(0, 1));

        // Table setup with scroll pane
        JScrollPane scrollPane = new JScrollPane(historyTable);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons setup
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.add(refreshButton);
        buttonPanel.add(exitButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        refreshButton.addActionListener(e -> refreshHistory());
        exitButton.addActionListener(e -> exit());
    }

    private void refreshHistory() {
        DefaultTableModel tableModel = (DefaultTableModel) historyTable.getModel();
        tableModel.setRowCount(0); // Clear existing data

        ArrayList<String[]> data = read();
        
        if (data.isEmpty()) {
            // Create a message label with custom styling
            JLabel emptyLabel = new JLabel("No transfer history available", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 18));
            emptyLabel.setForeground(new Color(128, 128, 128)); // Gray color
            
            // Remove the table and show the message
            remove(historyTable.getParent().getParent()); // Remove ScrollPane
            add(emptyLabel, BorderLayout.CENTER);
            
            // Update the UI
            revalidate();
            repaint();
        } else {
            // If we previously showed the empty message, remove it and add back the table
            if (historyTable.getParent() == null) {
                remove(((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER));
                JScrollPane scrollPane = new JScrollPane(historyTable);
                add(scrollPane, BorderLayout.CENTER);
            }
            
            // Add the data to the table
            for (String[] row : data) {
                tableModel.addRow(row);
            }
            
            // Update the UI
            revalidate();
            repaint();
        }
    }

    private void exit() {
        timeUpdateTimer.stop(); // Stop the timer before disposing
        dispose();
        SwingUtilities.invokeLater(() -> new ApplicationGUI().setVisible(true));
    }

    @SuppressWarnings("finally")
    public static ArrayList<String[]> read() {
        String folderPath = "C:\\Users\\" + System.getProperty("user.name") + "\\OneDrive\\Documents\\LetsShare";
        ArrayList<String[]> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(folderPath + "\\logs.txt"))) {
            String line;
            String[] log;
            while ((line = reader.readLine()) != null) {
                log = line.split("  ");
                data.add(log);
            }
        } catch (IOException e) {
            System.err.println("Error reading log: " + e.getMessage());
        } finally {
            return data;
        }
    }

    @Override
    public void dispose() {
        timeUpdateTimer.stop();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(History::new);
    }
}