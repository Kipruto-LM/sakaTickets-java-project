package views;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

public class MainFrame extends JFrame {
    public MainFrame() {
        // Setup the main window properties
        setTitle("SakaTickets");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        
        // Add a temporary placeholder label so it's not just a blank window
        JLabel welcomeLabel = new JLabel("Welcome to SakaTickets Application Interface", SwingConstants.CENTER);
        add(welcomeLabel, BorderLayout.CENTER);
    }
}