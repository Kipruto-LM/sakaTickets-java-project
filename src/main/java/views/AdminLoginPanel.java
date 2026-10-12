// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.AccountManager;          // Checks logins and creates accounts
import java.awt.BorderLayout;               // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                      // An RGB colour
import java.awt.Dimension;                  // A width and height
import java.awt.GridLayout;                 // Layout with equal-sized cells
import java.awt.GridBagConstraints;         // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;              // A flexible grid layout (used for the forms)
import java.awt.Insets;                     // Spacing around an item
import database.DatabaseException;          // Thrown if saving to the database fails
import java.util.Optional;                  // A box that may or may not hold a User
import java.util.function.Consumer;         // An object holding an action that takes one value
import javax.swing.BorderFactory;           // Helper for making borders
import javax.swing.JButton;                 // A clickable button
import javax.swing.JLabel;                  // A piece of text
import javax.swing.JPanel;                  // A container
import javax.swing.JPasswordField;          // A text box that hides what you type
import javax.swing.JTabbedPane;             // A panel with clickable tabs
import javax.swing.JTextField;              // A one-line text box
import javax.swing.border.EmptyBorder;      // Invisible padding
import models.User;                         // A host account

/**
 * Sign-in and sign-up screen for event hosts.
 *
 * Layout: a header (logo + "Back to home"), then two columns:
 *   left  = a picture and a short pitch for hosts,
 *   right = a card with two tabs: "Sign In" and "Create Host Account".
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel.
 * - MVC: the View passes the username and password to the AccountManager (Controller) and
 *   never handles password hashing itself.
 * - Callbacks: on a successful login, the logged-in User object goes to loginSuccess
 *   (a Consumer<User>). MainFrame then opens that host's dashboard.
 */
public class AdminLoginPanel extends JPanel {
    private final AccountManager accountManager;    // The controller that checks/creates accounts
    private final Consumer<User> loginSuccess;      // Callback run with the User after logging in
    // Sign-in form fields. The number (20) is how many characters wide the box starts.
    private final JTextField loginUsername = new JTextField(20);
    private final JPasswordField loginPassword = new JPasswordField(20);
    // Registration form fields.
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);
    // Red error messages under each form. " " (a space) keeps their height when empty.
    private final JLabel loginMessage = new JLabel(" ");
    private final JLabel registrationMessage = new JLabel(" ");

    // Constructor: builds the header and the two-column layout.
    public AdminLoginPanel(AccountManager accountManager, Consumer<User> loginSuccess, Runnable backToHome) {
        this.accountManager = accountManager;
        this.loginSuccess = loginSuccess;
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(25, 32, 24, 32));     // Page padding

        // --- Header: logo text on the left, "Back to home" on the right ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 3));    // 2 rows: name, caption
        brand.setOpaque(false);
        JLabel brandTitle = new JLabel("SakaTickets");
        ViewStyles.styleHeading(brandTitle, 20f);
        JLabel brandCaption = new JLabel("HOST STUDIO   /   ACCOUNT ACCESS");
        brandCaption.setForeground(new Color(0x00, 0xE5, 0xC4));  // Teal
        brandCaption.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 9f));
        brand.add(brandTitle);
        brand.add(brandCaption);
        header.add(brand, BorderLayout.WEST);
        JButton backButton = new JButton("Back to home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(action -> backToHome.run());   // Callback to MainFrame
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // --- The tabbed card: one tab per form ---
        JTabbedPane accountTabs = new JTabbedPane();
        accountTabs.addTab("Sign In", buildLoginForm());
        accountTabs.addTab("Create Host Account", buildRegistrationForm());
        accountTabs.putClientProperty("JTabbedPane.tabType", "card");   // FlatLaf: card-style tabs
        accountTabs.setPreferredSize(new Dimension(500, 480));

        // Wrap the tabs in a card with an outline and padding.
        JPanel accountCard = new JPanel(new BorderLayout());
        accountCard.setBackground(new Color(0x17, 0x1B, 0x24));
        accountCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(13, 13, 13, 13)));
        accountCard.add(accountTabs, BorderLayout.CENTER);

        // Two equal columns: intro on the left, the form card on the right.
        JPanel layout = new JPanel(new GridLayout(1, 2, 22, 0));
        layout.setOpaque(false);
        layout.add(buildHostIntroduction());
        layout.add(accountCard);
        add(layout, BorderLayout.CENTER);
    }

    // Builds the left column: a picture and some text aimed at event hosts.
    private JPanel buildHostIntroduction() {
        JPanel intro = new JPanel(new BorderLayout(0, 14));
        intro.setOpaque(false);
        intro.add(EventVisuals.hostChoice(255, 12), BorderLayout.NORTH);   // Picture on top

        // The text, stacked vertically.
        JPanel copy = new JPanel();
        copy.setLayout(new javax.swing.BoxLayout(copy, javax.swing.BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel eyebrow = new JLabel("FOR THE PEOPLE WHO MAKE IT HAPPEN");
        eyebrow.setForeground(new Color(0x00, 0xB4, 0xFF));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 10f));
        // Note: a plain JLabel ignores "\n", so this shows on one line.
        JLabel title = new JLabel("Turn your idea into\na city moment.");
        ViewStyles.styleHeading(title, 27f);
        // <html> lets the long sentence wrap onto more than one line.
        JLabel description = new JLabel("<html>Create an event, build ticket tiers, and keep every sale in view.</html>");
        ViewStyles.styleSecondaryText(description, 14f);
        JLabel note = new JLabel("One host account. Your event workspace.");
        note.setForeground(new Color(0x00, 0xE5, 0xC4));
        note.setFont(ViewStyles.BODY_FONT.deriveFont(java.awt.Font.BOLD, 12f));
        copy.add(eyebrow);
        copy.add(javax.swing.Box.createVerticalStrut(9));     // Gaps in pixels between lines
        copy.add(title);
        copy.add(javax.swing.Box.createVerticalStrut(9));
        copy.add(description);
        copy.add(javax.swing.Box.createVerticalStrut(17));
        copy.add(note);
        intro.add(copy, BorderLayout.CENTER);
        return intro;
    }

    // Builds the "Sign In" tab: username, password, an error line and a button.
    private JPanel buildLoginForm() {
        JPanel form = createForm("Host Sign In");
        styleInputs(loginUsername, loginPassword);
        addField(form, "Username", loginUsername, 1);         // Row 1
        addField(form, "Password", loginPassword, 2);         // Row 2
        styleMessage(loginMessage);
        addMessage(form, loginMessage, 3);                    // Row 3: error message
        JButton submit = new JButton("Sign In");
        ViewStyles.stylePrimaryButton(submit);
        submit.addActionListener(action -> authenticate());   // Clicking runs authenticate() below
        addAction(form, submit, 4);                           // Row 4: button
        return form;
    }

    // Builds the "Create Host Account" tab: five fields, an error line and a button.
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
        submit.addActionListener(action -> register());       // Clicking runs register() below
        addAction(form, submit, 7);
        return form;
    }

    // Makes an empty form panel with a heading in row 0. Both forms start from this.
    private JPanel createForm(String title) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(0x17, 0x1B, 0x24));
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)), new EmptyBorder(24, 28, 24, 28)));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;                                // Column 0
        constraints.gridy = 0;                                // Row 0
        constraints.gridwidth = 2;                            // Spans both columns
        constraints.anchor = GridBagConstraints.WEST;         // Left-aligned
        constraints.insets = new Insets(0, 0, 18, 0);         // 18px gap below
        JLabel heading = new JLabel(title);
        ViewStyles.styleHeading(heading, 21f);
        form.add(heading, constraints);
        return form;
    }

    // Adds one form row: the label in column 0 and the input box in column 1.
    private void addField(JPanel form, String label, java.awt.Component input, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.insets = new Insets(0, 0, 11, 14);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 13f);
        form.add(fieldLabel, constraints);
        // Same constraints object, changed for column 1: the input stretches to fill the width.
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 11, 0);
        form.add(input, constraints);
    }

    // Adds the submit button in column 1, aligned to the right.
    private void addAction(JPanel form, JButton button, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(8, 0, 0, 0);
        form.add(button, constraints);
    }

    // Makes an error label red and bold.
    private void styleMessage(JLabel message) {
        message.setForeground(new Color(0xFF, 0x4D, 0x6A));
        message.setFont(ViewStyles.BODY_FONT.deriveFont(java.awt.Font.BOLD, 12f));
    }

    // Adds the error label in column 1 of the given row.
    private void addMessage(JPanel form, JLabel message, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 7, 0);
        form.add(message, constraints);
    }

    // Applies the shared text-box style to each field given.
    // "JTextField... fields" (varargs) means you can pass any number of fields.
    private void styleInputs(JTextField... fields) {
        for (JTextField field : fields) {
            ViewStyles.styleInput(field);
        }
    }

    // Asks the controller to check the login. Optional<User> holds a User only when the login works.
    private void authenticate() {
        // Password boxes give back char[] (an array of characters); new String(...) turns it into normal text.
        String password = new String(loginPassword.getPassword());
        Optional<User> user = accountManager.authenticate(loginUsername.getText(), password);
        if (user.isPresent()) {                             // Login worked
            loginPassword.setText("");                      // Clear the password box
            loginMessage.setText(" ");                      // Clear any old error
            loginSuccess.accept(user.get());                // Tell MainFrame: open the dashboard
        } else {                                            // Wrong username or password
            loginMessage.setText("Username or password is incorrect.");
            loginPassword.setText("");
        }
    }

    // Asks the controller to create an account. Errors come back as exceptions with friendly messages.
    private void register() {
        String password = new String(passwordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());
        // Both password boxes must match.
        if (!password.equals(confirmation)) {
            registrationMessage.setText("The passwords do not match.");
            return;
        }
        try {
            User user = accountManager.register(nameField.getText(), emailField.getText(),
                    usernameField.getText(), password);
            clearRegistrationForm();
            registrationMessage.setText(" ");
            loginSuccess.accept(user);                          // New hosts go straight to their dashboard
        } catch (IllegalArgumentException | DatabaseException exception) {
            // Either a rule was broken (e.g. "Enter a valid email address.") or saving failed:
            // show the exception's message under the form.
            registrationMessage.setText(exception.getMessage());
        }
    }

    // Empties every registration box.
    private void clearRegistrationForm() {
        nameField.setText("");
        emailField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

}
