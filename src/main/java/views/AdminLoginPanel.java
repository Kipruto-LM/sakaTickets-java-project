package views;

import controllers.AccountManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
    private final JLabel loginMessage = new JLabel(" ");
    private final JLabel registrationMessage = new JLabel(" ");

    public AdminLoginPanel(AccountManager accountManager, Consumer<User> loginSuccess, Runnable backToHome) {
        this.accountManager = accountManager;
        this.loginSuccess = loginSuccess;
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(25, 32, 24, 32));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 3));
        brand.setOpaque(false);
        JLabel brandTitle = new JLabel("SakaTickets");
        ViewStyles.styleHeading(brandTitle, 20f);
        JLabel brandCaption = new JLabel("HOST STUDIO   /   ACCOUNT ACCESS");
        brandCaption.setForeground(new Color(0x00, 0xE5, 0xC4));
        brandCaption.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 9f));
        brand.add(brandTitle);
        brand.add(brandCaption);
        header.add(brand, BorderLayout.WEST);
        JButton backButton = new JButton("Back to home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(action -> backToHome.run());
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane accountTabs = new JTabbedPane();
        accountTabs.addTab("Sign In", buildLoginForm());
        accountTabs.addTab("Create Host Account", buildRegistrationForm());
        accountTabs.putClientProperty("JTabbedPane.tabType", "card");
        accountTabs.setPreferredSize(new Dimension(500, 480));

        JPanel accountCard = new JPanel(new BorderLayout());
        accountCard.setBackground(new Color(0x17, 0x1B, 0x24));
        accountCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(13, 13, 13, 13)));
        accountCard.add(accountTabs, BorderLayout.CENTER);

        JPanel layout = new JPanel(new GridLayout(1, 2, 22, 0));
        layout.setOpaque(false);
        layout.add(buildHostIntroduction());
        layout.add(accountCard);
        add(layout, BorderLayout.CENTER);
    }

    private JPanel buildHostIntroduction() {
        JPanel intro = new JPanel(new BorderLayout(0, 14));
        intro.setOpaque(false);
        intro.add(EventVisuals.hostChoice(255, 12), BorderLayout.NORTH);

        JPanel copy = new JPanel();
        copy.setLayout(new javax.swing.BoxLayout(copy, javax.swing.BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel eyebrow = new JLabel("FOR THE PEOPLE WHO MAKE IT HAPPEN");
        eyebrow.setForeground(new Color(0x00, 0xB4, 0xFF));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 10f));
        JLabel title = new JLabel("Turn your idea into\na city moment.");
        ViewStyles.styleHeading(title, 27f);
        JLabel description = new JLabel("<html>Create an event, build ticket tiers, and keep every sale in view.</html>");
        ViewStyles.styleSecondaryText(description, 14f);
        JLabel note = new JLabel("One host account. Your event workspace.");
        note.setForeground(new Color(0x00, 0xE5, 0xC4));
        note.setFont(ViewStyles.BODY_FONT.deriveFont(java.awt.Font.BOLD, 12f));
        copy.add(eyebrow);
        copy.add(javax.swing.Box.createVerticalStrut(9));
        copy.add(title);
        copy.add(javax.swing.Box.createVerticalStrut(9));
        copy.add(description);
        copy.add(javax.swing.Box.createVerticalStrut(17));
        copy.add(note);
        intro.add(copy, BorderLayout.CENTER);
        return intro;
    }

    private JPanel buildLoginForm() {
        JPanel form = createForm("Host Sign In");
        styleInputs(loginUsername, loginPassword);
        addField(form, "Username", loginUsername, 1);
        addField(form, "Password", loginPassword, 2);
        styleMessage(loginMessage);
        addMessage(form, loginMessage, 3);
        JButton submit = new JButton("Sign In");
        ViewStyles.stylePrimaryButton(submit);
        submit.addActionListener(action -> authenticate());
        addAction(form, submit, 4);
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
        styleMessage(registrationMessage);
        addMessage(form, registrationMessage, 6);
        JButton submit = new JButton("Create Account");
        ViewStyles.stylePrimaryButton(submit);
        submit.addActionListener(action -> register());
        addAction(form, submit, 7);
        return form;
    }

    private JPanel createForm(String title) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(0x17, 0x1B, 0x24));
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)), new EmptyBorder(24, 28, 24, 28)));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 18, 0);
        JLabel heading = new JLabel(title);
        ViewStyles.styleHeading(heading, 21f);
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

    private void styleMessage(JLabel message) {
        message.setForeground(new Color(0xFF, 0x4D, 0x6A));
        message.setFont(ViewStyles.BODY_FONT.deriveFont(java.awt.Font.BOLD, 12f));
    }

    private void addMessage(JPanel form, JLabel message, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 7, 0);
        form.add(message, constraints);
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
                loginMessage.setText(" ");
                loginSuccess.accept(user.get());
            } else {
                loginMessage.setText("Username or password is incorrect.");
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
                registrationMessage.setText("The passwords do not match.");
                return;
            }
            User user = accountManager.register(nameField.getText(), emailField.getText(),
                    usernameField.getText(), password);
            clearRegistrationForm();
            registrationMessage.setText(" ");
            loginSuccess.accept(user);
        } catch (IllegalArgumentException | IOException exception) {
            registrationMessage.setText(exception.getMessage());
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

}
