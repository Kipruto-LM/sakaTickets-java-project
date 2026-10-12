// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.EventManager;          // Used to look up the booked event
import java.awt.BorderLayout;             // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                    // An RGB colour
import java.awt.Dimension;                // A width and height
import java.awt.FlowLayout;               // Layout that places items in a row
import java.awt.GridBagConstraints;       // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;            // A flexible grid layout
import java.awt.Insets;                   // Spacing around an item: top, left, bottom, right
import javax.swing.BorderFactory;         // Helper for making borders
import javax.swing.JButton;               // A clickable button
import javax.swing.JLabel;                // A piece of text
import javax.swing.JPanel;                // A container
import javax.swing.border.EmptyBorder;    // Invisible padding
import models.Booking;                    // The purchase being shown
import models.Event;                      // The event that was booked

/**
 * The receipt screen shown after a successful purchase.
 *
 * Layout: a single card in the middle of the page with
 *   - a green tick and "Ticket confirmed" at the top,
 *   - the event picture,
 *   - the booking details (attendee, date, venue, ticket, quantity),
 *   - the booking reference and total,
 *   - an "Explore more events" button.
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel.
 * - MVC: a View that only displays data. setBooking() reads from a Booking object (Model)
 *   and asks the EventManager for that booking's Event.
 * - Fields as UI state: the labels are created once, and setBooking() just updates their text.
 */
public class TicketSummaryPanel extends JPanel {
    private final EventManager eventManager;    // To find the Event for a booking
    private final Runnable backToEvents;        // Callback: go back to the catalogue
    // The labels below are fields so setBooking() can change their text later.
    // "-" is a placeholder until a booking is shown.
    private final JPanel hero = new JPanel(new BorderLayout());   // Holds the event picture
    private final JLabel eventTitle = new JLabel("Your event");
    private final JLabel reference = new JLabel("-");             // Booking reference code
    private final JLabel guest = new JLabel("-");                 // Name and email
    private final JLabel eventDate = new JLabel("-");
    private final JLabel venue = new JLabel("-");
    private final JLabel ticket = new JLabel("-");                // Tier name
    private final JLabel count = new JLabel("-");                 // Number of tickets
    private final JLabel total = new JLabel("KES 0.00");

    // Constructor: stores what it's given, then builds the layout once.
    public TicketSummaryPanel(EventManager eventManager, Runnable backToEvents) {
        this.eventManager = eventManager;
        this.backToEvents = backToEvents;
        buildView();
    }

    // Fills the receipt from a Booking object, using its getters (encapsulation).
    public void setBooking(Booking booking) {
        Event event = eventManager.getEventById(booking.getEventId());   // null if it was deleted
        // Swap in the event's picture (or the default concert photo if there's no event).
        hero.removeAll();
        hero.add(event == null ? EventVisuals.hero(180, 12) : EventVisuals.event(event, 180, 12),
            BorderLayout.CENTER);
        hero.revalidate();     // Re-do the layout after changing what's inside
        hero.repaint();        // Redraw it
        // Fill in each label. "event == null ? ... : ..." avoids errors if the event is missing.
        eventTitle.setText(event == null ? "Booking confirmed" : event.getTitle());
        guest.setText(booking.getCustomerName() + "  ·  " + booking.getCustomerEmail());
        reference.setText(booking.getBookingId());
        eventDate.setText(event == null ? "" : event.getDate());
        venue.setText(event == null ? "" : event.getVenue());
        ticket.setText(booking.getTierName());
        count.setText(Integer.toString(booking.getQuantity()));               // int -> text
        total.setText(String.format("KES %,.2f", booking.getTotalPrice()));   // e.g. "KES 2,400.00"
    }

    // Builds the receipt card. Called once, from the constructor.
    private void buildView() {
        // GridBagLayout with one item centres that item on the page.
        setLayout(new GridBagLayout());
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(24, 20, 24, 20));

        // The card itself: outline + padding, fixed size.
        JPanel summary = new JPanel(new BorderLayout(0, 14));
        summary.setBackground(new Color(0x17, 0x1B, 0x24));
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(18, 18, 18, 18)));
        summary.setPreferredSize(new Dimension(620, 560));

        // --- Top: green tick box + headings ---
        JPanel masthead = new JPanel(new BorderLayout(12, 0));
        masthead.setOpaque(false);
        JPanel checkBox = new JPanel(new GridBagLayout());       // GridBagLayout centres the tick
        checkBox.setOpaque(true);
        checkBox.setBackground(new Color(0x08, 0x35, 0x31));     // Dark green
        checkBox.setBorder(BorderFactory.createLineBorder(new Color(0x00, 0xE0, 0xA0)));   // Bright green outline
        checkBox.setPreferredSize(new Dimension(46, 46));
        JLabel check = new JLabel("✓");
        check.setForeground(new Color(0x00, 0xE0, 0xA0));
        check.setFont(check.getFont().deriveFont(java.awt.Font.BOLD, 25f));
        checkBox.add(check);
        masthead.add(checkBox, BorderLayout.WEST);
        // Heading and caption stacked: 2 rows, 1 column.
        JPanel headingGroup = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        headingGroup.setOpaque(false);
        JLabel heading = new JLabel("Ticket confirmed");
        ViewStyles.styleHeading(heading, 21f);
        JLabel caption = new JLabel("Your booking is ready");
        ViewStyles.styleSecondaryText(caption, 12f);
        headingGroup.add(heading);
        headingGroup.add(caption);
        masthead.add(headingGroup, BorderLayout.CENTER);
        summary.add(masthead, BorderLayout.NORTH);

        // --- Middle: picture, then the details grid ---
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(560, 180));
        hero.add(EventVisuals.hero(180, 12), BorderLayout.CENTER);   // Placeholder picture until setBooking()
        body.add(hero, BorderLayout.NORTH);
        // The details use a GridBagLayout: column 0 = label, column 1 = value.
        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        // Row 0: the event title, spanning both columns (gridwidth = 2).
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;                                   // Column
        constraints.gridy = 0;                                   // Row
        constraints.gridwidth = 2;                               // Spans 2 columns
        constraints.fill = GridBagConstraints.HORIZONTAL;        // Stretch sideways
        constraints.weightx = 1;                                 // Take spare width
        constraints.insets = new Insets(0, 0, 12, 0);            // 12px gap below
        ViewStyles.styleHeading(eventTitle, 17f);
        details.add(eventTitle, constraints);
        // Rows 1-5: one "label  value" pair each.
        addInfo(details, "Attendee", guest, 1);
        addInfo(details, "Date", eventDate, 2);
        addInfo(details, "Venue", venue, 3);
        addInfo(details, "Ticket", ticket, 4);
        addInfo(details, "Quantity", count, 5);

        // Row 6: the reference + total strip, with a line along its top.
        JPanel receipt = new JPanel(new BorderLayout(0, 5));
        receipt.setBackground(new Color(0x10, 0x15, 0x20));
        receipt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2A, 0x30, 0x44)),   // Top line only
                new EmptyBorder(12, 12, 12, 12)));
        JLabel refLabel = new JLabel("BOOKING REFERENCE");
        refLabel.setForeground(new Color(0x88, 0x96, 0xB3));
        refLabel.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 10f));
        reference.setForeground(new Color(0x00, 0xB4, 0xFF));
        reference.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 17f));
        receipt.add(refLabel, BorderLayout.NORTH);
        receipt.add(reference, BorderLayout.CENTER);
        total.setForeground(new Color(0x00, 0xB4, 0xFF));
        total.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 16f));
        receipt.add(total, BorderLayout.EAST);                  // Total on the right
        constraints = new GridBagConstraints();                  // Fresh rules for row 6
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(8, 0, 0, 0);
        details.add(receipt, constraints);
        body.add(details, BorderLayout.CENTER);
        summary.add(body, BorderLayout.CENTER);

        // --- Bottom: the button, on the right ---
        JButton browse = new JButton("Explore more events");
        ViewStyles.stylePrimaryButton(browse);
        browse.addActionListener(action -> backToEvents.run());   // Callback to MainFrame
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(browse);
        summary.add(actions, BorderLayout.SOUTH);
        add(summary);                                             // Put the card on the page
    }

    // Adds one "label  value" row to the details grid.
    private void addInfo(JPanel panel, String label, JLabel value, int row) {
        // Column 0: the grey label, e.g. "Venue".
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;                                 // Doesn't stretch
        constraints.insets = new Insets(0, 0, 9, 18);            // Gap below and to the right
        constraints.anchor = GridBagConstraints.WEST;            // Stick to the left
        JLabel key = new JLabel(label);
        ViewStyles.styleSecondaryText(key, 12f);
        panel.add(key, constraints);
        // Column 1: the value, which takes the remaining width.
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        value.setForeground(new Color(0xF0, 0xF4, 0xFF));
        value.setFont(value.getFont().deriveFont(12f));
        panel.add(value, constraints);
    }
}
