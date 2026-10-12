// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.OrderManager;                        // All the sales numbers come from here
import java.awt.BorderLayout;                           // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                                  // An RGB colour
import java.awt.Dimension;                              // A width and height
import java.awt.FlowLayout;                             // Layout that places items in a row
import java.awt.Font;                                   // Font style constants
import java.awt.GridBagConstraints;                     // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;                          // A flexible grid layout
import java.awt.GridLayout;                             // Layout with equal-sized cells
import java.awt.Insets;                                 // Spacing around an item
import java.time.LocalDate;                             // A date without a time
import java.time.format.DateTimeFormatter;              // Turns dates into text
import java.time.format.DateTimeParseException;         // Error thrown when text isn't a valid date
import java.time.temporal.ChronoUnit;                   // Counts days between two dates
import java.util.List;                                  // The general list type
import java.util.Locale;                                // Language rules (English month names)
import java.util.Map;                                   // The general map type
import java.util.function.Consumer;                     // An object holding an action that takes one value
import javax.swing.BorderFactory;                       // Helper for making borders
import javax.swing.Box;                                 // Invisible spacers
import javax.swing.BoxLayout;                           // Stacks items in one column or row
import javax.swing.JButton;                             // A clickable button
import javax.swing.JComponent;                          // The parent class of all Swing components
import javax.swing.JLabel;                              // A piece of text
import javax.swing.JPanel;                              // A container
import javax.swing.JScrollPane;                         // Adds scroll bars
import javax.swing.JTable;                              // A table with rows and columns
import javax.swing.SwingConstants;                      // Alignment constants such as RIGHT
import javax.swing.border.EmptyBorder;                  // Invisible padding
import javax.swing.table.DefaultTableCellRenderer;      // Controls how table cells look
import javax.swing.table.DefaultTableModel;             // Holds a table's data
import models.Booking;                                  // One purchase
import models.Event;                                    // The event being analysed
import models.TicketTier;                               // A ticket price level

/**
 * The host's insights page for ONE event: headline numbers, sales charts, a tier breakdown
 * and the latest bookings.
 *
 * Layout from top to bottom:
 *   1. Header       - back button, title, category/date/venue, status badge, Export and Edit buttons
 *   2. Five tiles   - tickets sold, revenue, average order, last 7 days, event date
 *   3. Two charts   - running total of tickets (line) + sell-through (ring)
 *   4. One chart    - tickets sold per day (columns)
 *   5. Three charts - seats by tier, revenue by tier, order size
 *   6. Table        - the 12 latest bookings
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel, so the dashboard can show it like any other page.
 * - Polymorphism: it builds several different chart objects (LineChart, ColumnChart,
 *   HorizontalBarChart, RingMeter) and places them all through the same card() method,
 *   which only needs to know each one is a JComponent.
 * - MVC: all the numbers come from the OrderManager controller. This View only lays them out.
 * - Callbacks: the Edit, Export and Back buttons run actions handed in by the dashboard.
 */
public class EventAnalyticsPanel extends JPanel {
    private static final int DAYS = 30;                                   // Charts cover the last 30 days
    // Colours used on this page.
    private static final Color PAGE = new Color(0x0F, 0x11, 0x17);       // Page background
    private static final Color CARD = new Color(0x17, 0x1B, 0x24);       // Card background
    private static final Color BORDER = new Color(0x2A, 0x30, 0x44);     // Card outline
    private static final Color GOOD = new Color(0x00, 0xE0, 0xA0);       // Green: good news
    private static final Color BAD = new Color(0xFF, 0x4D, 0x6A);        // Red: bad news
    // Date formats: "d MMM" -> "3 Oct"; "EEE d MMM yyyy" -> "Thu 15 Oct 2026"; "d MMM, HH:mm" -> "3 Oct, 19:35".
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH);

    private final OrderManager orderManager;
    private final Runnable backAction;                    // Callback: back to the events list
    private final Consumer<Event> editAction;             // Callback: open this event in the editor
    private final Consumer<Event> exportAction;           // Callback: export this event's bookings to CSV
    private final JPanel content = new ScrollableColumn(); // Everything on the page, stacked
    private final JScrollPane scroll;

    // Constructor: sets up an empty scrolling page. showEvent() fills it in later.
    public EventAnalyticsPanel(OrderManager orderManager, Runnable backAction, Consumer<Event> editAction,
            Consumer<Event> exportAction) {
        this.orderManager = orderManager;
        this.backAction = backAction;
        this.editAction = editAction;
        this.exportAction = exportAction;
        setLayout(new BorderLayout());
        setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));    // Stack blocks top to bottom
        content.setBackground(PAGE);
        content.setBorder(new EmptyBorder(0, 0, 12, 6));
        scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    // Public entry point: rebuilds the whole page for the chosen event.
    public void showEvent(Event event) {
        content.removeAll();                                   // Clear the previous event
        addBlock(buildHeader(event));
        addBlock(buildStatTiles(event));
        // Line chart gets 2.2 shares of the width, the ring gets 1.
        addBlock(buildRow(new double[] {2.2, 1},
                buildCumulativeCard(event), buildSellThroughCard(event)));
        addBlock(buildDailyCard(event));
        addBlock(buildRow(new double[] {1.2, 1.2, 1},
                buildTierSeatsCard(event), buildTierRevenueCard(event), buildOrderSizeCard(event)));
        addBlock(buildBookingsCard(event));
        content.revalidate();
        content.repaint();
        // After Swing has laid out the page, scroll back to the top.
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new java.awt.Point(0, 0)));
    }

    // Adds one full-width block to the page with a gap below it.
    private void addBlock(JComponent block) {
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));
        content.add(block);
        content.add(Box.createVerticalStrut(14));
    }

    // ---- Header ----

    private JPanel buildHeader(Event event) {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        // Left side, stacked: back button, title, then "category · date · venue".
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JButton back = secondaryButton("Back to all events");
        back.addActionListener(action -> backAction.run());
        back.setAlignmentX(LEFT_ALIGNMENT);
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 24f);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(12, 0, 4, 0));
        JLabel meta = new JLabel(event.getCategory() + "   ·   " + longDate(event.getDate()) + "   ·   "
                + event.getVenue());
        ViewStyles.styleSecondaryText(meta, 13f);
        meta.setAlignmentX(LEFT_ALIGNMENT);
        left.add(back);
        left.add(title);
        left.add(meta);
        header.add(left, BorderLayout.CENTER);

        // Right side, in a row: status badge, Export CSV, Edit event.
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(statusBadge(event.getStatus()));
        JButton export = secondaryButton("Export CSV");
        export.addActionListener(action -> exportAction.accept(event));
        JButton edit = new JButton("Edit event");
        ViewStyles.stylePrimaryButton(edit);
        edit.addActionListener(action -> editAction.accept(event));
        right.add(export);
        right.add(edit);
        // Put the row at the bottom of the right side so it lines up with the title area.
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(right, BorderLayout.SOUTH);
        header.add(rightWrap, BorderLayout.EAST);
        return header;
    }

    // Status shown as a coloured dot plus a text label, never colour alone.
    private JPanel statusBadge(String status) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        badge.setBackground(CARD);
        badge.setBorder(BorderFactory.createLineBorder(BORDER));
        JLabel dot = new JLabel("●");
        // Pick the dot colour from the status: green on sale, red sold out/cancelled, amber otherwise (paused).
        dot.setForeground(switch (status) {
            case "ON_SALE" -> GOOD;
            case "SOLD_OUT", "CANCELLED" -> BAD;
            default -> new Color(0xF5, 0xB0, 0x41);
        });
        JLabel text = new JLabel(status.replace('_', ' '));     // "ON_SALE" -> "ON SALE"
        text.setForeground(ChartPanel.TEXT);
        text.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        badge.add(dot);
        badge.add(text);
        return badge;
    }

    // ---- Headline numbers ----

    // The row of five stat tiles.
    private JPanel buildStatTiles(Event event) {
        String id = event.getEventId();
        int capacity = capacity(event);                                   // Total seats
        int sold = orderManager.calculateTotalTicketsSold(id);
        double revenue = orderManager.calculateEventRevenue(id);
        int bookings = orderManager.getBookingsByEvent(id).size();        // Number of separate purchases
        // Money still to be made: seats left x price, added up across tiers.
        double unsoldValue = event.getTicketTiers().stream()
                .mapToDouble(tier -> tier.getAvailableSeats() * tier.getPrice()).sum();
        // Split the last 14 days into the week before (days 0-6) and this week (days 7-13).
        int[] lastFortnight = orderManager.getDailyTicketSales(id, 14);
        int previousWeek = 0;
        int thisWeek = 0;
        for (int i = 0; i < 14; i++) {
            if (i < 7) previousWeek += lastFortnight[i]; else thisWeek += lastFortnight[i];
        }

        JPanel tiles = new JPanel(new GridLayout(1, 5, 12, 0));          // 1 row of 5 equal tiles
        tiles.setOpaque(false);
        // Tile 1: tickets sold, with "of 120 seats · 30 left" underneath.
        tiles.add(statTile("Tickets sold", String.format("%,d", sold),
                String.format("of %,d seats  ·  %,d left", capacity, Math.max(0, capacity - sold)), null));
        // Tile 2: revenue so far, and how much is still on sale.
        tiles.add(statTile("Revenue", "KES " + ChartPanel.compact(revenue),
                "KES " + ChartPanel.compact(unsoldValue) + " still on sale", null));
        // Tile 3: average spend per booking ("–" if there are none, to avoid dividing by zero).
        tiles.add(statTile("Average order", bookings == 0 ? "–" : "KES " + ChartPanel.compact(revenue / bookings),
                bookings == 0 ? "No bookings yet" : String.format("%.1f tickets per booking", (double) sold / bookings),
                null));
        // Tile 4: this week vs last week, as a percentage change in green (up) or red (down).
        String change;
        Color changeColor = null;
        if (previousWeek == 0) {
            change = thisWeek == 0 ? "No sales in two weeks" : "New sales this week";
        } else {
            long percent = Math.round((thisWeek - previousWeek) * 100.0 / previousWeek);
            change = (percent >= 0 ? "+" : "-") + Math.abs(percent) + "% vs previous 7 days";
            changeColor = percent >= 0 ? GOOD : BAD;
        }
        tiles.add(statTile("Last 7 days", String.format("%,d tickets", thisWeek), change, changeColor));
        // Tile 5: how long until the event, e.g. "In 3 days".
        tiles.add(statTile("Event date", countdown(event.getDate()), longDate(event.getDate()), null));
        return tiles;
    }

    // One stat tile: small caption, big number, small hint (optionally coloured).
    private JPanel statTile(String label, String value, String hint, Color hintColor) {
        JPanel tile = new JPanel(new GridLayout(3, 1, 0, 4));             // 3 rows
        tile.setBackground(CARD);
        tile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(13, 15, 13, 15)));
        JLabel caption = new JLabel(label);
        caption.setForeground(ChartPanel.MUTED);
        caption.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 11f));
        JLabel number = new JLabel(value);
        number.setForeground(ChartPanel.TEXT);
        number.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 21f));
        JLabel note = new JLabel(hint);
        ViewStyles.styleSecondaryText(note, 11f);
        if (hintColor != null) note.setForeground(hintColor);              // Only tile 4 uses a colour
        tile.add(caption);
        tile.add(number);
        tile.add(note);
        return tile;
    }

    // ---- Charts ----

    // Line chart: total tickets sold so far, day by day, with the capacity line on top.
    private JPanel buildCumulativeCard(Event event) {
        String id = event.getEventId();
        int[] daily = orderManager.getDailyTicketSales(id, DAYS);
        int inWindow = 0;
        for (int count : daily) inWindow += count;                         // Tickets sold in the 30 days
        // Tickets sold before the 30-day window started, so the line starts at the right height.
        int running = orderManager.calculateTotalTicketsSold(id) - inWindow;
        double[] totals = new double[DAYS];
        for (int i = 0; i < DAYS; i++) {
            running += daily[i];                                           // Add each day's sales
            totals[i] = running;                                           // Running total so far
        }
        String[] labels = dayLabels(DAYS);
        int capacity = capacity(event);
        // A date label every 7 days, capacity reference line, 230px tall.
        LineChart chart = new LineChart(totals, labels,
                i -> String.format("%s: %,.0f sold in total (+%d that day)", labels[i], totals[i], daily[i]),
                ChartPanel.SERIES[0], 7, capacity, String.format("Capacity %,d", capacity), 230);
        return card("Tickets sold over time", "Running total for the last 30 days", chart);
    }

    // Ring: share of all seats that have been sold.
    private JPanel buildSellThroughCard(Event event) {
        int capacity = capacity(event);
        int sold = orderManager.calculateTotalTicketsSold(event.getEventId());
        double fraction = capacity == 0 ? 0 : (double) sold / capacity;
        RingMeter ring = new RingMeter(fraction, "of seats sold",
                String.format("%,d of %,d seats sold", sold, capacity), 230);
        return card("Sell-through", String.format("%,d of %,d seats", sold, capacity), ring);
    }

    // Columns: tickets sold on each of the last 30 days.
    private JPanel buildDailyCard(Event event) {
        int[] daily = orderManager.getDailyTicketSales(event.getEventId(), DAYS);
        double[] values = new double[DAYS];
        for (int i = 0; i < DAYS; i++) values[i] = daily[i];               // int[] -> double[] for the chart
        String[] labels = dayLabels(DAYS);
        ColumnChart chart = new ColumnChart(values, labels,
                i -> labels[i] + ": " + daily[i] + (daily[i] == 1 ? " ticket" : " tickets"),
                ChartPanel.SERIES[0], 5, false, 200);
        return card("Daily ticket sales", "Tickets sold per day for the last 30 days. Hover a day for details.", chart);
    }

    // Meters: for each tier, seats sold compared with the tier's capacity.
    private JPanel buildTierSeatsCard(Event event) {
        List<TicketTier> tiers = event.getTicketTiers();
        // Build three matching arrays: position i holds tier i's name, sold count and capacity.
        String[] names = new String[tiers.size()];
        double[] sold = new double[tiers.size()];
        double[] capacity = new double[tiers.size()];
        for (int i = 0; i < tiers.size(); i++) {
            TicketTier tier = tiers.get(i);
            names[i] = tier.getTierName();
            sold[i] = tier.getInitialCapacity() - tier.getAvailableSeats();   // Seats taken so far
            capacity[i] = tier.getInitialCapacity();
        }
        HorizontalBarChart chart = new HorizontalBarChart(names, sold, capacity,
                value -> String.format("%,.0f sold", value),
                i -> String.format("%s: %,.0f of %,.0f sold at KES %,.0f each", names[i], sold[i], capacity[i],
                        tiers.get(i).getPrice()));
        return card("Seats by tier", "Sold compared with each tier's capacity", chart);
    }

    // Bars: money taken by each tier.
    private JPanel buildTierRevenueCard(Event event) {
        Map<String, Double> revenue = orderManager.getRevenueByTier(event);
        String[] names = revenue.keySet().toArray(String[]::new);                       // Tier names
        double[] values = revenue.values().stream().mapToDouble(Double::doubleValue).toArray();   // Their revenue
        double total = 0;
        for (double value : values) total += value;
        // Lambdas can only use variables that never change after being set, so copy total into a final one.
        double finalTotal = total;
        HorizontalBarChart chart = new HorizontalBarChart(names, values, null,
                value -> "KES " + ChartPanel.compact(value),
                i -> String.format("%s: KES %,.0f (%.0f%% of revenue)", names[i], values[i],
                        finalTotal == 0 ? 0 : values[i] * 100 / finalTotal));
        return card("Revenue by tier", "Where the money comes from", chart);
    }

    // Columns: how many bookings bought 1, 2, 3 or 4+ tickets.
    private JPanel buildOrderSizeCard(Event event) {
        int[] counts = orderManager.getOrderSizeCounts(event.getEventId());
        double[] values = new double[counts.length];
        for (int i = 0; i < counts.length; i++) values[i] = counts[i];
        String[] labels = {"1", "2", "3", "4+"};
        // labelEvery = 1 (show every label), labelEveryColumn = true (value on every column).
        ColumnChart chart = new ColumnChart(values, labels,
                i -> labels[i] + (i == 0 ? " ticket: " : " tickets: ") + counts[i] + " bookings",
                ChartPanel.SERIES[0], 1, true, 150);
        return card("Order size", "Bookings by number of tickets", chart);
    }

    // ---- Latest bookings table (the table view behind the charts) ----

    private JPanel buildBookingsCard(Event event) {
        List<Booking> recent = orderManager.getRecentBookings(event.getEventId(), 12);   // Newest 12
        // The table's data. An anonymous subclass overrides isCellEditable() so nobody can type in cells.
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Receipt", "Guest", "Tier", "Qty", "Total (KES)", "Purchased"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        // One table row per booking.
        for (Booking booking : recent) {
            model.addRow(new Object[] {booking.getBookingId(), booking.getCustomerName(), booking.getTierName(),
                booking.getQuantity(), String.format("%,.0f", booking.getTotalPrice()),
                booking.getPurchasedAt().format(TIME)});
        }
        JTable table = new JTable(model);
        styleTable(table);
        // Without a scroll pane, a table's header must be added by hand above it.
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(table.getTableHeader(), BorderLayout.NORTH);
        holder.add(table, BorderLayout.CENTER);
        if (recent.isEmpty()) {
            JLabel none = new JLabel("No bookings yet.");
            ViewStyles.styleSecondaryText(none, 12f);
            holder.add(none, BorderLayout.SOUTH);
        }
        int total = orderManager.getBookingsByEvent(event.getEventId()).size();
        return card("Latest bookings", "Showing the " + recent.size() + " most recent of " + total
                + " bookings. Export CSV for the full list.", holder);
    }

    // Dark styling for the bookings table.
    private void styleTable(JTable table) {
        table.setBackground(CARD);
        table.setForeground(ChartPanel.TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);                  // Only horizontal lines between rows
        table.setRowHeight(30);
        table.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
        table.setFocusable(false);                          // Read-only: no keyboard focus
        table.setRowSelectionAllowed(false);                // Clicking doesn't highlight rows
        table.getTableHeader().setBackground(CARD);
        table.getTableHeader().setForeground(ChartPanel.MUTED);
        table.getTableHeader().setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        table.getTableHeader().setReorderingAllowed(false); // Columns can't be dragged around
        // Numbers are right-aligned so their digits line up.
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);    // Qty
        table.getColumnModel().getColumn(4).setCellRenderer(right);    // Total
    }

    // ---- Layout helpers ----

    // A titled card around any component: one method for every chart type (polymorphism).
    // Static and package-private so the dashboard overview can reuse it.
    static JPanel card(String title, String subtitle, JComponent body) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(15, 16, 14, 16)));
        // Title and subtitle stacked at the top.
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        ViewStyles.styleHeading(titleLabel, 14f);
        JLabel subtitleLabel = new JLabel(subtitle);
        ViewStyles.styleSecondaryText(subtitleLabel, 11f);
        heading.add(titleLabel);
        heading.add(subtitleLabel);
        card.add(heading, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);                // The chart (or table) fills the rest
        return card;
    }

    // Lays cards side by side, sharing the width by the given weights; all cards get the same height.
    // "JComponent... cards" (varargs) accepts any number of cards.
    private JPanel buildRow(double[] weights, JComponent... cards) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        for (int i = 0; i < cards.length; i++) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i;                              // Column i
            constraints.weightx = weights[i];                   // Share of the width
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;         // Stretch to fill (same height for all)
            // 7px on each inner side, giving a 14px gap between cards (none at the outer edges).
            constraints.insets = new Insets(0, i == 0 ? 0 : 7, 0, i == cards.length - 1 ? 0 : 7);
            // Width 1 = "no width of my own", so only the weights decide the widths.
            cards[i].setPreferredSize(new Dimension(1, cards[i].getPreferredSize().height));
            row.add(cards[i], constraints);
        }
        return row;
    }

    // A smaller secondary-style button.
    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        ViewStyles.styleSecondaryButton(button);
        button.setFont(button.getFont().deriveFont(12f));
        return button;
    }

    // ---- Small calculations ----

    // Total seats across all tiers.
    private static int capacity(Event event) {
        return event.getTicketTiers().stream().mapToInt(TicketTier::getInitialCapacity).sum();
    }

    // Labels for the last `days` days, oldest first, e.g. {"13 Sep", ..., "12 Oct"}.
    private static String[] dayLabels(int days) {
        String[] labels = new String[days];
        LocalDate first = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) labels[i] = first.plusDays(i).format(SHORT_DATE);
        return labels;
    }

    // Turns "2026-10-15" into "Thu 15 Oct 2026". If the text isn't a valid date, returns it unchanged.
    private static String longDate(String date) {
        try {
            return LocalDate.parse(date).format(LONG_DATE);
        } catch (DateTimeParseException exception) {
            return date;
        }
    }

    // Friendly countdown to a date: "In 3 days", "Tomorrow", "Today" or "2 days ago".
    // Package-private (no "private") so EventDetailsPanel can use it too.
    static String countdown(String date) {
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date));
            if (days > 1) return "In " + days + " days";
            if (days == 1) return "Tomorrow";
            if (days == 0) return "Today";
            return Math.abs(days) + " days ago";             // Negative = in the past
        } catch (DateTimeParseException exception) {
            return date;
        }
    }
}
