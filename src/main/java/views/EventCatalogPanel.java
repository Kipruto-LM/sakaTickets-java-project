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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import models.Event;

public class EventCatalogPanel extends JPanel {
    private static final Color BACKGROUND = new Color(19, 27, 35);
    private static final Color CARD_BACKGROUND = new Color(28, 39, 48);
    private static final Color BORDER = new Color(55, 72, 81);
    private static final Color ACCENT = new Color(67, 207, 155);
    private static final Color PRIMARY_TEXT = new Color(239, 244, 246);
    private static final Color SECONDARY_TEXT = new Color(164, 179, 187);

    public EventCatalogPanel(EventManager eventManager, Consumer<String> checkoutAction, Runnable backToHome) {
        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel heading = new JLabel("Discover events");
        heading.setForeground(PRIMARY_TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 27f));
        header.add(heading, BorderLayout.WEST);
        JButton backButton = new JButton("Back to Home");
        backButton.addActionListener(action -> backToHome.run());
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel eventList = new JPanel();
        eventList.setLayout(new BoxLayout(eventList, BoxLayout.Y_AXIS));
        eventList.setBackground(BACKGROUND);
        eventList.setBorder(new EmptyBorder(2, 2, 2, 2));

        for (Event event : eventManager.getEvents()) {
            JPanel card = createEventCard(event, checkoutAction);
            card.setAlignmentX(LEFT_ALIGNMENT);
            eventList.add(card);
            eventList.add(Box.createRigidArea(new Dimension(0, 14)));
        }

        JScrollPane scrollPane = new JScrollPane(eventList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createEventCard(Event event, Consumer<String> checkoutAction) {
        JPanel card = new JPanel(new BorderLayout(24, 0));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(15, 15, 15, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        card.setPreferredSize(new Dimension(720, 132));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(event.getTitle());
        title.setForeground(PRIMARY_TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel date = new JLabel(event.getDate());
        date.setForeground(ACCENT);
        date.setFont(date.getFont().deriveFont(Font.PLAIN, 13f));
        date.setBorder(new EmptyBorder(7, 0, 3, 0));
        date.setAlignmentX(LEFT_ALIGNMENT);

        JLabel venue = new JLabel(event.getVenue());
        venue.setForeground(SECONDARY_TEXT);
        venue.setFont(venue.getFont().deriveFont(Font.PLAIN, 13f));
        venue.setAlignmentX(LEFT_ALIGNMENT);

        details.add(title);
        details.add(date);
        details.add(venue);
        card.add(details, BorderLayout.CENTER);

        JPanel purchase = new JPanel();
        purchase.setOpaque(false);
        purchase.setLayout(new BoxLayout(purchase, BoxLayout.Y_AXIS));

        JLabel price = new JLabel(String.format("KSh %,.0f", event.getTicketPrice()));
        price.setForeground(PRIMARY_TEXT);
        price.setFont(price.getFont().deriveFont(Font.BOLD, 17f));
        price.setAlignmentX(RIGHT_ALIGNMENT);

        JLabel seats = new JLabel(event.getAvailableSeats() + " seats left");
        seats.setForeground(SECONDARY_TEXT);
        seats.setFont(seats.getFont().deriveFont(Font.PLAIN, 12f));
        seats.setAlignmentX(RIGHT_ALIGNMENT);
        seats.setBorder(new EmptyBorder(4, 0, 10, 0));

        JButton bookButton = new JButton("Book Now");
        bookButton.setForeground(new Color(18, 35, 30));
        bookButton.setBackground(ACCENT);
        bookButton.setFont(bookButton.getFont().deriveFont(Font.BOLD, 13f));
        bookButton.setOpaque(true);
        bookButton.setFocusPainted(false);
        bookButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        bookButton.setAlignmentX(RIGHT_ALIGNMENT);
        bookButton.addActionListener(action -> checkoutAction.accept(event.getEventId()));

        purchase.add(price);
        purchase.add(seats);
        purchase.add(bookButton);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(purchase);
        card.add(east, BorderLayout.EAST);

        return card;
    }
}