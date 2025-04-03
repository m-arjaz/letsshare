package tester;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ApplicationGUI extends BaseWindow {
    private final JButton sendButton;
    private final JButton receiveButton;
    private final JButton historyButton;

    public ApplicationGUI() {
        super(AppConstants.APP_TITLE, AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize components
        sendButton = createButton("Send");
        receiveButton = createButton("Receive");
        historyButton = createButton("History");

        setupUI();
        setupListeners();
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(AppConstants.DEFAULT_FONT);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(150, 40));
        return button;
    }

    private void setupUI() {
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title Panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel(AppConstants.APP_TITLE);
        titleLabel.setFont(AppConstants.TITLE_FONT);
        titlePanel.add(titleLabel);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonsPanel.add(sendButton);
        buttonsPanel.add(receiveButton);

        // History Panel
        JPanel historyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        historyPanel.add(historyButton);

        // Add panels to main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(buttonsPanel, BorderLayout.CENTER);
        mainPanel.add(historyPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Pack and center
        pack();
        setLocationRelativeTo(null);
    }

    private void setupListeners() {
        sendButton.addActionListener(e -> openSendWindow());
        receiveButton.addActionListener(e -> openReceiveWindow());
        historyButton.addActionListener(e -> openHistoryWindow());
    }

    private void openSendWindow() {
        dispose();
        SwingUtilities.invokeLater(Send::new);
    }

    private void openReceiveWindow() {
        dispose();
        SwingUtilities.invokeLater(Receive::new);
    }

    private void openHistoryWindow() {
        dispose();
        SwingUtilities.invokeLater(History::new);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Custom modifications to UI defaults
                UIManager.put("Button.focusPainted", false);
                UIManager.put("Button.margin", new Insets(5, 10, 5, 10));
                
                new ApplicationGUI().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}