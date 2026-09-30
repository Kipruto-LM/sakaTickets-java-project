package views;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class AdminLoginPanel extends JPanel {
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final Runnable loginSuccess;

    public AdminLoginPanel(Runnable loginSuccess, Runnable backToHome) {
        this.loginSuccess = loginSuccess;
        setLayout(new GridBagLayout());
        setBackground(ViewStyles.BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ViewStyles.OUTLINE),
            new EmptyBorder(28, 32, 28, 32)));
        ViewStyles.styleInput(usernameField);
        ViewStyles.styleInput(passwordField);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 20, 0);
        JLabel title = new JLabel("Host an Event");
        ViewStyles.styleHeading(title, 24f);
        form.add(title, constraints);

        constraints.gridwidth = 1;
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 12, 14);
        JLabel usernameLabel = new JLabel("Username");
        ViewStyles.styleSecondaryText(usernameLabel, 14f);
        form.add(usernameLabel, constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 12, 0);
        form.add(usernameField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 18, 14);
        JLabel passwordLabel = new JLabel("Password");
        ViewStyles.styleSecondaryText(passwordLabel, 14f);
        form.add(passwordLabel, constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 18, 0);
        form.add(passwordField, constraints);

        JButton loginButton = new JButton("Login");
        ViewStyles.stylePrimaryButton(loginButton);
        loginButton.addActionListener(event -> authenticate());
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 8);
        form.add(loginButton, constraints);

        JButton backButton = new JButton("Back to Home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(event -> backToHome.run());
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        form.add(backButton, constraints);
        add(form);
    }

    private void authenticate() {
        char[] password = passwordField.getPassword();
        boolean authenticated = "Talel".equals(usernameField.getText().trim())
                && Arrays.equals(password, "admin123".toCharArray());
        Arrays.fill(password, '\0');
        if (authenticated) {
            passwordField.setText("");
            loginSuccess.run();
        } else {
            JOptionPane.showMessageDialog(this, "Username or password is incorrect.",
                    "Login failed", JOptionPane.WARNING_MESSAGE);
            passwordField.setText("");
        }
    }
}