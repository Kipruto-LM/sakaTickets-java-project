package views;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AdminDashboardPanel extends JPanel {
    public AdminDashboardPanel(Runnable backToHome) {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 18, 0);

        JLabel welcomeLabel = new JLabel("Welcome Admin");
        welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(Font.BOLD, 28f));
        add(welcomeLabel, constraints);

        JButton backButton = new JButton("Back to Home");
        backButton.addActionListener(event -> backToHome.run());
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(backButton, constraints);
    }
}