package views;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class WelcomePanel extends JPanel {
    public WelcomePanel(Runnable buyTicketAction, Runnable hostEventAction) {
        setLayout(new GridBagLayout());
        setBackground(ViewStyles.BACKGROUND);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 12, 0);
        JLabel title = new JLabel("SakaTickets");
        ViewStyles.styleHeading(title, 28f);
        content.add(title, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 30, 0);
        JLabel subtitle = new JLabel("Your gateway to the best events");
        ViewStyles.styleSecondaryText(subtitle, 16f);
        content.add(subtitle, constraints);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.insets = new Insets(0, 8, 0, 8);

        JButton buyButton = createButton("Buy a Ticket");
        buyButton.addActionListener(event -> buyTicketAction.run());
        actions.add(buyButton, actionConstraints);

        JButton hostButton = createButton("Host an Event");
        hostButton.addActionListener(event -> hostEventAction.run());
        actionConstraints.gridx = 1;
        actions.add(hostButton, actionConstraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        content.add(actions, constraints);
        add(content);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 50));
        ViewStyles.stylePrimaryButton(button);
        return button;
    }
}