// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.OrderManager;                    // Gives us the sales numbers
import java.awt.BorderLayout;                       // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                              // An RGB colour
import java.awt.Dimension;                          // A width and height
import java.awt.FlowLayout;                         // Layout that places items in a row
import java.awt.Font;                               // Font style constants
import java.awt.GridBagConstraints;                 // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;                      // A flexible grid layout
import java.awt.GridLayout;                         // Layout with equal-sized cells
import java.awt.Insets;                             // Spacing around an item
import java.time.LocalDate;                         // A date without a time
import java.time.format.DateTimeFormatter;          // Turns dates into text like "Saturday 24 October 2026"
import java.time.format.DateTimeParseException;     // Error thrown when text isn't a valid date
import java.util.Locale;                            // Language rules (English month names)
import java.util.Map;                               // The general map (key -> value) type
import java.util.function.BiConsumer;               // An object holding an action that takes two values
import javax.swing.BorderFactory;                   // Helper for making borders
import javax.swing.Box;                             // Invisible spacers
import javax.swing.BoxLayout;                       // Stacks items in one column or row
import javax.swing.JButton;                         // A clickable button
import javax.swing.JComponent;                      // The parent class of all Swing components
import javax.swing.JLabel;                          // A piece of text
import javax.swing.JPanel;                          // A container
import javax.swing.JProgressBar;                    // A bar that fills up
import javax.swing.JScrollPane;                     // Adds scroll bars
import javax.swing.border.EmptyBorder;              // Invisible padding
import models.Event;                                // The event being shown
import models.TicketTier;                           // A ticket price level

/**
 * The guest's page for one event: a big banner, the key details, how popular the event is,
 * and a card for each ticket tier with its price and how many seats are left.
 *
 * Layout from top to bottom:
 *   1. Top bar      - "Back to events" button and the SakaTickets name.
 *   2. Banner       - the event's picture, full width.
 *   3. Two columns  - left: title, When/Where/Price facts, host details, demand chart;
 *                     right: one row per ticket tier with a Select button.
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel, so MainFrame can show it as one of its pages.
 * - Reuse through objects: it uses the same ColumnChart class as the admin pages, just with
 *   guest-friendly data (tickets per day, never revenue).
 * - Callbacks: choosing a tier calls bookAction (a BiConsumer of event + tier name), and
 *   MainFrame opens checkout with that tier already selected.
 */
public class EventDetailsPanel extends JPanel {
    // Colours used on this page.
    private static final Color PAGE = ViewStyles.BACKGROUND;               // Page background
    private static final Color CARD = new Color(0x17, 0x1B, 0x24);         // Card background
    private static final Color BORDER = new Color(0x2A, 0x30, 0x44);       // Card outline
    private static final Color TEAL = new Color(0x00, 0xE5, 0xC4);         // "Good" / category colour
    private static final Color WARNING = new Color(0xF5, 0xB0, 0x41);      // Amber: few seats left
    private static final Color SOLD_OUT = new Color(0xFF, 0x4D, 0x6A);     // Red: sold out
    // Date formats: "EEEE d MMMM yyyy" -> "Saturday 24 October 2026"; "d MMM" -> "24 Oct".
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final int DEMAND_DAYS = 14;      // The demand chart covers the last 14 days

    private final OrderManager orderManager;
    private final Runnable backAction;                     // Callback: back to the catalogue
    private final BiConsumer<Event, String> bookAction;    // Callback: open checkout with (event, tier name)
    private final JPanel content = new ScrollableColumn(); // Everything on the page, stacked vertically
    private final JScrollPane scroll;                      // Lets the page scroll up and down

    // Constructor: sets up the empty scrolling page. showEvent() fills it in later.
    public EventDetailsPanel(OrderManager orderManager, Runnable backAction, BiConsumer<Event, String> bookAction) {
        this.orderManager = orderManager;
        this.backAction = backAction;
        this.bookAction = bookAction;
        setLayout(new BorderLayout());
        setBackground(PAGE);
        setBorder(new EmptyBorder(20, 30, 16, 30));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));    // Stack blocks top to bottom
        content.setBackground(PAGE);
        content.setBorder(new EmptyBorder(0, 0, 10, 8));
        scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);            // Scroll speed
        add(scroll, BorderLayout.CENTER);
    }

    // Public entry point: rebuilds the page for one event.
    public void showEvent(Event event) {
        content.removeAll();                                   // Clear the previous event's page
        if (event == null) {
            // The event no longer exists: show a short message.
            JLabel missing = new JLabel("This event is no longer available.");
            ViewStyles.styleHeading(missing, 18f);
            addBlock(buildTopBar());
            addBlock(missing);
        } else {
            addBlock(buildTopBar());
            JPanel banner = EventVisuals.event(event, 260);   // 260px-tall picture
            banner.setPreferredSize(new Dimension(1, 260));       // Width 1 = let the layout decide
            addBlock(banner);
            addBlock(buildColumns(event));
        }
        content.revalidate();
        content.repaint();
        // After Swing has laid out the page, scroll back to the top.
        scroll.getVerticalScrollBar().setValue(0);             // Start at the top of the page
    }

    // Adds one full-width block to the page, followed by a 16px gap.
    private void addBlock(JComponent block) {
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));   // Any width, own height
        content.add(block);
        content.add(Box.createVerticalStrut(16));
    }

    // The top bar: back button on the left, brand name on the right.
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
        // Left column rules: column 0, gets 1.5 shares of the width, stretches both ways.
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = 1.5;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.anchor = GridBagConstraints.NORTH;
        left.insets = new Insets(0, 0, 0, 9);
        // Right column rules: a copy of the left rules, changed to column 1 with 1 share.
        GridBagConstraints right = (GridBagConstraints) left.clone();
        right.gridx = 1;
        right.weightx = 1;
        right.insets = new Insets(0, 9, 0, 0);
        JPanel info = buildInfoColumn(event);
        JPanel tickets = buildTicketsColumn(event);
        // Preferred width 1 means "no width of my own", so the weights alone decide the split (60% / 40%).
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

        // Category (small teal capitals) and the big title.
        JLabel category = new JLabel(event.getCategory().toUpperCase(Locale.ROOT));
        category.setForeground(TEAL);
        category.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 11f));
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 30f);
        title.setBorder(new EmptyBorder(4, 0, 12, 0));
        column.add(leftAligned(category));
        column.add(leftAligned(title));

        // Three fact boxes in a row: When, Where, Tickets from.
        JPanel facts = new JPanel(new GridLayout(1, 3, 12, 0));
        facts.setOpaque(false);
        facts.add(fact("When", longDate(event.getDate()), EventAnalyticsPanel.countdown(event.getDate())));
        facts.add(fact("Where", event.getVenue(), "Venue"));
        // Cheapest tier that still has seats (or the cheapest overall if all are sold out).
        double lowest = event.getLowestPrice();
        // "1 ticket type" or "3 ticket types" - singular or plural.
        facts.add(fact("Tickets from", String.format("KES %,.0f", lowest),
                event.getTicketTiers().size() + (event.getTicketTiers().size() == 1 ? " ticket type" : " ticket types")));
        column.add(leftAligned(facts));
        column.add(Box.createVerticalStrut(16));

        // "Good to know" card, only if the host added custom details.
        Map<String, String> details = event.getCustomDetails();
        if (!details.isEmpty()) {
            JPanel about = new JPanel(new GridLayout(0, 1, 0, 6));    // One line per detail
            about.setOpaque(false);
            details.forEach((name, value) -> {                         // Runs once per name/value pair
                JLabel line = new JLabel(name + ":  " + value);
                ViewStyles.styleSecondaryText(line, 13f);
                about.add(line);
            });
            column.add(leftAligned(EventAnalyticsPanel.card("Good to know", "Details from the host", about)));
            column.add(Box.createVerticalStrut(16));
        }

        column.add(leftAligned(buildDemandCard(event)));
        column.add(Box.createVerticalGlue());                          // Push everything to the top
        return column;
    }

    // One small fact box: a grey caption, a bold value and a grey hint, stacked.
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
        main.setToolTipText(value);              // Hover shows the full text if it gets cut off
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
        int[] daily = orderManager.getDailyTicketSales(id, DEMAND_DAYS);   // Tickets per day
        double[] values = new double[DEMAND_DAYS];                          // The chart wants doubles
        String[] labels = new String[DEMAND_DAYS];                          // Date label for each day
        int lastWeek = 0;                                                   // Tickets in the last 7 days
        for (int i = 0; i < DEMAND_DAYS; i++) {
            values[i] = daily[i];
            labels[i] = LocalDate.now().minusDays(DEMAND_DAYS - 1L - i).format(SHORT_DATE);
            if (i >= DEMAND_DAYS - 7) lastWeek += daily[i];                 // Only count the last 7 entries
        }
        int sold = orderManager.calculateTotalTicketsSold(id);
        int capacity = event.getCapacity();
        double soldShare = capacity == 0 ? 0 : (double) sold / capacity;   // e.g. 0.75 = 75% sold

        // The chart: blue columns, a date label every 3 days, 150px tall.
        ColumnChart chart = new ColumnChart(values, labels, ChartPanel.SERIES[0], 3, false, 150);

        // Above the chart: the demand badge and a one-line summary.
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
        if (soldShare >= 0.85) {                         // 85% or more sold
            text = "Almost sold out";
            color = SOLD_OUT;
        } else if (soldShare >= 0.6 || lastWeek >= 25) { // 60%+ sold, or 25+ tickets this week
            text = "Selling fast";
            color = WARNING;
        } else {
            text = "Good availability";
            color = TEAL;
        }
        return badge(text, color);
    }

    // A small rounded tag: a coloured dot followed by text.
    private JPanel badge(String text, Color color) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        badge.setBackground(new Color(0x1E, 0x24, 0x33));
        badge.setBorder(BorderFactory.createLineBorder(BORDER));
        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(dot.getFont().deriveFont(10f));
        JLabel label = new JLabel(text);
        label.setForeground(ChartPanel.TEXT);           // Text stays white; only the dot is coloured
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
        boolean salesOpen = event.canPurchase();
        // Price of the cheapest tier that still has seats, for the "Best price" badge.
        double cheapestAvailable = event.getLowestPrice();
        // One row per tier, with a 10px gap after each.
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
        // Wrapping in BorderLayout.NORTH keeps the card at the top instead of stretching it tall.
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.NORTH);
        return wrap;
    }

    // One tier row: name + badge on top, price + seats bar in the middle, Select button on the right.
    private JPanel buildTierRow(Event event, TicketTier tier, boolean salesOpen, boolean bestPrice) {
        JPanel row = new JPanel(new BorderLayout(12, 8));
        row.setBackground(new Color(0x1E, 0x24, 0x33));
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)));

        int left = tier.getAvailableSeats();                      // Seats still free
        int capacity = tier.getInitialCapacity();                 // Seats the tier started with
        double leftShare = capacity == 0 ? 0 : (double) left / capacity;   // e.g. 0.1 = 10% left

        // Tier name, plus at most one badge.
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        heading.setOpaque(false);
        JLabel name = new JLabel(tier.getTierName() + "   ");
        ViewStyles.styleHeading(name, 15f);
        heading.add(name);
        if (left == 0) {
            heading.add(badge("Sold out", SOLD_OUT));
        } else if (leftShare <= 0.15) {                           // 15% or fewer seats left
            heading.add(badge("Only " + left + " left", WARNING));
        } else if (bestPrice) {
            heading.add(badge("Best price", TEAL));
        }
        row.add(heading, BorderLayout.NORTH);

        // Price, the seats-left bar and a seats-left line.
        JPanel middle = new JPanel(new BorderLayout(0, 6));
        middle.setOpaque(false);
        JLabel price = new JLabel(String.format("KES %,.0f", tier.getPrice()));
        price.setForeground(ChartPanel.TEXT);
        price.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 20f));
        middle.add(price, BorderLayout.NORTH);
        // A progress bar showing how many seats are left (0 to 100%).
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) (leftShare * 100));
        bar.setPreferredSize(new Dimension(160, 6));
        bar.setForeground(leftShare <= 0.15 ? WARNING : ChartPanel.SERIES[0]);   // Amber when nearly gone
        middle.add(bar, BorderLayout.CENTER);
        JLabel seats = new JLabel(left == 0 ? "No seats left" : String.format("%,d of %,d seats left", left, capacity));
        ViewStyles.styleSecondaryText(seats, 11f);
        middle.add(seats, BorderLayout.SOUTH);
        row.add(middle, BorderLayout.CENTER);

        // The button: blue "Select" if it can be bought, otherwise grey and disabled.
        JButton select = new JButton(left == 0 ? "Sold out" : "Select");
        boolean available = salesOpen && left > 0;
        if (available) {
            ViewStyles.stylePrimaryButton(select);
            // Callback: MainFrame opens checkout with this event and tier.
            select.addActionListener(action -> bookAction.accept(event, tier.getTierName()));
        } else {
            ViewStyles.styleSecondaryButton(select);
            select.setEnabled(false);
        }
        JPanel buttonWrap = new JPanel(new BorderLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.add(select, BorderLayout.SOUTH);               // Button sits at the bottom of the row
        row.add(buttonWrap, BorderLayout.EAST);
        return row;
    }

    // ---- Helpers ----

    // Lines a component up on the left and lets it stretch sideways (but not taller).
    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    // Turns "2026-10-24" into "Saturday 24 October 2026". If the text isn't a valid date, returns it unchanged.
    private static String longDate(String date) {
        try {
            return LocalDate.parse(date).format(LONG_DATE);
        } catch (DateTimeParseException exception) {
            return date;
        }
    }
}
