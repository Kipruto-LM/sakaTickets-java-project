package views;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class WelcomePanel extends JPanel {
    private static final Color BACKGROUND = new Color(19, 27, 35);
    private static final Color ACCENT = new Color(67, 207, 155);
    private static final Color FOREGROUND = new Color(239, 244, 246);
    private static final Color MUTED = new Color(164, 179, 187);

    public WelcomePanel(Runnable buyTicketAction, Runnable hostEventAction) {
        setLayout(new GridBagLayout());
        setBackground(BACKGROUND);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 14, 0);
        JLabel title = new JLabel("Welcome to SakaTickets");
        title.setForeground(FOREGROUND);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        content.add(title, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 32, 0);
        JLabel subtitle = new JLabel("Find your next great experience.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 16f));
        content.add(subtitle, constraints);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.insets = new Insets(0, 8, 0, 8);

        JButton buyButton = createButton("Buy a Ticket", ACCENT, BACKGROUND);
        buyButton.addActionListener(event -> buyTicketAction.run());
        actionConstraints.ipadx = 18;
        actionConstraints.ipady = 12;
        actions.add(buyButton, actionConstraints);

        JButton hostButton = createButton("Host an Event", new Color(39, 53, 63), FOREGROUND);
        hostButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(76, 96, 106)),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        hostButton.addActionListener(event -> hostEventAction.run());
        actionConstraints.ipadx = 0;
        actionConstraints.ipady = 0;
        actions.add(hostButton, actionConstraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        content.add(actions, constraints);
        add(content);
    }

    private JButton createButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }
}