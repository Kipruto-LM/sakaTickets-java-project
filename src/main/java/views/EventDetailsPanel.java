package views;

import controllers.OrderManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import models.Event;
import models.TicketTier;

/**
 * The guest's page for one event: a big banner, the key details, how popular the event is,
 * and a card for each ticket tier with its price and how many seats are left.
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel, so MainFrame can show it as one of its pages.
 * - Reuse through objects: it uses the same ColumnChart class as the admin pages, just with
 *   guest-friendly data (tickets per day, never revenue).
 * - Callbacks: choosing a tier calls bookAction (a BiConsumer of event + tier name), and
 *   MainFrame opens checkout with that tier already selected.
 * - Nested class: AvailabilityBar overrides paintComponent() to draw a slim meter.
 */
public class EventDetailsPanel extends JPanel {
    private static final Color PAGE = ViewStyles.BACKGROUND;
    private static final Color CARD = new Color(0x17, 0x1B, 0x24);
    private static final Color BORDER = new Color(0x2A, 0x30, 0x44);
    private static final Color TEAL = new Color(0x00, 0xE5, 0xC4);
    private static final Color WARNING = new Color(0xF5, 0xB0, 0x41);
    private static final Color SOLD_OUT = new Color(0xFF, 0x4D, 0x6A);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final int DEMAND_DAYS = 14;

    private final OrderManager orderManager;
    private final Runnable backAction;
    private final BiConsumer<Event, String> bookAction;
    private final JPanel content = new ScrollableColumn();
    private final JScrollPane scroll;

    public EventDetailsPanel(OrderManager orderManager, Runnable backAction, BiConsumer<Event, String> bookAction) {
        this.orderManager = orderManager;
        this.backAction = backAction;
        this.bookAction = bookAction;
        setLayout(new BorderLayout());
        setBackground(PAGE);
        setBorder(new EmptyBorder(20, 30, 16, 30));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(PAGE);
        content.setBorder(new EmptyBorder(0, 0, 10, 8));
        scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    // Public entry point: rebuilds the page for one event.
    public void showEvent(Event event) {
        content.removeAll();
        if (event == null) {
            JLabel missing = new JLabel("This event is no longer available.");
            ViewStyles.styleHeading(missing, 18f);
            addBlock(buildTopBar());
            addBlock(missing);
        } else {
            addBlock(buildTopBar());
            JPanel banner = EventVisuals.event(event, 260, 14);
            banner.setPreferredSize(new Dimension(1, 260));
            addBlock(banner);
            addBlock(buildColumns(event));
        }
        content.revalidate();
        content.repaint();
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new java.awt.Point(0, 0)));
    }

    private void addBlock(JComponent block) {
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));
        content.add(block);
        content.add(Box.createVerticalStrut(16));
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JButton back = new JButton("Back to events");
        ViewStyles.styleSecondaryButton(back);
        back.addActionListener(action -> backAction.run());
        bar.add(back, BorderLayout.WEST);
        JLabel brand = new JLabel("SakaTickets");
        ViewStyles.styleHeading(brand, 16f);
        bar.add(brand, BorderLayout.EAST);
        return bar;
    }

    // Two columns: event information on the left, ticket choices on the right.
    private JPanel buildColumns(Event event) {
        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = 1.5;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.anchor = GridBagConstraints.NORTH;
        left.insets = new Insets(0, 0, 0, 9);
        GridBagConstraints right = (GridBagConstraints) left.clone();
        right.gridx = 1;
        right.weightx = 1;
        right.insets = new Insets(0, 9, 0, 0);
        JPanel info = buildInfoColumn(event);
        JPanel tickets = buildTicketsColumn(event);
        info.setPreferredSize(new Dimension(1, info.getPreferredSize().height));
        tickets.setPreferredSize(new Dimension(1, tickets.getPreferredSize().height));
        columns.add(info, left);
        columns.add(tickets, right);
        return columns;
    }

    // ---- Left column: what, when, where, and how popular ----

    private JPanel buildInfoColumn(Event event) {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel category = new JLabel(event.getCategory().toUpperCase(Locale.ROOT));
        category.setForeground(TEAL);
        category.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 11f));
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 30f);
        title.setBorder(new EmptyBorder(4, 0, 12, 0));
        column.add(leftAligned(category));
        column.add(leftAligned(title));

        JPanel facts = new JPanel(new GridLayout(1, 3, 12, 0));
        facts.setOpaque(false);
        facts.add(fact("When", longDate(event.getDate()), EventAnalyticsPanel.countdown(event.getDate())));
        facts.add(fact("Where", event.getVenue(), "Venue"));
        double lowest = event.getTicketTiers().stream().filter(tier -> tier.getAvailableSeats() > 0)
                .mapToDouble(TicketTier::getPrice).min()
                .orElse(event.getTicketTiers().stream().mapToDouble(TicketTier::getPrice).min().orElse(0));
        facts.add(fact("Tickets from", String.format("KES %,.0f", lowest),
                event.getTicketTiers().size() + (event.getTicketTiers().size() == 1 ? " ticket type" : " ticket types")));
        column.add(leftAligned(facts));
        column.add(Box.createVerticalStrut(16));

        Map<String, String> details = event.getCustomDetails();
        if (!details.isEmpty()) {
            JPanel about = new JPanel(new GridLayout(0, 1, 0, 6));
            about.setOpaque(false);
            details.forEach((name, value) -> {
                JLabel line = new JLabel(name + ":  " + value);
                ViewStyles.styleSecondaryText(line, 13f);
                about.add(line);
            });
            column.add(leftAligned(EventAnalyticsPanel.card("Good to know", "Details from the host", about)));
            column.add(Box.createVerticalStrut(16));
        }

        column.add(leftAligned(buildDemandCard(event)));
        column.add(Box.createVerticalGlue());
        return column;
    }

    private JPanel fact(String label, String value, String hint) {
        JPanel fact = new JPanel(new GridLayout(3, 1, 0, 3));
        fact.setBackground(CARD);
        fact.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel caption = new JLabel(label);
        caption.setForeground(ChartPanel.MUTED);
        caption.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 11f));
        JLabel main = new JLabel(value);
        main.setForeground(ChartPanel.TEXT);
        main.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 14f));
        main.setToolTipText(value);
        JLabel note = new JLabel(hint);
        ViewStyles.styleSecondaryText(note, 11f);
        fact.add(caption);
        fact.add(main);
        fact.add(note);
        return fact;
    }

    // Shows guests how popular the event is: tickets per day for the last two weeks (no revenue).
    private JPanel buildDemandCard(Event event) {
        String id = event.getEventId();
        int[] daily = orderManager.getDailyTicketSales(id, DEMAND_DAYS);
        double[] values = new double[DEMAND_DAYS];
        String[] labels = new String[DEMAND_DAYS];
        int lastWeek = 0;
        for (int i = 0; i < DEMAND_DAYS; i++) {
            values[i] = daily[i];
            labels[i] = LocalDate.now().minusDays(DEMAND_DAYS - 1L - i).format(SHORT_DATE);
            if (i >= DEMAND_DAYS - 7) lastWeek += daily[i];
        }
        int sold = orderManager.calculateTotalTicketsSold(id);
        int capacity = event.getTicketTiers().stream().mapToInt(TicketTier::getInitialCapacity).sum();
        double soldShare = capacity == 0 ? 0 : (double) sold / capacity;

        ColumnChart chart = new ColumnChart(values, labels,
                i -> labels[i] + ": " + daily[i] + (daily[i] == 1 ? " ticket booked" : " tickets booked"),
                ChartPanel.SERIES[0], 3, false, 150);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        summary.setOpaque(false);
        summary.add(demandBadge(soldShare, lastWeek));
        JLabel numbers = new JLabel(String.format("   %,d tickets booked so far  ·  %,d in the last 7 days",
                sold, lastWeek));
        ViewStyles.styleSecondaryText(numbers, 12f);
        summary.add(numbers);
        body.add(summary, BorderLayout.NORTH);
        body.add(chart, BorderLayout.CENTER);
        return EventAnalyticsPanel.card("Ticket demand", "Tickets booked per day over the last two weeks", body);
    }

    // A short, honest label based on the real numbers: label text plus a coloured dot.
    private JPanel demandBadge(double soldShare, int lastWeek) {
        String text;
        Color color;
        if (soldShare >= 0.85) {
            text = "Almost sold out";
            color = SOLD_OUT;
        } else if (soldShare >= 0.6 || lastWeek >= 25) {
            text = "Selling fast";
            color = WARNING;
        } else {
            text = "Good availability";
            color = TEAL;
        }
        return badge(text, color);
    }

    private JPanel badge(String text, Color color) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        badge.setBackground(new Color(0x1E, 0x24, 0x33));
        badge.setBorder(BorderFactory.createLineBorder(BORDER));
        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(dot.getFont().deriveFont(10f));
        JLabel label = new JLabel(text);
        label.setForeground(ChartPanel.TEXT);
        label.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 11f));
        badge.add(dot);
        badge.add(label);
        return badge;
    }

    // ---- Right column: ticket tiers ----

    private JPanel buildTicketsColumn(Event event) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        boolean salesOpen = event.canPurchaseOn(LocalDate.now());
        double cheapestAvailable = event.getTicketTiers().stream().filter(tier -> tier.getAvailableSeats() > 0)
                .mapToDouble(TicketTier::getPrice).min().orElse(-1);
        for (TicketTier tier : event.getTicketTiers()) {
            list.add(leftAligned(buildTierRow(event, tier, salesOpen,
                    tier.getAvailableSeats() > 0 && tier.getPrice() == cheapestAvailable)));
            list.add(Box.createVerticalStrut(10));
        }
        if (!salesOpen) {
            JLabel closed = new JLabel("Ticket sales are currently closed for this event.");
            ViewStyles.styleSecondaryText(closed, 12f);
            list.add(leftAligned(closed));
        }
        JPanel card = EventAnalyticsPanel.card("Choose your tickets", "Prices are per person, in Kenyan shillings", list);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.NORTH);
        return wrap;
    }

    private JPanel buildTierRow(Event event, TicketTier tier, boolean salesOpen, boolean bestPrice) {
        JPanel row = new JPanel(new BorderLayout(12, 8));
        row.setBackground(new Color(0x1E, 0x24, 0x33));
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)));

        int left = tier.getAvailableSeats();
        int capacity = tier.getInitialCapacity();
        double leftShare = capacity == 0 ? 0 : (double) left / capacity;

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        heading.setOpaque(false);
        JLabel name = new JLabel(tier.getTierName() + "   ");
        ViewStyles.styleHeading(name, 15f);
        heading.add(name);
        if (left == 0) {
            heading.add(badge("Sold out", SOLD_OUT));
        } else if (leftShare <= 0.15) {
            heading.add(badge("Only " + left + " left", WARNING));
        } else if (bestPrice) {
            heading.add(badge("Best price", TEAL));
        }
        row.add(heading, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 6));
        middle.setOpaque(false);
        JLabel price = new JLabel(String.format("KES %,.0f", tier.getPrice()));
        price.setForeground(ChartPanel.TEXT);
        price.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 20f));
        middle.add(price, BorderLayout.NORTH);
        middle.add(new AvailabilityBar(leftShare), BorderLayout.CENTER);
        JLabel seats = new JLabel(left == 0 ? "No seats left" : String.format("%,d of %,d seats left", left, capacity));
        ViewStyles.styleSecondaryText(seats, 11f);
        middle.add(seats, BorderLayout.SOUTH);
        row.add(middle, BorderLayout.CENTER);

        JButton select = new JButton(left == 0 ? "Sold out" : "Select");
        boolean available = salesOpen && left > 0;
        if (available) {
            ViewStyles.stylePrimaryButton(select);
            select.addActionListener(action -> bookAction.accept(event, tier.getTierName()));
        } else {
            ViewStyles.styleSecondaryButton(select);
            select.setEnabled(false);
        }
        JPanel buttonWrap = new JPanel(new BorderLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.add(select, BorderLayout.SOUTH);
        row.add(buttonWrap, BorderLayout.EAST);
        return row;
    }

    // ---- Helpers ----

    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    private static String longDate(String date) {
        try {
            return LocalDate.parse(date).format(LONG_DATE);
        } catch (DateTimeParseException exception) {
            return date;
        }
    }

    /**
     * A slim meter showing the share of seats still available. The track is a dark step of
     * the same blue, so the meter reads as one object.
     *
     * OOP idea: a private nested subclass of JPanel that overrides paintComponent().
     */
    private static class AvailabilityBar extends JPanel {
        private final double fraction;

        AvailabilityBar(double fraction) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            setOpaque(false);
            setPreferredSize(new Dimension(160, 8));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = 6;
            int y = (getHeight() - height) / 2;
            g.setColor(new Color(0x10, 0x42, 0x81));
            g.fillRoundRect(0, y, getWidth(), height, height, height);
            g.setColor(fraction <= 0.15 ? WARNING : ChartPanel.SERIES[0]);
            g.setStroke(new BasicStroke(1f));
            int width = (int) Math.round(getWidth() * fraction);
            if (width > 0) g.fillRoundRect(0, y, Math.max(width, height), height, height, height);
            g.dispose();
        }
    }
}
