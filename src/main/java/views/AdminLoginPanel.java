package views;

import controllers.AccountManager;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import models.User;

public class AdminLoginPanel extends JPanel {
    private final AccountManager accountManager;
    private final Consumer<User> loginSuccess;
    private final JTextField loginUsername = new JTextField(20);
    private final JPasswordField loginPassword = new JPasswordField(20);
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);

    public AdminLoginPanel(AccountManager accountManager, Consumer<User> loginSuccess, Runnable backToHome) {
        this.accountManager = accountManager;
        this.loginSuccess = loginSuccess;
        setLayout(new BorderLayout(0, 12));
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 22, 24));

        JTabbedPane accountTabs = new JTabbedPane();
        accountTabs.addTab("Sign In", buildLoginForm());
        accountTabs.addTab("Create Host Account", buildRegistrationForm());
        add(accountTabs, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(action -> backToHome.run());
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(backButton);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildLoginForm() {
        JPanel form = createForm("Host Sign In");
        styleInputs(loginUsername, loginPassword);
        addField(form, "Username", loginUsername, 1);
        addField(form, "Password", loginPassword, 2);
        JButton submit = new JButton("Sign In");
        ViewStyles.stylePrimaryButton(submit);
        submit.addActionListener(action -> authenticate());
        addAction(form, submit, 3);
        return form;
    }

    private JPanel buildRegistrationForm() {
        JPanel form = createForm("Create Your Host Account");
        styleInputs(nameField, emailField, usernameField, passwordField, confirmPasswordField);
        addField(form, "Name", nameField, 1);
        addField(form, "Email", emailField, 2);
        addField(form, "Username", usernameField, 3);
        addField(form, "Password (8+ characters)", passwordField, 4);
        addField(form, "Confirm password", confirmPasswordField, 5);
        JButton submit = new JButton("Create Account");
        ViewStyles.stylePrimaryButton(submit);
        submit.addActionListener(action -> register());
        addAction(form, submit, 6);
        return form;
    }

    private JPanel createForm(String title) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE), new EmptyBorder(24, 28, 24, 28)));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 18, 0);
        JLabel heading = new JLabel(title);
        ViewStyles.styleHeading(heading, 22f);
        form.add(heading, constraints);
        return form;
    }

    private void addField(JPanel form, String label, java.awt.Component input, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.insets = new Insets(0, 0, 11, 14);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 13f);
        form.add(fieldLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 11, 0);
        form.add(input, constraints);
    }

    private void addAction(JPanel form, JButton button, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 0, 0, 0);
        form.add(button, constraints);
    }

    private void styleInputs(JTextField... fields) {
        for (JTextField field : fields) {
            ViewStyles.styleInput(field);
        }
    }

    private void authenticate() {
        char[] password = loginPassword.getPassword();
        try {
            Optional<User> user = accountManager.authenticate(loginUsername.getText(), password);
            if (user.isPresent()) {
                loginPassword.setText("");
                loginSuccess.accept(user.get());
            } else {
                showError("Username or password is incorrect.");
                loginPassword.setText("");
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void register() {
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmPasswordField.getPassword();
        try {
            if (!Arrays.equals(password, confirmation)) {
                showError("The passwords do not match.");
                return;
            }
            User user = accountManager.register(nameField.getText(), emailField.getText(),
                    usernameField.getText(), password);
            clearRegistrationForm();
            loginSuccess.accept(user);
        } catch (IllegalArgumentException | IOException exception) {
            showError(exception.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    private void clearRegistrationForm() {
        nameField.setText("");
        emailField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Host Account", JOptionPane.WARNING_MESSAGE);
    }
}
