// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.EventManager;            // Looks up events by ID
import controllers.OrderManager;            // Records the booking
import java.awt.BorderLayout;               // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.FlowLayout;                 // Layout that places items in a row
import java.awt.GridBagConstraints;         // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;              // A flexible grid layout
import java.awt.Insets;                     // Spacing around an item
import java.util.function.Consumer;         // An object holding an action that takes one value
import javax.swing.BorderFactory;           // Helper for making borders
import javax.swing.JButton;                 // A clickable button
import javax.swing.JLabel;                  // A piece of text
import javax.swing.JComboBox;               // A drop-down list
import javax.swing.JOptionPane;             // Pop-up message boxes
import javax.swing.JPanel;                  // A container
import javax.swing.JSpinner;                // A number box with up/down arrows
import javax.swing.JTextField;              // A one-line text box
import javax.swing.SpinnerNumberModel;      // The value, minimum, maximum and step for a JSpinner
import javax.swing.border.EmptyBorder;      // Invisible padding
import models.Booking;                      // The purchase record
import models.Event;                        // The event being bought
import models.TicketTier;                   // The chosen ticket type

/**
 * Screen where a guest picks a ticket tier and quantity, enters their details and pays.
 *
 * Layout: two cards side by side.
 *   left  = the event's picture, title, date, venue and the chosen tier's price,
 *   right = the form: name, email, tier drop-down, quantity, total, Cancel / Confirm buttons.
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel.
 * - MVC: the View collects the input, the Event model checks and takes the seats,
 *   and the OrderManager controller records the Booking.
 * - Method overloading: three setEvent() methods (one takes an ID, one an Event object,
 *   and one an Event plus the tier to pre-select).
 * - Composition: the panel's own state is the event and tier currently selected.
 */
public class CheckoutPanel extends JPanel {
    private final EventManager eventManager;
    private final OrderManager orderManager;
    private final Runnable returnToCatalog;           // Callback for the Cancel button
    // Labels on the left card. Their text changes when a different event or tier is chosen.
    private final JLabel titleLabel = new JLabel("Select an event");
    private final JLabel dateLabel = new JLabel("Date");
    private final JLabel venueLabel = new JLabel("Venue");
    private final JLabel priceLabel = new JLabel("Price per ticket");
    private final JLabel availabilityLabel = new JLabel("Select a ticket tier");
    private final JLabel totalLabel = new JLabel("Total: KSh 0.00");
    // Form inputs.
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    // SpinnerNumberModel(value, minimum, maximum, step) - starts at 1, updated per tier later.
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JComboBox<TicketTier> tierSelector = new JComboBox<>();   // Drop-down of TicketTier objects
    private final JPanel eventImage = new JPanel(new BorderLayout());       // Holds the event picture
    private final Consumer<Booking> bookingConfirmed;   // Callback run with the new Booking after paying
    private JButton confirmButton;                      // Kept as a field so it can be enabled/disabled
    private Event selectedEvent;                        // The event being bought (can change)
    private TicketTier selectedTier;                    // The tier currently picked in the drop-down

    // Constructor: stores everything, then builds the screen.
    // bookingConfirmed = what to do after a successful purchase (MainFrame shows the receipt).
    public CheckoutPanel(EventManager eventManager, OrderManager orderManager, Runnable returnToCatalog,
            Consumer<Booking> bookingConfirmed) {
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.returnToCatalog = returnToCatalog;
        this.bookingConfirmed = bookingConfirmed;
        buildView();
    }

    // Overloaded setEvent(): looks up the Event by ID, then calls the version below.
    public void setEvent(String eventId) {
        setEvent(eventManager.getEventById(eventId));
    }

    // Second overload: no tier preference (null), so the first tier with seats is chosen.
    public void setEvent(Event event) {
        setEvent(event, null);
    }

    // Third overload: opens checkout with a particular tier already chosen (used by the event
    // details page). With no tier given, it picks the first tier that still has seats.
    public void setEvent(Event event, String preferredTierName) {
        selectedEvent = event;
        // No event (e.g. it was removed): blank everything and disable buying.
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

        // Show the event's details and picture on the left card.
        titleLabel.setText(selectedEvent.getTitle());
        dateLabel.setText(selectedEvent.getDate());
        venueLabel.setText(selectedEvent.getVenue());
        eventImage.removeAll();
        eventImage.add(EventVisuals.event(selectedEvent, 156, 9), BorderLayout.CENTER);
        eventImage.revalidate();
        eventImage.repaint();
        // Refill the tier drop-down with this event's tiers.
        // "tierSelector::addItem" is a method reference: call addItem(tier) for each tier.
        tierSelector.removeAllItems();
        selectedEvent.getTicketTiers().forEach(tierSelector::addItem);
        if (tierSelector.getItemCount() > 0) {
            tierSelector.setSelectedIndex(0);                 // Default: the first tier
            // Look for a better choice: the requested tier, or else the first one with seats left.
            for (int index = 0; index < tierSelector.getItemCount(); index++) {
                TicketTier tier = tierSelector.getItemAt(index);
                boolean preferred = preferredTierName != null && preferredTierName.equals(tier.getTierName());
                boolean firstAvailable = preferredTierName == null && tier.getAvailableSeats() > 0;
                if (preferred || firstAvailable) {
                    tierSelector.setSelectedIndex(index);
                    break;                                    // Stop at the first match
                }
            }
        }
        updateTierSelection();                                // Update price, seats and total
    }

    // Builds the page: the two cards side by side, centred. Called once from the constructor.
    private void buildView() {
        setLayout(new GridBagLayout());
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel content = new JPanel(new BorderLayout(18, 0));     // 18px gap between the cards
        content.setOpaque(false);
        content.add(buildEventDetails(), BorderLayout.WEST);      // Left card
        content.add(buildPurchaseForm(), BorderLayout.CENTER);    // Right card

        // Place the content in the page, stretched across the width.
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        add(content, constraints);
    }

    // Builds the left card: picture, title, date, venue, price.
    private JPanel buildEventDetails() {
        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(ViewStyles.SURFACE);
        details.putClientProperty("FlatLaf.style", "arc: 15");   // Rounded corners
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                new EmptyBorder(26, 24, 26, 24)));
        details.setPreferredSize(new java.awt.Dimension(330, 470));

        // One column; each item goes on the next row (gridy++).
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        // Row 0: the picture holder (filled in by setEvent()).
        eventImage.setBackground(ViewStyles.INPUT);
        eventImage.setPreferredSize(new java.awt.Dimension(280, 156));
        eventImage.putClientProperty("FlatLaf.style", "arc: 9");
        constraints.insets = new Insets(0, 0, 16, 0);
        details.add(eventImage, constraints);

        // Row 1: title.
        ViewStyles.styleHeading(titleLabel, 24f);
        titleLabel.setBorder(new EmptyBorder(0, 0, 18, 0));
        constraints.gridy++;                                     // Same as gridy = gridy + 1
        details.add(titleLabel, constraints);

        // Rows 2-3: date and venue.
        constraints.gridy++;
        addDetail(details, dateLabel, constraints, 12);
        constraints.gridy++;
        addDetail(details, venueLabel, constraints, 12);
        // Row 4: price of the chosen tier, in bold white.
        constraints.gridy++;
        priceLabel.setForeground(ViewStyles.FOREGROUND);
        priceLabel.setFont(priceLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        constraints.insets = new Insets(8, 0, 0, 0);
        details.add(priceLabel, constraints);
        return details;
    }

    // Adds a grey detail line with some space below it.
    private void addDetail(JPanel panel, JLabel label, GridBagConstraints constraints, int bottomPadding) {
        ViewStyles.styleSecondaryText(label, 15f);
        constraints.insets = new Insets(0, 0, bottomPadding, 0);
        panel.add(label, constraints);
    }

    // Builds the right card: the guest form and the buttons.
    private JPanel buildPurchaseForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.SURFACE);
        form.putClientProperty("FlatLaf.style", "arc: 15");
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                new EmptyBorder(26, 26, 26, 26)));

        ViewStyles.styleInput(nameField);
        ViewStyles.styleInput(emailField);
        // The spinner contains a text box; if it's the usual kind, style that box too.
        // "instanceof ... editor" checks the type AND gives us a variable of that type.
        if (quantitySpinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            ViewStyles.styleInput(editor.getTextField());
        }
        quantitySpinner.addChangeListener(event -> updateTotal());   // Recalculate the total when quantity changes

        // Row 0: the heading, across both columns.
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

        // Each field takes two rows: its label, then the input below it.
        constraints.gridwidth = 1;
        addField(form, "Guest Name", nameField, constraints, 1);          // Rows 1-2
        addField(form, "Guest Email", emailField, constraints, 3);        // Rows 3-4
        addField(form, "Ticket Tier", tierSelector, constraints, 5);      // Rows 5-6
        addField(form, "Ticket Quantity", quantitySpinner, constraints, 7);   // Rows 7-8

        // A "renderer" decides how each drop-down item looks. This lambda builds a label like
        // "VIP - KES 3,750.00 (12 left)", highlighted blue when selected.
        tierSelector.setRenderer((list, tier, index, selected, focus) -> {
            JLabel item = new JLabel(tier == null ? "Select a ticket tier"
                : String.format("%s - KES %,.2f (%d left)", tier.getTierName(), tier.getPrice(), tier.getAvailableSeats()));
            item.setOpaque(true);
            item.setForeground(ViewStyles.FOREGROUND);
            item.setBackground(selected ? ViewStyles.ACCENT : ViewStyles.INPUT);
            item.setBorder(new EmptyBorder(6, 8, 6, 8));
            return item;
        });
        tierSelector.addActionListener(event -> updateTierSelection());   // Picking a tier updates everything

        totalLabel.setForeground(ViewStyles.FOREGROUND);
        totalLabel.setFont(totalLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        constraints.gridx = 0;
        // Row 9: "N tickets available" / "Sales Closed".
        ViewStyles.styleSecondaryText(availabilityLabel, 13f);
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(2, 0, 8, 0);
        form.add(availabilityLabel, constraints);

        // Row 10: the total price.
        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(12, 0, 18, 0);
        form.add(totalLabel, constraints);

        // Row 11: Cancel and Confirm buttons, on the right.
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

    // Adds a label on `row` and its input on the row below, both across the full width.
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

        constraints.gridy++;                                  // Next row for the input
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 15, 0);
        form.add(input, constraints);
        constraints.gridwidth = 1;                            // Reset for the caller
    }

    // Recalculates "Total: KSh ..." from the tier price x quantity.
    private void updateTotal() {
        int quantity = (Integer) quantitySpinner.getValue();  // The spinner gives an Object; cast it to Integer
        double total = selectedTier == null ? 0 : selectedTier.getPrice() * quantity;
        totalLabel.setText(String.format("Total: KSh %,.2f", total));
    }

    // Runs when a tier is picked: updates the price, the quantity limit, the availability
    // message and whether the Confirm button can be clicked.
    private void updateTierSelection() {
        selectedTier = (TicketTier) tierSelector.getSelectedItem();
        // No tier at all: disable buying.
        if (selectedTier == null) {
            priceLabel.setText("No ticket tiers available");
            quantitySpinner.setModel(new SpinnerNumberModel(1, 1, 1, 1));
            if (confirmButton != null) confirmButton.setEnabled(false);
            availabilityLabel.setText("Sales Closed");
            updateTotal();
            return;
        }
        priceLabel.setText(String.format("%s - KES %,.2f per ticket", selectedTier.getTierName(), selectedTier.getPrice()));
        // The quantity can't go above the seats left (and must allow at least 1).
        int maximumSeats = Math.max(1, selectedTier.getAvailableSeats());
        int quantity = Math.min((Integer) quantitySpinner.getValue(), maximumSeats);
        quantitySpinner.setModel(new SpinnerNumberModel(quantity, 1, maximumSeats, 1));
        // Can this tier be bought today?
        boolean dateIsOpen = selectedEvent != null && selectedEvent.canPurchase();
        boolean tierHasSeats = selectedTier.getAvailableSeats() > 0;
        availabilityLabel.setText(!dateIsOpen ? "Sales Closed"
                : tierHasSeats ? selectedTier.getAvailableSeats() + " tickets available" : "This tier is sold out");
        if (confirmButton != null) confirmButton.setEnabled(dateIsOpen && tierHasSeats);
        updateTotal();
    }

    // The objects work together here: the Event takes the seats (its own rule), then
    // the OrderManager creates the Booking, then the callback shows the receipt.
    private void submitBooking() {
        int quantity = (Integer) quantitySpinner.getValue();
        // Step 1: check the form is filled in.
        if (selectedEvent == null || selectedTier == null || nameField.getText().isBlank()
            || emailField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter valid guest details before confirming.",
                    "Invalid booking", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Step 2: ask the EventManager to take the seats (it saves them in the database).
        // It refuses if sales are closed or the seats ran out.
        if (!eventManager.purchaseTickets(selectedEvent, selectedTier.getTierName(), quantity)) {
            JOptionPane.showMessageDialog(this, "Sales are closed or this ticket tier is sold out.", "Purchase unavailable",
                    JOptionPane.WARNING_MESSAGE);
            setEvent(selectedEvent);                          // Refresh the screen with the latest seat counts
            return;
        }

        // Step 3: record the Booking (trim() removes spaces at the ends of the typed text).
        Booking booking = orderManager.createBooking(selectedEvent.getEventId(), nameField.getText().trim(),
            emailField.getText().trim(), selectedTier.getTierName(), quantity, selectedTier.getPrice() * quantity);
        clearForm();
        // Step 4: hand the booking to MainFrame, which shows the receipt.
        bookingConfirmed.accept(booking);
    }

    // Resets the form for the next guest.
    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        quantitySpinner.setValue(1);
    }
}
