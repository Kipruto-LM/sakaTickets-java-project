package views;

import controllers.EventManager;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import models.Event;

public class AdminDashboardPanel extends JPanel {
    private final EventManager eventManager;
    private final JTextField titleField = new JTextField(22);
    private final JTextField dateField = new JTextField(22);
    private final JTextField venueField = new JTextField(22);
    private final JSpinner seatsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 10));
    private final JSpinner priceSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000000.0, 100.0));

    public AdminDashboardPanel(EventManager eventManager, Runnable backToHome) {
        this.eventManager = eventManager;
        setLayout(new GridBagLayout());
        setBackground(ViewStyles.BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.SURFACE);
        form.putClientProperty("FlatLaf.style", "arc: 15");
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                new EmptyBorder(26, 30, 24, 30)));

        ViewStyles.styleInput(titleField);
        ViewStyles.styleInput(dateField);
        ViewStyles.styleInput(venueField);
        styleSpinner(seatsSpinner);
        styleSpinner(priceSpinner);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 20, 0);
        JLabel heading = new JLabel("Create New Event");
        ViewStyles.styleHeading(heading, 24f);
        form.add(heading, constraints);

        constraints.gridwidth = 1;
        addField(form, "Title", titleField, constraints, 1);
        addField(form, "Date (YYYY-MM-DD)", dateField, constraints, 2);
        addField(form, "Venue", venueField, constraints, 3);
        addField(form, "Total Seats", seatsSpinner, constraints, 4);
        addField(form, "Ticket Price (KSh)", priceSpinner, constraints, 5);

        JButton logoutButton = new JButton("Logout");
        ViewStyles.styleSecondaryButton(logoutButton);
        logoutButton.addActionListener(event -> backToHome.run());
        JButton saveButton = new JButton("Save Event");
        ViewStyles.stylePrimaryButton(saveButton);
        saveButton.addActionListener(event -> saveEvent());

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(logoutButton);
        actions.add(saveButton);
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(18, 0, 0, 0);
        form.add(actions, constraints);

        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.gridy = 0;
        outer.anchor = GridBagConstraints.CENTER;
        add(form, outer);
    }

    private void addField(JPanel form, String label, java.awt.Component input,
            GridBagConstraints constraints, int row) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 12, 16);
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 14f);
        form.add(fieldLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        form.add(input, constraints);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setOpaque(true);
        spinner.setBackground(ViewStyles.INPUT);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            ViewStyles.styleInput(editor.getTextField());
        }
    }

    private void saveEvent() {
        String title = titleField.getText().trim();
        String date = dateField.getText().trim();
        String venue = venueField.getText().trim();
        if (title.isEmpty() || date.isEmpty() || venue.isEmpty()) {
            showValidationError("Complete the title, date, and venue fields.");
            return;
        }
        try {
            LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException exception) {
            showValidationError("Enter the date in YYYY-MM-DD format.");
            return;
        }

        Event createdEvent = new Event("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                title, date, venue, (Integer) seatsSpinner.getValue(), (Double) priceSpinner.getValue());
        eventManager.addEvent(createdEvent);
        JOptionPane.showMessageDialog(this, "Event saved successfully.", "Event Created",
                JOptionPane.INFORMATION_MESSAGE);
        clearForm();
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check Event Details", JOptionPane.WARNING_MESSAGE);
    }

    private void clearForm() {
        titleField.setText("");
        dateField.setText("");
        venueField.setText("");
        seatsSpinner.setValue(50);
        priceSpinner.setValue(0.0);
        titleField.requestFocusInWindow();
    }
}