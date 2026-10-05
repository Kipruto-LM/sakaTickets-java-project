package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import models.Event;
import models.TicketTier;

public class EventCatalogPanel extends JPanel {
    private final EventManager eventManager;
    private final Consumer<String> checkoutAction;
    private final JPanel eventList = new JPanel();

    public EventCatalogPanel(EventManager eventManager, Consumer<String> checkoutAction, Runnable backToHome) {
        this.eventManager = eventManager;
        this.checkoutAction = checkoutAction;
        setLayout(new BorderLayout(0, 16));
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel headings = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        headings.setOpaque(false);
        JLabel heading = new JLabel("Upcoming Events");
        ViewStyles.styleHeading(heading, 24f);
        JLabel subheading = new JLabel("Find something good happening around Nairobi.");
        ViewStyles.styleSecondaryText(subheading, 12f);
        headings.add(heading);
        headings.add(subheading);
        header.add(headings, BorderLayout.WEST);
        JButton backButton = new JButton("Back to Home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(action -> backToHome.run());
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        eventList.setLayout(new GridLayout(0, 3, 15, 15));
        eventList.setBackground(ViewStyles.BACKGROUND);
        eventList.setBorder(new EmptyBorder(2, 1, 14, 1));
        refreshEvents();

        JScrollPane scrollPane = new JScrollPane(eventList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ViewStyles.BACKGROUND);
        scrollPane.getViewport().setBackground(ViewStyles.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshEvents() {
        eventList.removeAll();
        for (Event event : eventManager.getPublicEvents()) {
            JPanel card = createEventCard(event, checkoutAction);
            eventList.add(card);
        }
        if (eventManager.getPublicEvents().isEmpty()) {
            JLabel empty = new JLabel("No public events right now. Please check back soon.", JLabel.CENTER);
            ViewStyles.styleSecondaryText(empty, 15f);
            eventList.add(empty);
        }
        eventList.revalidate();
        eventList.repaint();
    }

    private JPanel createEventCard(Event event, Consumer<String> checkoutAction) {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 9));
        card.setBackground(new Color(0x17, 0x1B, 0x24));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
            BorderFactory.createEmptyBorder(8, 8, 10, 8)));
        card.putClientProperty("FlatLaf.style", "arc: 10");
        card.setPreferredSize(new Dimension(280, 285));
        card.add(EventVisuals.event(event, 132, 8), BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 15f);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel category = new JLabel(event.getCategory().toUpperCase(java.util.Locale.ROOT));
        category.setForeground(new Color(0x00, 0xE5, 0xC4));
        category.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 9));
        category.setAlignmentX(LEFT_ALIGNMENT);
        details.add(title);
        details.add(category);
        JLabel date = new JLabel(event.getDate());
        ViewStyles.styleSecondaryText(date, 11f);
        date.setBorder(new EmptyBorder(5, 0, 1, 0));
        date.setAlignmentX(LEFT_ALIGNMENT);
        JLabel venue = new JLabel(event.getVenue());
        ViewStyles.styleSecondaryText(venue, 11f);
        venue.setAlignmentX(LEFT_ALIGNMENT);
        details.add(date);
        details.add(venue);
        details.add(date);
        details.add(venue);
        event.getCustomDetails().entrySet().stream().limit(2).forEach(detail -> {
            String name = detail.getKey();
            String value = detail.getValue();
            JLabel custom = new JLabel(name + ": " + value);
            ViewStyles.styleSecondaryText(custom, 12f);
            custom.setAlignmentX(LEFT_ALIGNMENT);
            details.add(custom);
        });

        JPanel purchase = new JPanel();
        purchase.setOpaque(false);
        purchase.setLayout(new BoxLayout(purchase, BoxLayout.Y_AXIS));

        double lowestPrice = event.getTicketTiers().stream().mapToDouble(TicketTier::getPrice)
            .min().orElse(0.0);
        JLabel price = new JLabel(String.format("From KES %,.0f", lowestPrice));
        price.setForeground(ViewStyles.FOREGROUND);
        price.setFont(price.getFont().deriveFont(Font.BOLD, 16f));
        price.setAlignmentX(RIGHT_ALIGNMENT);

        JLabel seats = new JLabel(event.getAvailableSeats() + " tickets remaining");
        ViewStyles.styleSecondaryText(seats, 11f);
        seats.setAlignmentX(RIGHT_ALIGNMENT);
        seats.setBorder(new EmptyBorder(4, 0, 10, 0));

        JButton bookButton = new JButton("Book Now");
        boolean salesOpen = event.canPurchaseOn(java.time.LocalDate.now()) && event.getAvailableSeats() > 0;
        if (salesOpen) {
            ViewStyles.stylePrimaryButton(bookButton);
        } else {
            ViewStyles.styleSecondaryButton(bookButton);
            bookButton.setText("Sales Closed");
            bookButton.setEnabled(false);
        }
        bookButton.setAlignmentX(RIGHT_ALIGNMENT);
        bookButton.addActionListener(action -> checkoutAction.accept(event.getEventId()));

        purchase.add(price);
        purchase.add(seats);
        purchase.add(bookButton);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(purchase);
        JPanel info = new JPanel(new BorderLayout(0, 5));
        info.setOpaque(false);
        info.add(details, BorderLayout.CENTER);
        info.add(east, BorderLayout.EAST);
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    private static class RoundedCardPanel extends JPanel {
        private RoundedCardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D graphics2D = (java.awt.Graphics2D) graphics.create();
            graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }
}