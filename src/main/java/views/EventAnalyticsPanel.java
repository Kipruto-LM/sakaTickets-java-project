package views;

import controllers.OrderManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import models.Booking;
import models.Event;
import models.TicketTier;

/**
 * The host's insights page for ONE event: headline numbers, sales charts, a tier breakdown
 * and the latest bookings.
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
    private static final int DAYS = 30;
    private static final Color PAGE = new Color(0x0F, 0x11, 0x17);
    private static final Color CARD = new Color(0x17, 0x1B, 0x24);
    private static final Color BORDER = new Color(0x2A, 0x30, 0x44);
    private static final Color GOOD = new Color(0x00, 0xE0, 0xA0);
    private static final Color BAD = new Color(0xFF, 0x4D, 0x6A);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH);

    private final OrderManager orderManager;
    private final Runnable backAction;
    private final Consumer<Event> editAction;
    private final Consumer<Event> exportAction;
    private final JPanel content = new ScrollableColumn();
    private final JScrollPane scroll;

    public EventAnalyticsPanel(OrderManager orderManager, Runnable backAction, Consumer<Event> editAction,
            Consumer<Event> exportAction) {
        this.orderManager = orderManager;
        this.backAction = backAction;
        this.editAction = editAction;
        this.exportAction = exportAction;
        setLayout(new BorderLayout());
        setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
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
        content.removeAll();
        addBlock(buildHeader(event));
        addBlock(buildStatTiles(event));
        addBlock(buildRow(new double[] {2.2, 1},
                buildCumulativeCard(event), buildSellThroughCard(event)));
        addBlock(buildDailyCard(event));
        addBlock(buildRow(new double[] {1.2, 1.2, 1},
                buildTierSeatsCard(event), buildTierRevenueCard(event), buildOrderSizeCard(event)));
        addBlock(buildBookingsCard(event));
        content.revalidate();
        content.repaint();
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
        dot.setForeground(switch (status) {
            case "ON_SALE" -> GOOD;
            case "SOLD_OUT", "CANCELLED" -> BAD;
            default -> new Color(0xF5, 0xB0, 0x41);
        });
        JLabel text = new JLabel(status.replace('_', ' '));
        text.setForeground(ChartPanel.TEXT);
        text.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        badge.add(dot);
        badge.add(text);
        return badge;
    }

    // ---- Headline numbers ----

    private JPanel buildStatTiles(Event event) {
        String id = event.getEventId();
        int capacity = capacity(event);
        int sold = orderManager.calculateTotalTicketsSold(id);
        double revenue = orderManager.calculateEventRevenue(id);
        int bookings = orderManager.getBookingsByEvent(id).size();
        double unsoldValue = event.getTicketTiers().stream()
                .mapToDouble(tier -> tier.getAvailableSeats() * tier.getPrice()).sum();
        int[] lastFortnight = orderManager.getDailyTicketSales(id, 14);
        int previousWeek = 0;
        int thisWeek = 0;
        for (int i = 0; i < 14; i++) {
            if (i < 7) previousWeek += lastFortnight[i]; else thisWeek += lastFortnight[i];
        }

        JPanel tiles = new JPanel(new GridLayout(1, 5, 12, 0));
        tiles.setOpaque(false);
        tiles.add(statTile("Tickets sold", String.format("%,d", sold),
                String.format("of %,d seats  ·  %,d left", capacity, Math.max(0, capacity - sold)), null));
        tiles.add(statTile("Revenue", "KES " + ChartPanel.compact(revenue),
                "KES " + ChartPanel.compact(unsoldValue) + " still on sale", null));
        tiles.add(statTile("Average order", bookings == 0 ? "–" : "KES " + ChartPanel.compact(revenue / bookings),
                bookings == 0 ? "No bookings yet" : String.format("%.1f tickets per booking", (double) sold / bookings),
                null));
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
        tiles.add(statTile("Event date", countdown(event.getDate()), longDate(event.getDate()), null));
        return tiles;
    }

    private JPanel statTile(String label, String value, String hint, Color hintColor) {
        JPanel tile = new JPanel(new GridLayout(3, 1, 0, 4));
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
        if (hintColor != null) note.setForeground(hintColor);
        tile.add(caption);
        tile.add(number);
        tile.add(note);
        return tile;
    }

    // ---- Charts ----

    private JPanel buildCumulativeCard(Event event) {
        String id = event.getEventId();
        int[] daily = orderManager.getDailyTicketSales(id, DAYS);
        int inWindow = 0;
        for (int count : daily) inWindow += count;
        // Tickets sold before the 30-day window started, so the line starts at the right height.
        int running = orderManager.calculateTotalTicketsSold(id) - inWindow;
        double[] totals = new double[DAYS];
        for (int i = 0; i < DAYS; i++) {
            running += daily[i];
            totals[i] = running;
        }
        String[] labels = dayLabels(DAYS);
        int capacity = capacity(event);
        LineChart chart = new LineChart(totals, labels,
                i -> String.format("%s: %,.0f sold in total (+%d that day)", labels[i], totals[i], daily[i]),
                ChartPanel.SERIES[0], 7, capacity, String.format("Capacity %,d", capacity), 230);
        return card("Tickets sold over time", "Running total for the last 30 days", chart);
    }

    private JPanel buildSellThroughCard(Event event) {
        int capacity = capacity(event);
        int sold = orderManager.calculateTotalTicketsSold(event.getEventId());
        double fraction = capacity == 0 ? 0 : (double) sold / capacity;
        RingMeter ring = new RingMeter(fraction, "of seats sold",
                String.format("%,d of %,d seats sold", sold, capacity), 230);
        return card("Sell-through", String.format("%,d of %,d seats", sold, capacity), ring);
    }

    private JPanel buildDailyCard(Event event) {
        int[] daily = orderManager.getDailyTicketSales(event.getEventId(), DAYS);
        double[] values = new double[DAYS];
        for (int i = 0; i < DAYS; i++) values[i] = daily[i];
        String[] labels = dayLabels(DAYS);
        ColumnChart chart = new ColumnChart(values, labels,
                i -> labels[i] + ": " + daily[i] + (daily[i] == 1 ? " ticket" : " tickets"),
                ChartPanel.SERIES[0], 5, false, 200);
        return card("Daily ticket sales", "Tickets sold per day for the last 30 days. Hover a day for details.", chart);
    }

    private JPanel buildTierSeatsCard(Event event) {
        List<TicketTier> tiers = event.getTicketTiers();
        String[] names = new String[tiers.size()];
        double[] sold = new double[tiers.size()];
        double[] capacity = new double[tiers.size()];
        for (int i = 0; i < tiers.size(); i++) {
            TicketTier tier = tiers.get(i);
            names[i] = tier.getTierName();
            sold[i] = tier.getInitialCapacity() - tier.getAvailableSeats();
            capacity[i] = tier.getInitialCapacity();
        }
        HorizontalBarChart chart = new HorizontalBarChart(names, sold, capacity,
                value -> String.format("%,.0f sold", value),
                i -> String.format("%s: %,.0f of %,.0f sold at KES %,.0f each", names[i], sold[i], capacity[i],
                        tiers.get(i).getPrice()));
        return card("Seats by tier", "Sold compared with each tier's capacity", chart);
    }

    private JPanel buildTierRevenueCard(Event event) {
        Map<String, Double> revenue = orderManager.getRevenueByTier(event);
        String[] names = revenue.keySet().toArray(String[]::new);
        double[] values = revenue.values().stream().mapToDouble(Double::doubleValue).toArray();
        double total = 0;
        for (double value : values) total += value;
        double finalTotal = total;
        HorizontalBarChart chart = new HorizontalBarChart(names, values, null,
                value -> "KES " + ChartPanel.compact(value),
                i -> String.format("%s: KES %,.0f (%.0f%% of revenue)", names[i], values[i],
                        finalTotal == 0 ? 0 : values[i] * 100 / finalTotal));
        return card("Revenue by tier", "Where the money comes from", chart);
    }

    private JPanel buildOrderSizeCard(Event event) {
        int[] counts = orderManager.getOrderSizeCounts(event.getEventId());
        double[] values = new double[counts.length];
        for (int i = 0; i < counts.length; i++) values[i] = counts[i];
        String[] labels = {"1", "2", "3", "4+"};
        ColumnChart chart = new ColumnChart(values, labels,
                i -> labels[i] + (i == 0 ? " ticket: " : " tickets: ") + counts[i] + " bookings",
                ChartPanel.SERIES[0], 1, true, 150);
        return card("Order size", "Bookings by number of tickets", chart);
    }

    // ---- Latest bookings table (the table view behind the charts) ----

    private JPanel buildBookingsCard(Event event) {
        List<Booking> recent = orderManager.getRecentBookings(event.getEventId(), 12);
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Receipt", "Guest", "Tier", "Qty", "Total (KES)", "Purchased"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        for (Booking booking : recent) {
            model.addRow(new Object[] {booking.getBookingId(), booking.getCustomerName(), booking.getTierName(),
                booking.getQuantity(), String.format("%,.0f", booking.getTotalPrice()),
                booking.getPurchasedAt().format(TIME)});
        }
        JTable table = new JTable(model);
        styleTable(table);
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

    private void styleTable(JTable table) {
        table.setBackground(CARD);
        table.setForeground(ChartPanel.TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setRowHeight(30);
        table.setFont(ViewStyles.BODY_FONT.deriveFont(12f));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setBackground(CARD);
        table.getTableHeader().setForeground(ChartPanel.MUTED);
        table.getTableHeader().setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        table.getTableHeader().setReorderingAllowed(false);
        // Numbers are right-aligned so their digits line up.
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);
        table.getColumnModel().getColumn(4).setCellRenderer(right);
    }

    // ---- Layout helpers ----

    // A titled card around any component: one method for every chart type (polymorphism).
    // Static and package-private so the dashboard overview can reuse it.
    static JPanel card(String title, String subtitle, JComponent body) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(15, 16, 14, 16)));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        ViewStyles.styleHeading(titleLabel, 14f);
        JLabel subtitleLabel = new JLabel(subtitle);
        ViewStyles.styleSecondaryText(subtitleLabel, 11f);
        heading.add(titleLabel);
        heading.add(subtitleLabel);
        card.add(heading, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // Lays cards side by side, sharing the width by the given weights; all cards get the same height.
    private JPanel buildRow(double[] weights, JComponent... cards) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        for (int i = 0; i < cards.length; i++) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i;
            constraints.weightx = weights[i];
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets(0, i == 0 ? 0 : 7, 0, i == cards.length - 1 ? 0 : 7);
            cards[i].setPreferredSize(new Dimension(1, cards[i].getPreferredSize().height));
            row.add(cards[i], constraints);
        }
        return row;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        ViewStyles.styleSecondaryButton(button);
        button.setFont(button.getFont().deriveFont(12f));
        return button;
    }

    // ---- Small calculations ----

    private static int capacity(Event event) {
        return event.getTicketTiers().stream().mapToInt(TicketTier::getInitialCapacity).sum();
    }

    private static String[] dayLabels(int days) {
        String[] labels = new String[days];
        LocalDate first = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) labels[i] = first.plusDays(i).format(SHORT_DATE);
        return labels;
    }

    private static String longDate(String date) {
        try {
            return LocalDate.parse(date).format(LONG_DATE);
        } catch (DateTimeParseException exception) {
            return date;
        }
    }

    static String countdown(String date) {
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date));
            if (days > 1) return "In " + days + " days";
            if (days == 1) return "Tomorrow";
            if (days == 0) return "Today";
            return Math.abs(days) + " days ago";
        } catch (DateTimeParseException exception) {
            return date;
        }
    }
}
