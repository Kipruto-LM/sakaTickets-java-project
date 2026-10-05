package views;

import controllers.EventManager;
import controllers.OrderManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import models.Booking;
import models.Event;
import models.TicketTier;

public class CheckoutPanel extends JPanel {
    private final EventManager eventManager;
    private final OrderManager orderManager;
    private final Runnable returnToCatalog;
    private final JLabel titleLabel = new JLabel("Select an event");
    private final JLabel dateLabel = new JLabel("Date");
    private final JLabel venueLabel = new JLabel("Venue");
    private final JLabel priceLabel = new JLabel("Price per ticket");
    private final JLabel availabilityLabel = new JLabel("Select a ticket tier");
    private final JLabel totalLabel = new JLabel("Total: KSh 0.00");
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JComboBox<TicketTier> tierSelector = new JComboBox<>();
    private final JPanel eventImage = new JPanel(new BorderLayout());
    private final Consumer<Booking> bookingConfirmed;
    private JButton confirmButton;
    private Event selectedEvent;
    private TicketTier selectedTier;

    public CheckoutPanel(EventManager eventManager, OrderManager orderManager, Runnable returnToCatalog) {
        this(eventManager, orderManager, returnToCatalog, booking -> {
            JOptionPane.showMessageDialog(null, "Purchase confirmed. Receipt: " + booking.getBookingId(),
                    "Booking successful", JOptionPane.INFORMATION_MESSAGE);
            returnToCatalog.run();
        });
    }

    public CheckoutPanel(EventManager eventManager, OrderManager orderManager, Runnable returnToCatalog,
            Consumer<Booking> bookingConfirmed) {
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.returnToCatalog = returnToCatalog;
        this.bookingConfirmed = bookingConfirmed;
        buildView();
    }

    public void setEvent(String eventId) {
        setEvent(eventManager.getEventById(eventId));
    }

    public void setEvent(Event event) {
        selectedEvent = event;
        if (selectedEvent == null) {
            titleLabel.setText("Event unavailable");
            dateLabel.setText("");
            venueLabel.setText("");
            priceLabel.setText("");
            tierSelector.removeAllItems();
            selectedTier = null;
            quantitySpinner.setModel(new SpinnerNumberModel(1, 1, 1, 1));
            if (confirmButton != null) confirmButton.setEnabled(false);
            updateTotal();
            return;
        }

        titleLabel.setText(selectedEvent.getTitle());
        dateLabel.setText(selectedEvent.getDate());
        venueLabel.setText(selectedEvent.getVenue());
        eventImage.removeAll();
        eventImage.add(EventVisuals.event(selectedEvent, 156, 9), BorderLayout.CENTER);
        eventImage.revalidate();
        eventImage.repaint();
        tierSelector.removeAllItems();
        selectedEvent.getTicketTiers().forEach(tierSelector::addItem);
        if (tierSelector.getItemCount() > 0) {
            tierSelector.setSelectedIndex(0);
        }
        updateTierSelection();
    }

    private void buildView() {
        setLayout(new GridBagLayout());
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setOpaque(false);
        content.add(buildEventDetails(), BorderLayout.WEST);
        content.add(buildPurchaseForm(), BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        add(content, constraints);
    }

    private JPanel buildEventDetails() {
        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(ViewStyles.SURFACE);
        details.putClientProperty("FlatLaf.style", "arc: 15");
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                new EmptyBorder(26, 24, 26, 24)));
        details.setPreferredSize(new java.awt.Dimension(330, 470));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        eventImage.setBackground(ViewStyles.INPUT);
        eventImage.setPreferredSize(new java.awt.Dimension(280, 156));
        eventImage.putClientProperty("FlatLaf.style", "arc: 9");
        constraints.insets = new Insets(0, 0, 16, 0);
        details.add(eventImage, constraints);

        ViewStyles.styleHeading(titleLabel, 24f);
        titleLabel.setBorder(new EmptyBorder(0, 0, 18, 0));
        constraints.gridy++;
        details.add(titleLabel, constraints);

        constraints.gridy++;
        addDetail(details, dateLabel, constraints, 12);
        constraints.gridy++;
        addDetail(details, venueLabel, constraints, 12);
        constraints.gridy++;
        priceLabel.setForeground(ViewStyles.FOREGROUND);
        priceLabel.setFont(priceLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        constraints.insets = new Insets(8, 0, 0, 0);
        details.add(priceLabel, constraints);
        return details;
    }

    private void addDetail(JPanel panel, JLabel label, GridBagConstraints constraints, int bottomPadding) {
        ViewStyles.styleSecondaryText(label, 15f);
        constraints.insets = new Insets(0, 0, bottomPadding, 0);
        panel.add(label, constraints);
    }

    private JPanel buildPurchaseForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.SURFACE);
        form.putClientProperty("FlatLaf.style", "arc: 15");
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                new EmptyBorder(26, 26, 26, 26)));

        ViewStyles.styleInput(nameField);
        ViewStyles.styleInput(emailField);
        if (quantitySpinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            ViewStyles.styleInput(editor.getTextField());
        }
        quantitySpinner.addChangeListener(event -> updateTotal());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 20, 0);
        JLabel heading = new JLabel("Guest details");
        ViewStyles.styleHeading(heading, 21f);
        form.add(heading, constraints);

        constraints.gridwidth = 1;
        addField(form, "Guest Name", nameField, constraints, 1);
        addField(form, "Guest Email", emailField, constraints, 3);
        addField(form, "Ticket Tier", tierSelector, constraints, 5);
        addField(form, "Ticket Quantity", quantitySpinner, constraints, 7);

        tierSelector.setRenderer((list, tier, index, selected, focus) -> {
            JLabel item = new JLabel(tier == null ? "Select a ticket tier"
                : String.format("%s - KES %,.2f (%d left)", tier.getTierName(), tier.getPrice(), tier.getAvailableSeats()));
            item.setOpaque(true);
            item.setForeground(ViewStyles.FOREGROUND);
            item.setBackground(selected ? ViewStyles.ACCENT : ViewStyles.INPUT);
            item.setBorder(new EmptyBorder(6, 8, 6, 8));
            return item;
        });
        tierSelector.addActionListener(event -> updateTierSelection());

        totalLabel.setForeground(ViewStyles.FOREGROUND);
        totalLabel.setFont(totalLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        constraints.gridx = 0;
        ViewStyles.styleSecondaryText(availabilityLabel, 13f);
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(2, 0, 8, 0);
        form.add(availabilityLabel, constraints);

        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(12, 0, 18, 0);
        form.add(totalLabel, constraints);

        JButton cancelButton = new JButton("Cancel");
        ViewStyles.styleSecondaryButton(cancelButton);
        cancelButton.addActionListener(event -> returnToCatalog.run());

        confirmButton = new JButton("Confirm Purchase");
        ViewStyles.stylePrimaryButton(confirmButton);
        confirmButton.addActionListener(event -> submitBooking());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(confirmButton);
        constraints.gridy = 11;
        constraints.insets = new Insets(0, 0, 0, 0);
        form.add(actions, constraints);
        return form;
    }

    private void addField(JPanel form, String label, java.awt.Component input,
            GridBagConstraints constraints, int row) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 0, 7, 0);
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 14f);
        form.add(fieldLabel, constraints);

        constraints.gridy++;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 15, 0);
        form.add(input, constraints);
        constraints.gridwidth = 1;
    }

    private void updateTotal() {
        int quantity = (Integer) quantitySpinner.getValue();
        double total = selectedTier == null ? 0 : selectedTier.getPrice() * quantity;
        totalLabel.setText(String.format("Total: KSh %,.2f", total));
    }

    private void updateTierSelection() {
        selectedTier = (TicketTier) tierSelector.getSelectedItem();
        if (selectedTier == null) {
            priceLabel.setText("No ticket tiers available");
            quantitySpinner.setModel(new SpinnerNumberModel(1, 1, 1, 1));
            if (confirmButton != null) confirmButton.setEnabled(false);
            availabilityLabel.setText("Sales Closed");
            updateTotal();
            return;
        }
        priceLabel.setText(String.format("%s - KES %,.2f per ticket", selectedTier.getTierName(), selectedTier.getPrice()));
        int maximumSeats = Math.max(1, selectedTier.getAvailableSeats());
        int quantity = Math.min((Integer) quantitySpinner.getValue(), maximumSeats);
        quantitySpinner.setModel(new SpinnerNumberModel(quantity, 1, maximumSeats, 1));
        boolean dateIsOpen = selectedEvent != null && selectedEvent.canPurchaseOn(LocalDate.now());
        boolean tierHasSeats = selectedTier.getAvailableSeats() > 0;
        availabilityLabel.setText(!dateIsOpen ? "Sales Closed"
                : tierHasSeats ? selectedTier.getAvailableSeats() + " tickets available" : "This tier is sold out");
        if (confirmButton != null) confirmButton.setEnabled(dateIsOpen && tierHasSeats);
        updateTotal();
    }

    private void submitBooking() {
        int quantity = (Integer) quantitySpinner.getValue();
        if (selectedEvent == null || selectedTier == null || nameField.getText().isBlank()
            || emailField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter valid guest details before confirming.",
                    "Invalid booking", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!selectedEvent.purchase(selectedTier.getTierName(), quantity)) {
            JOptionPane.showMessageDialog(this, "Sales are closed or this ticket tier is sold out.", "Purchase unavailable",
                    JOptionPane.WARNING_MESSAGE);
            setEvent(selectedEvent);
            return;
        }

        Booking booking = orderManager.createBooking(selectedEvent.getEventId(), nameField.getText().trim(),
            emailField.getText().trim(), selectedTier.getTierName(), quantity, selectedTier.getPrice() * quantity);
        clearForm();
        bookingConfirmed.accept(booking);
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        quantitySpinner.setValue(1);
    }
}