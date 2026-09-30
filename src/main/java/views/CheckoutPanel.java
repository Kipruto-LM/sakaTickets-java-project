package views;

import controllers.EventManager;
import controllers.OrderManager;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import models.Booking;
import models.Event;

public class CheckoutPanel extends JPanel {
    private final EventManager eventManager;
    private final OrderManager orderManager;
    private final Runnable returnToCatalog;
    private final JLabel eventLabel = new JLabel();
    private final JLabel totalLabel = new JLabel("Total: KSh 0.00");
    private final JTextField nameField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JTextField quantityField = new JTextField("1", 22);
    private Event selectedEvent;

    public CheckoutPanel(EventManager eventManager, OrderManager orderManager, Runnable returnToCatalog) {
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.returnToCatalog = returnToCatalog;
        buildForm();
    }

    public void setEvent(String eventId) {
        selectedEvent = eventManager.getEventById(eventId);
        eventLabel.setText(selectedEvent == null ? "Event unavailable" : selectedEvent.getTitle());
        updateTotal();
    }

    private void buildForm() {
        setBorder(BorderFactory.createEmptyBorder(32, 80, 32, 80));
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        addField("Event", eventLabel, constraints, 0);
        addField("Guest Name", nameField, constraints, 1);
        addField("Guest Email", emailField, constraints, 2);
        addField("Ticket Quantity", quantityField, constraints, 3);

        quantityField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { updateTotal(); }
            public void removeUpdate(DocumentEvent event) { updateTotal(); }
            public void changedUpdate(DocumentEvent event) { updateTotal(); }
        });
        JButton submitButton = new JButton("Confirm Booking");
        submitButton.addActionListener(event -> submitBooking());
        constraints.gridy = 4;
        add(totalLabel, constraints);
        constraints.gridy = 5;
        add(submitButton, constraints);
        JButton backButton = new JButton("Back to Catalog");
        backButton.addActionListener(event -> returnToCatalog.run());
        constraints.gridy = 6;
        add(backButton, constraints);
    }

    private void addField(String label, JTextField field, GridBagConstraints constraints, int row) {
        addField(label, (JLabel) null, constraints, row);
        constraints.gridx = 1;
        add(field, constraints);
        constraints.gridx = 0;
    }

    private void addField(String label, JLabel value, GridBagConstraints constraints, int row) {
        constraints.gridy = row;
        constraints.gridx = 0;
        add(new JLabel(label), constraints);
        if (value != null) {
            constraints.gridx = 1;
            add(value, constraints);
            constraints.gridx = 0;
        }
    }

    private void updateTotal() {
        int quantity = parseQuantity();
        double total = selectedEvent == null ? 0 : selectedEvent.getTicketPrice() * quantity;
        totalLabel.setText(String.format("Total: KSh %.2f", total));
    }

    private int parseQuantity() {
        try {
            return Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void submitBooking() {
        int quantity = parseQuantity();
        if (selectedEvent == null || nameField.getText().isBlank() || emailField.getText().isBlank()
                || quantity <= 0) {
            JOptionPane.showMessageDialog(this, "Enter valid guest details and quantity.", "Invalid booking",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!selectedEvent.purchaseTickets(quantity)) {
            JOptionPane.showMessageDialog(this, "Not enough seats are available.", "Sold out",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Booking booking = orderManager.createBooking(selectedEvent.getEventId(), nameField.getText().trim(),
                emailField.getText().trim(), quantity, selectedEvent.getTicketPrice() * quantity);
        JOptionPane.showMessageDialog(this, "Booking confirmed. Receipt: " + booking.getBookingId(),
                "Booking successful", JOptionPane.INFORMATION_MESSAGE);
        clearForm();
        returnToCatalog.run();
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        quantityField.setText("1");
    }
}