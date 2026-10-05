package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import models.Event;
import models.TicketTier;

public class WelcomePanel extends JPanel {
    private final EventManager eventManager;
    private final Runnable browseEvents;
    private final Consumer<String> openEvent;

    public WelcomePanel(EventManager eventManager, Runnable browseEvents, Runnable hostEventAction,
            Consumer<String> openEvent) {
        this.eventManager = eventManager;
        this.browseEvents = browseEvents;
        this.openEvent = openEvent;
        setLayout(new BorderLayout());
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(18, 28, 14, 28));

        add(buildHeader(hostEventAction), BorderLayout.NORTH);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildHero(hostEventAction));
        content.add(Box.createVerticalStrut(22));
        content.add(buildUpcomingEvents());
        JScrollPane scroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeader(Runnable hostEventAction) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel identity = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        identity.setOpaque(false);
        JLabel name = new JLabel("SakaTickets");
        ViewStyles.styleHeading(name, 20f);
        JLabel descriptor = new JLabel("NAIROBI  /  LIVE EVENTS");
        descriptor.setForeground(new Color(0x88, 0x96, 0xB3));
        descriptor.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 9));
        identity.add(name);
        identity.add(descriptor);
        header.add(identity, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        JButton browse = new JButton("Browse events");
        ViewStyles.styleSecondaryButton(browse);
        browse.addActionListener(action -> browseEvents.run());
        JButton host = new JButton("Host an event");
        ViewStyles.stylePrimaryButton(host);
        host.addActionListener(action -> hostEventAction.run());
        actions.add(browse);
        actions.add(host);
        header.add(actions, BorderLayout.EAST);
        header.setBorder(new EmptyBorder(0, 2, 18, 2));
        return header;
    }

    private JPanel buildHero(Runnable hostEventAction) {
        Event featured = eventManager.getPublicEvents().stream()
                .filter(event -> event.getTitle().toLowerCase(java.util.Locale.ROOT).contains("amapiano"))
                .findFirst().orElse(eventManager.getPublicEvents().stream().findFirst().orElse(null));
        JPanel hero = EventVisuals.hero(294, 14);
        hero.setLayout(new BorderLayout());
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(24, 28, 24, 28)));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("FEATURED EVENT  /  NAIROBI");
        eyebrow.setForeground(new Color(0x00, 0xB4, 0xFF));
        eyebrow.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10));
        JLabel title = new JLabel(featured == null ? "Find your next night out" : featured.getTitle());
        ViewStyles.styleHeading(title, 29f);
        JLabel venue = new JLabel(featured == null ? "Discover the city, one event at a time"
                : "at " + featured.getVenue());
        venue.setForeground(new Color(0x00, 0xE5, 0xC4));
        venue.setFont(venue.getFont().deriveFont(java.awt.Font.PLAIN, 18f));
        JLabel date = new JLabel(featured == null ? "" : featured.getDate() + "   ·   " + featured.getVenue());
        ViewStyles.styleSecondaryText(date, 12f);
        JLabel description = new JLabel("Music, culture and ideas worth showing up for.");
        ViewStyles.styleSecondaryText(description, 13f);
        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(12));
        copy.add(title);
        copy.add(Box.createVerticalStrut(3));
        copy.add(venue);
        copy.add(Box.createVerticalStrut(9));
        copy.add(date);
        copy.add(Box.createVerticalStrut(5));
        copy.add(description);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton book = new JButton("Book featured event");
        ViewStyles.stylePrimaryButton(book);
        book.setEnabled(featured != null && featured.canPurchaseOn(java.time.LocalDate.now()));
        book.addActionListener(action -> {
            if (featured != null) openEvent.accept(featured.getEventId());
        });
        JButton host = new JButton("Create an event");
        ViewStyles.styleSecondaryButton(host);
        host.addActionListener(action -> hostEventAction.run());
        actions.add(book);
        actions.add(host);
        copy.add(Box.createVerticalStrut(17));
        copy.add(actions);
        hero.add(copy, BorderLayout.WEST);
        return hero;
    }

    private JPanel buildUpcomingEvents() {
        JPanel section = new JPanel(new BorderLayout(0, 12));
        section.setOpaque(false);
        JPanel headingRow = new JPanel(new BorderLayout());
        headingRow.setOpaque(false);
        JLabel heading = new JLabel("Upcoming Events");
        ViewStyles.styleHeading(heading, 17f);
        headingRow.add(heading, BorderLayout.WEST);
        JButton viewAll = new JButton("View all events  →");
        viewAll.setBorderPainted(false);
        viewAll.setContentAreaFilled(false);
        viewAll.setForeground(new Color(0x00, 0xB4, 0xFF));
        viewAll.addActionListener(action -> browseEvents.run());
        headingRow.add(viewAll, BorderLayout.EAST);
        section.add(headingRow, BorderLayout.NORTH);

        JPanel grid = new JPanel(new java.awt.GridLayout(1, 4, 13, 0));
        grid.setOpaque(false);
        List<Event> upcoming = eventManager.getPublicEvents().stream().limit(4).toList();
        for (Event event : upcoming) grid.add(createEventCard(event));
        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    private JPanel createEventCard(Event event) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(new Color(0x17, 0x1B, 0x24));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(8, 8, 10, 8)));
        card.putClientProperty("FlatLaf.style", "arc: 10");
        card.add(EventVisuals.event(event, 94, 8), BorderLayout.NORTH);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel category = new JLabel(event.getCategory().toUpperCase(java.util.Locale.ROOT));
        category.setForeground(new Color(0x00, 0xE5, 0xC4));
        category.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 9));
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 12f);
        title.setToolTipText(event.getTitle());
        JLabel date = new JLabel(event.getDate());
        ViewStyles.styleSecondaryText(date, 10f);
        JLabel price = new JLabel(String.format("From KES %,.0f", event.getTicketTiers().stream()
                .mapToDouble(TicketTier::getPrice).min().orElse(0)));
        price.setForeground(new Color(0x00, 0xB4, 0xFF));
        price.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10));
        text.add(category);
        text.add(Box.createVerticalStrut(4));
        text.add(title);
        text.add(Box.createVerticalStrut(3));
        text.add(date);
        text.add(Box.createVerticalStrut(7));
        text.add(price);
        card.add(text, BorderLayout.CENTER);
        JButton tickets = new JButton("View tickets");
        ViewStyles.styleSecondaryButton(tickets);
        tickets.setFont(tickets.getFont().deriveFont(11f));
        tickets.setEnabled(event.canPurchaseOn(java.time.LocalDate.now()) && event.getAvailableSeats() > 0);
        tickets.addActionListener(action -> openEvent.accept(event.getEventId()));
        card.add(tickets, BorderLayout.SOUTH);
        return card;
    }
}