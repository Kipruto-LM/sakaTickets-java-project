package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
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
        setLayout(new BorderLayout(0, 18));
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel heading = new JLabel("Discover events");
        ViewStyles.styleHeading(heading, 24f);
        header.add(heading, BorderLayout.WEST);
        JButton backButton = new JButton("Back to Home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(action -> backToHome.run());
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        eventList.setLayout(new BoxLayout(eventList, BoxLayout.Y_AXIS));
        eventList.setBackground(ViewStyles.BACKGROUND);
        eventList.setBorder(new EmptyBorder(2, 2, 2, 2));
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
            card.setAlignmentX(LEFT_ALIGNMENT);
            eventList.add(card);
            eventList.add(Box.createVerticalStrut(15));
        }
        eventList.revalidate();
        eventList.repaint();
    }

    private JPanel createEventCard(Event event, Consumer<String> checkoutAction) {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(24, 0));
        card.setBackground(ViewStyles.SURFACE);
        Color accent;
        try {
            accent = Color.decode(event.getAccentColorHex());
        } catch (RuntimeException exception) {
            accent = ViewStyles.ACCENT;
        }
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 5, 1, 1, accent),
                BorderFactory.createEmptyBorder(16, 17, 16, 17)));
        card.putClientProperty("FlatLaf.style", "arc: 15");
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        card.setPreferredSize(new Dimension(720, 170));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 18f);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel date = new JLabel(event.getDate());
        ViewStyles.styleSecondaryText(date, 14f);
        date.setBorder(new EmptyBorder(5, 0, 2, 0));
        date.setAlignmentX(LEFT_ALIGNMENT);

        JLabel venue = new JLabel(event.getVenue());
        ViewStyles.styleSecondaryText(venue, 14f);
        venue.setAlignmentX(LEFT_ALIGNMENT);

        JLabel category = new JLabel(event.getCategory() + "  |  " + event.getStatus().replace('_', ' '));
        ViewStyles.styleSecondaryText(category, 12f);
        category.setAlignmentX(LEFT_ALIGNMENT);
        details.add(title);
        details.add(category);
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

        JLabel seats = new JLabel(event.getAvailableSeats() + " seats left");
        ViewStyles.styleSecondaryText(seats, 14f);
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
        card.add(east, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        if (!event.getBannerImagePath().isBlank() && new java.io.File(event.getBannerImagePath()).isFile()) {
            ImageIcon source = new ImageIcon(event.getBannerImagePath());
            JLabel image = new JLabel(new ImageIcon(source.getImage().getScaledInstance(150, 104,
                java.awt.Image.SCALE_SMOOTH)));
            center.add(image, BorderLayout.WEST);
        }
        center.add(details, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

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