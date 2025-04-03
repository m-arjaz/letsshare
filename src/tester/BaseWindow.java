package tester;

import javax.swing.JFrame;

public abstract class BaseWindow extends JFrame {
    protected BaseWindow(String title, int width, int height) {
        super(title);
        setSize(width, height);
        setResizable(false);
        setLocationRelativeTo(null); // Center window
    }
}