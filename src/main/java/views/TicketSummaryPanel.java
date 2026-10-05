package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import models.Booking;
import models.Event;

public class TicketSummaryPanel extends JPanel {
    private final EventManager eventManager;
    private final Runnable backToEvents;
    private final JPanel hero = new JPanel(new BorderLayout());
    private final JLabel eventTitle = new JLabel("Your event");
    private final JLabel reference = new JLabel("-");
    private final JLabel guest = new JLabel("-");
    private final JLabel eventDate = new JLabel("-");
    private final JLabel venue = new JLabel("-");
    private final JLabel ticket = new JLabel("-");
    private final JLabel count = new JLabel("-");
    private final JLabel total = new JLabel("KES 0.00");

    public TicketSummaryPanel(EventManager eventManager, Runnable backToEvents) {
        this.eventManager = eventManager;
        this.backToEvents = backToEvents;
        buildView();
    }

    public void setBooking(Booking booking) {
        Event event = eventManager.getEventById(booking.getEventId());
        hero.removeAll();
        hero.add(event == null ? EventVisuals.hero(180, 12) : EventVisuals.event(event, 180, 12),
            BorderLayout.CENTER);
        hero.revalidate();
        hero.repaint();
        eventTitle.setText(event == null ? "Booking confirmed" : event.getTitle());
        guest.setText(booking.getCustomerName() + "  ·  " + booking.getCustomerEmail());
        reference.setText(booking.getBookingId());
        eventDate.setText(event == null ? "" : event.getDate());
        venue.setText(event == null ? "" : event.getVenue());
        ticket.setText(booking.getTierName());
        count.setText(Integer.toString(booking.getQuantity()));
        total.setText(String.format("KES %,.2f", booking.getTotalPrice()));
    }

    private void buildView() {
        setLayout(new GridBagLayout());
        setBackground(new Color(0x0F, 0x11, 0x17));
        setBorder(new EmptyBorder(24, 20, 24, 20));

        JPanel summary = new JPanel(new BorderLayout(0, 14));
        summary.setBackground(new Color(0x17, 0x1B, 0x24));
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(18, 18, 18, 18)));
        summary.setPreferredSize(new Dimension(620, 560));

        JPanel masthead = new JPanel(new BorderLayout(12, 0));
        masthead.setOpaque(false);
        JPanel checkBox = new JPanel(new GridBagLayout());
        checkBox.setOpaque(true);
        checkBox.setBackground(new Color(0x08, 0x35, 0x31));
        checkBox.setBorder(BorderFactory.createLineBorder(new Color(0x00, 0xE0, 0xA0)));
        checkBox.setPreferredSize(new Dimension(46, 46));
        JLabel check = new JLabel("✓");
        check.setForeground(new Color(0x00, 0xE0, 0xA0));
        check.setFont(check.getFont().deriveFont(java.awt.Font.BOLD, 25f));
        checkBox.add(check);
        masthead.add(checkBox, BorderLayout.WEST);
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

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(560, 180));
        hero.add(EventVisuals.hero(180, 12), BorderLayout.CENTER);
        body.add(hero, BorderLayout.NORTH);
        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 12, 0);
        ViewStyles.styleHeading(eventTitle, 17f);
        details.add(eventTitle, constraints);
        addInfo(details, "Attendee", guest, 1);
        addInfo(details, "Date", eventDate, 2);
        addInfo(details, "Venue", venue, 3);
        addInfo(details, "Ticket", ticket, 4);
        addInfo(details, "Quantity", count, 5);

        JPanel receipt = new JPanel(new BorderLayout(0, 5));
        receipt.setBackground(new Color(0x10, 0x15, 0x20));
        receipt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(12, 12, 12, 12)));
        JLabel refLabel = new JLabel("BOOKING REFERENCE");
        refLabel.setForeground(new Color(0x88, 0x96, 0xB3));
        refLabel.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10));
        reference.setForeground(new Color(0x00, 0xB4, 0xFF));
        reference.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 17));
        receipt.add(refLabel, BorderLayout.NORTH);
        receipt.add(reference, BorderLayout.CENTER);
        total.setForeground(new Color(0x00, 0xB4, 0xFF));
        total.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 16));
        receipt.add(total, BorderLayout.EAST);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(8, 0, 0, 0);
        details.add(receipt, constraints);
        body.add(details, BorderLayout.CENTER);
        summary.add(body, BorderLayout.CENTER);

        JButton browse = new JButton("Explore more events");
        ViewStyles.stylePrimaryButton(browse);
        browse.addActionListener(action -> backToEvents.run());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(browse);
        summary.add(actions, BorderLayout.SOUTH);
        add(summary);
    }

    private void addInfo(JPanel panel, String label, JLabel value, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 9, 18);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel key = new JLabel(label);
        ViewStyles.styleSecondaryText(key, 12f);
        panel.add(key, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        value.setForeground(new Color(0xF0, 0xF4, 0xFF));
        value.setFont(value.getFont().deriveFont(12f));
        panel.add(value, constraints);
    }
}
