package views;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("Event Ticketing System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Welcome to the Event Ticketing System", SwingConstants.CENTER);
        add(titleLabel, BorderLayout.CENTER);
    }
}
