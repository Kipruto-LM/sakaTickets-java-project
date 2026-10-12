// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.OrderManager;            // Gives us the sales numbers
import java.awt.BorderLayout;               // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                      // An RGB colour
import java.awt.Dimension;                  // A width and height
import java.awt.Font;                       // Font style constants
import java.awt.GridLayout;                 // Layout with equal-sized cells
import java.util.ArrayList;                 // A resizable list
import java.util.List;                      // The general list type
import javax.swing.BorderFactory;           // Helper for making borders
import javax.swing.Box;                     // Invisible spacers
import javax.swing.BoxLayout;               // Stacks items in one column
import javax.swing.JButton;                 // A clickable button
import javax.swing.JLabel;                  // A piece of text
import javax.swing.JPanel;                  // A container
import javax.swing.JScrollPane;             // Adds scroll bars
import javax.swing.border.EmptyBorder;      // Invisible padding
import models.Event;                        // The event model
import models.TicketTier;                   // A ticket price level

/**
 * The dashboard's "Overview" page: a welcome line, three totals, a "Revenue by event" chart,
 * and a count of events on sale / sold out.
 *
 * OOP ideas shown here:
 * - Single responsibility: this class only builds and updates the Overview page. The other
 *   dashboard pages are their own classes (EventListPage, EventEditorPage, EventAnalyticsPanel).
 * - Encapsulation: the labels are private fields. Other classes update them only by calling
 *   refresh(), never by reaching in and changing the text themselves.
 * - Callbacks: the button runs openEventList, and AdminDashboardPanel decides what that does.
 */
class OverviewPage extends JPanel {
    private final OrderManager orderManager;
    // The numbers that change. They are fields so refresh() can update them.
    private final JLabel capacityValue = new JLabel("0");
    private final JLabel capacityHint = new JLabel(" ");
    private final JLabel soldValue = new JLabel("0");
    private final JLabel soldHint = new JLabel(" ");
    private final JLabel revenueValue = new JLabel("KES 0");
    private final JLabel onSaleCount = new JLabel("0");
    private final JLabel soldOutCount = new JLabel("0");
    private final JPanel chartArea = new JPanel(new BorderLayout());   // The chart is swapped in here

    // Constructor: builds the page once. refresh() fills in the numbers later.
    OverviewPage(String hostName, OrderManager orderManager, Runnable openEventList) {
        this.orderManager = orderManager;
        setLayout(new BorderLayout());
        setOpaque(false);

        // Everything is stacked in one column that can scroll.
        JPanel content = new ScrollableColumn();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome back, " + hostName + ".");
        ViewStyles.styleHeading(welcome, 18f);
        addBlock(content, welcome);

        // Row of three stat cards.
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.add(statCard("TOTAL CAPACITY", capacityValue, capacityHint));
        cards.add(statCard("TICKETS SOLD", soldValue, soldHint));
        cards.add(statCard("REVENUE", revenueValue, new JLabel("before platform fees")));
        addBlock(content, cards);

        // The chart card (filled in by refresh()).
        chartArea.setOpaque(false);
        addBlock(content, chartArea);

        // Bottom row: inventory counts on the left, a shortcut button on the right.
        JPanel status = new JPanel(new GridLayout(2, 2, 0, 8));
        status.setOpaque(false);
        status.add(secondaryLabel("Events on sale"));
        status.add(countLabel(onSaleCount, new Color(0x00, 0xE0, 0xA0)));   // Green
        status.add(secondaryLabel("Events sold out"));
        status.add(countLabel(soldOutCount, new Color(0xFF, 0x4D, 0x6A)));  // Red
        JButton open = new JButton("Open event inventory");
        ViewStyles.stylePrimaryButton(open);
        open.addActionListener(action -> openEventList.run());              // Callback
        JPanel buttonHolder = new JPanel(new BorderLayout());
        buttonHolder.setOpaque(false);
        buttonHolder.add(open, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        bottom.add(EventAnalyticsPanel.card("Inventory status", "Your active events", status));
        bottom.add(EventAnalyticsPanel.card("Event workspace", "Manage tiers, follow sales", buttonHolder));
        addBlock(content, bottom);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    // Updates every number and the chart from the host's events. Called whenever sales or events change.
    void refresh(List<Event> hostEvents) {
        // Only count events that aren't archived.
        List<Event> active = new ArrayList<>();
        for (Event event : hostEvents) {
            if (!event.isArchived()) {
                active.add(event);
            }
        }

        // Add up capacity, tickets sold and revenue, and count the statuses.
        int capacity = 0;
        int sold = 0;
        double revenue = 0;
        int onSale = 0;
        int soldOut = 0;
        for (Event event : active) {
            for (TicketTier tier : event.getTicketTiers()) {
                capacity += tier.getInitialCapacity();
            }
            sold += orderManager.calculateTotalTicketsSold(event.getEventId());
            revenue += orderManager.calculateEventRevenue(event.getEventId());
            if ("ON_SALE".equals(event.getStatus())) onSale++;
            if ("SOLD_OUT".equals(event.getStatus())) soldOut++;
        }
        int percentSold = capacity == 0 ? 0 : sold * 100 / capacity;

        // Show the results. %,d adds thousands commas, e.g. 1,230.
        capacityValue.setText(String.format("%,d", capacity));
        capacityHint.setText(active.size() + " active events");
        soldValue.setText(String.format("%,d", sold));
        soldHint.setText(percentSold + "% sell-through");
        revenueValue.setText(String.format("KES %,.0f", revenue));
        onSaleCount.setText(Integer.toString(onSale));
        soldOutCount.setText(Integer.toString(soldOut));

        // Rebuild the chart: one bar per active event, showing its revenue.
        chartArea.removeAll();
        if (!active.isEmpty()) {
            String[] names = new String[active.size()];
            double[] money = new double[active.size()];
            for (int i = 0; i < active.size(); i++) {
                names[i] = active.get(i).getTitle();
                money[i] = orderManager.calculateEventRevenue(active.get(i).getEventId());
            }
            // One blue bar per event, with "KES" before each number.
            HorizontalBarChart chart = new HorizontalBarChart(names, money, null, "KES ", "", ChartPanel.SERIES[0]);
            chartArea.add(EventAnalyticsPanel.card("Revenue by event", "Money taken by each active event", chart));
        }
        chartArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, chartArea.getPreferredSize().height));
        chartArea.revalidate();
        chartArea.repaint();
    }

    // ---- Small helpers ----

    // Adds a full-width block to the column, with a gap after it.
    private void addBlock(JPanel column, javax.swing.JComponent block) {
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));
        column.add(block);
        column.add(Box.createVerticalStrut(16));
    }

    // One stat card: caption, big blue number, small hint.
    private JPanel statCard(String caption, JLabel value, JLabel hint) {
        JPanel card = new JPanel(new GridLayout(3, 1, 0, 5));
        card.setBackground(ViewStyles.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.CARD_BORDER), new EmptyBorder(15, 16, 14, 16)));
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(ViewStyles.LABEL);
        captionLabel.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        value.setForeground(new Color(0x00, 0xB4, 0xFF));
        value.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 22f));
        ViewStyles.styleSecondaryText(hint, 11f);
        card.add(captionLabel);
        card.add(value);
        card.add(hint);
        return card;
    }

    // A grey label.
    private JLabel secondaryLabel(String text) {
        JLabel label = new JLabel(text);
        ViewStyles.styleSecondaryText(label, 13f);
        return label;
    }

    // Styles a count as a bold coloured number, right-aligned.
    private JLabel countLabel(JLabel label, Color color) {
        label.setForeground(color);
        label.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 16f));
        label.setHorizontalAlignment(JLabel.RIGHT);
        return label;
    }
}
