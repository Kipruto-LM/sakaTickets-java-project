// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.EventManager;              // Events (read, copy, archive)
import controllers.OrderManager;              // Bookings and sales numbers
import java.awt.BorderLayout;                 // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.CardLayout;                   // Shows one "card" at a time
import java.awt.Color;                        // An RGB colour
import java.awt.Dimension;                    // A width and height
import java.awt.FlowLayout;                   // Layout that places items in a row
import java.awt.Font;                         // Font style constants
import java.awt.GridBagConstraints;           // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;                // A flexible grid layout
import java.awt.GridLayout;                   // Layout with equal-sized cells
import java.awt.Insets;                       // Spacing around an item
import java.io.File;                          // A file on disk
import java.io.FileWriter;                    // Writes text into a file
import java.io.IOException;                   // Error thrown when writing a file fails
import java.util.List;                        // The general list type
import java.util.Locale;                      // Language rules for lower-casing
import java.util.function.Consumer;           // An object holding an action that takes one value
import javax.swing.BorderFactory;             // Helper for making borders
import javax.swing.JButton;                   // A clickable button
import javax.swing.JCheckBox;                 // A tick box
import javax.swing.JComboBox;                 // A drop-down list
import javax.swing.JFileChooser;              // A pop-up file picker
import javax.swing.JLabel;                    // A piece of text
import javax.swing.JMenuItem;                 // One item in a pop-up menu
import javax.swing.JOptionPane;               // Pop-up message and question boxes
import javax.swing.JPanel;                    // A container
import javax.swing.JPopupMenu;                // A menu that pops up
import javax.swing.JProgressBar;              // A bar that fills up
import javax.swing.JScrollPane;               // Adds scroll bars
import javax.swing.JTable;                    // A table with rows and columns
import javax.swing.JTextField;                // A one-line text box
import javax.swing.border.EmptyBorder;        // Invisible padding
import javax.swing.table.DefaultTableModel;   // Holds a table's data
import models.Booking;                        // One purchase
import models.Event;                          // The event model
import models.TicketTier;                     // A ticket price level

/**
 * The dashboard's "Manage Events" page: a filter bar and one row per event, with buttons to
 * open insights, edit, view attendees, export a CSV, duplicate, archive or delete.
 *
 * Under the filter bar, a CardLayout shows one of three "cards":
 *   "empty"    - the host has no events yet (with a button to create one)
 *   "filtered" - no events match the filters
 *   "table"    - column titles plus one row per matching event
 *
 * OOP ideas shown here:
 * - Single responsibility: this class only lists events and runs the row actions.
 * - Callbacks: Edit, Insights and "Create" are handed in as actions (Consumer / Runnable), so this
 *   page never needs to know about the editor or insights classes.
 */
class EventListPage extends JPanel {
    // Column titles, and the share of the row's width each column gets (they add up to 1).
    // Both the title row and every event row use these, so the columns line up.
    private static final String[] COLUMNS = {"EVENT NAME", "DATE", "CAPACITY", "SOLD", "REMAINING", "STATUS", "ACTION"};
    private static final double[] WEIGHTS = {.27, .11, .09, .16, .09, .10, .18};

    private final EventManager eventManager;
    private final OrderManager orderManager;
    private final String hostId;                    // Only this host's events are listed
    private final Consumer<Event> editAction;       // Callback: open an event in the editor
    private final Consumer<Event> insightsAction;   // Callback: open an event's insights page
    private final Runnable changedAction;           // Callback: something changed, refresh the dashboard

    // Filter controls.
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {"All statuses", "ON_SALE", "PAUSED", "SOLD_OUT", "CANCELLED"});
    private final JCheckBox archivedToggle = new JCheckBox("Show archived");
    // The rows, and the CardLayout that switches between "empty", "filtered" and "table".
    private final JPanel rows = new JPanel();
    private final CardLayout stateLayout = new CardLayout();
    private final JPanel states = new JPanel(stateLayout);

    // Constructor: builds the filter bar and the three cards. refresh() fills in the rows.
    EventListPage(EventManager eventManager, OrderManager orderManager, String hostId,
            Consumer<Event> editAction, Consumer<Event> insightsAction, Runnable createAction, Runnable changedAction) {
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.hostId = hostId;
        this.editAction = editAction;
        this.insightsAction = insightsAction;
        this.changedAction = changedAction;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        add(buildFilters(), BorderLayout.NORTH);

        // Card 1: no events yet.
        JPanel empty = new JPanel(new GridBagLayout());          // GridBagLayout centres its content
        empty.setBackground(ViewStyles.CARD);
        JPanel message = new JPanel(new GridLayout(3, 1, 0, 10));
        message.setOpaque(false);
        JLabel emptyTitle = new JLabel("You haven't created any events yet.", JLabel.CENTER);
        ViewStyles.styleHeading(emptyTitle, 21f);
        JLabel hint = new JLabel("Start with an event and build your ticket tiers.", JLabel.CENTER);
        ViewStyles.styleSecondaryText(hint, 14f);
        JButton create = new JButton("+ Create Your First Event");
        ViewStyles.stylePrimaryButton(create);
        create.addActionListener(action -> createAction.run());
        message.add(emptyTitle);
        message.add(hint);
        message.add(create);
        empty.add(message);
        states.add(empty, "empty");

        // Card 2: nothing matches the filters.
        JPanel filtered = new JPanel(new GridBagLayout());
        filtered.setBackground(ViewStyles.CARD);
        JLabel filteredMessage = new JLabel("No events match these filters.");
        ViewStyles.styleSecondaryText(filteredMessage, 14f);
        filtered.add(filteredMessage);
        states.add(filtered, "filtered");

        // Card 3: column titles on top, scrolling rows underneath.
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        rows.setBackground(ViewStyles.CARD);
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createLineBorder(ViewStyles.CARD_BORDER));
        scroll.getViewport().setBackground(ViewStyles.CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel table = new JPanel(new BorderLayout());
        table.setBackground(ViewStyles.CARD);
        table.add(buildColumnTitles(), BorderLayout.NORTH);
        table.add(scroll, BorderLayout.CENTER);
        states.add(table, "table");
        add(states, BorderLayout.CENTER);
    }

    // Rebuilds the rows using the current filters, and shows the right card.
    void refresh() {
        rows.removeAll();
        List<Event> hostEvents = eventManager.getEventsByHostId(hostId);
        int shown = 0;
        for (Event event : hostEvents) {
            if (matchesFilters(event)) {
                rows.add(buildRow(event));
                shown++;
            }
        }
        // Pick the card: no events at all, none matching, or the table.
        if (hostEvents.isEmpty()) {
            stateLayout.show(states, "empty");
        } else if (shown == 0) {
            stateLayout.show(states, "filtered");
        } else {
            stateLayout.show(states, "table");
        }
        rows.revalidate();
        rows.repaint();
    }

    // True if an event passes all three filters: archived, status and search text.
    private boolean matchesFilters(Event event) {
        if (event.isArchived() && !archivedToggle.isSelected()) {
            return false;                                  // Hide archived events unless ticked
        }
        String status = (String) statusFilter.getSelectedItem();
        if (!"All statuses".equals(status) && !event.getStatus().equals(status)) {
            return false;                                  // Wrong status
        }
        // The search text must appear somewhere in the title, venue, category or date.
        String search = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String text = (event.getTitle() + " " + event.getVenue() + " " + event.getCategory() + " "
                + event.getDate()).toLowerCase(Locale.ROOT);
        return search.isEmpty() || text.contains(search);
    }

    // The filter bar. Pressing Enter, changing the status or ticking the box all refresh the list.
    private JPanel buildFilters() {
        ViewStyles.styleInput(searchField);
        searchField.setToolTipText("Search by title, venue, category or date");
        archivedToggle.setOpaque(false);
        archivedToggle.setForeground(ViewStyles.FOREGROUND);
        JButton apply = ViewStyles.smallButton("Search");
        apply.addActionListener(action -> refresh());
        searchField.addActionListener(action -> refresh());
        statusFilter.addActionListener(action -> refresh());
        archivedToggle.addActionListener(action -> refresh());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(apply);
        filters.add(statusFilter);
        filters.add(archivedToggle);
        return filters;
    }

    // The row of column titles.
    private JPanel buildColumnTitles() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(ViewStyles.CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ViewStyles.CARD_BORDER));   // Line underneath
        for (int i = 0; i < COLUMNS.length; i++) {
            JLabel label = new JLabel(COLUMNS[i]);
            label.setForeground(ViewStyles.LABEL);
            label.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
            label.setBorder(new EmptyBorder(12, 10, 12, 6));
            header.add(label, column(i));
        }
        return header;
    }

    // One event row: name/category, date, capacity, sold (+ bar), remaining, status, buttons.
    private JPanel buildRow(Event event) {
        int capacity = 0;
        for (TicketTier tier : event.getTicketTiers()) {
            capacity += tier.getInitialCapacity();
        }
        int sold = orderManager.calculateTotalTicketsSold(event.getEventId());
        int remaining = Math.max(0, capacity - sold);                       // Never below 0
        int percent = capacity == 0 ? 0 : Math.min(100, sold * 100 / capacity);

        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(ViewStyles.CARD);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E, 0x24, 0x33)));

        // Column 0: title with the category under it.
        JPanel name = new JPanel(new GridLayout(2, 1, 0, 3));
        name.setOpaque(false);
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 12f);
        JLabel category = new JLabel(event.getCategory());
        ViewStyles.styleSecondaryText(category, 10f);
        name.add(title);
        name.add(category);
        addCell(row, name, 0);
        addCell(row, smallText(event.getDate(), ViewStyles.LABEL), 1);
        addCell(row, smallText(Integer.toString(capacity), ViewStyles.FOREGROUND), 2);

        // Column 3: tickets sold with a thin progress bar (red when full).
        JPanel sales = new JPanel(new GridLayout(2, 1, 0, 3));
        sales.setOpaque(false);
        sales.add(smallText(Integer.toString(sold), ViewStyles.FOREGROUND));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setPreferredSize(new Dimension(90, 4));
        bar.setBorderPainted(false);
        bar.setBackground(ViewStyles.CARD_BORDER);
        bar.setForeground(percent >= 100 ? new Color(0xFF, 0x4D, 0x6A) : new Color(0x00, 0xB4, 0xFF));
        sales.add(bar);
        addCell(row, sales, 3);

        // Column 4: remaining seats, red at zero and green otherwise.
        addCell(row, smallText(Integer.toString(remaining),
                remaining == 0 ? new Color(0xFF, 0x4D, 0x6A) : new Color(0x00, 0xE0, 0xA0)), 4);
        // Column 5: status, e.g. "ON SALE".
        JLabel status = smallText(event.getStatus().replace('_', ' '), statusColor(event.getStatus()));
        status.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        addCell(row, status, 5);
        // Column 6: the buttons.
        addCell(row, buildActions(event), 6);
        return row;
    }

    // The buttons at the end of a row. "···" opens a small menu with more actions.
    private JPanel buildActions(Event event) {
        JButton insights = ViewStyles.smallButton("Insights");
        insights.addActionListener(action -> insightsAction.accept(event));
        JButton edit = ViewStyles.smallButton("Edit");
        edit.addActionListener(action -> editAction.accept(event));

        JPopupMenu menu = new JPopupMenu();
        JMenuItem attendees = new JMenuItem("View attendees");
        attendees.addActionListener(action -> showAttendees(event));
        JMenuItem export = new JMenuItem("Export CSV");
        export.addActionListener(action -> exportAttendees(event));
        JMenuItem duplicate = new JMenuItem("Duplicate event");
        duplicate.addActionListener(action -> duplicateEvent(event));
        JMenuItem archive = new JMenuItem(event.isArchived() ? "Restore event" : "Archive event");
        archive.addActionListener(action -> toggleArchive(event));
        JMenuItem delete = new JMenuItem("Delete event");
        delete.addActionListener(action -> deleteEvent(event));
        menu.add(attendees);
        menu.add(export);
        menu.add(duplicate);
        menu.add(archive);
        menu.addSeparator();                       // A thin line before the dangerous option
        menu.add(delete);
        JButton more = ViewStyles.smallButton("···");
        more.addActionListener(action -> menu.show(more, 0, more.getHeight()));   // Menu appears under the button

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        actions.setOpaque(false);
        actions.add(insights);
        actions.add(edit);
        actions.add(more);
        return actions;
    }

    // ---- Row actions ----

    // Pops up a read-only table of everyone who booked this event.
    private void showAttendees(Event event) {
        // The table's column titles, and 0 rows to start with.
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Receipt", "Guest", "Email", "Tier", "Qty", "Total (KES)"}, 0);
        for (Booking booking : orderManager.getBookingsByEvent(event.getEventId())) {
            model.addRow(new Object[] {booking.getBookingId(), booking.getCustomerName(), booking.getCustomerEmail(),
                booking.getTierName(), booking.getQuantity(), booking.getTotalPrice()});
        }
        JTable table = new JTable(model);
        table.setDefaultEditor(Object.class, null);         // Turn off editing: the table is read-only
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(780, 330));
        JOptionPane.showMessageDialog(this, scroll, event.getTitle() + " attendees", JOptionPane.PLAIN_MESSAGE);
    }

    // Saves this event's bookings as a CSV file the host can open in Excel or Google Sheets.
    // Package-private so the insights page's "Export CSV" button can use it too.
    void exportAttendees(Event event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(event.getTitle() + " attendees.csv"));   // Suggested file name
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;                                                         // The host cancelled
        }
        try {
            FileWriter writer = new FileWriter(chooser.getSelectedFile());       // Opens (or creates) the file
            writer.write("Receipt,Guest,Email,Tier,Quantity,Total KES\n");   // Header line
            for (Booking booking : orderManager.getBookingsByEvent(event.getEventId())) {
                writer.write(csv(booking.getBookingId()) + "," + csv(booking.getCustomerName()) + ","
                        + csv(booking.getCustomerEmail()) + "," + csv(booking.getTierName()) + ","
                        + booking.getQuantity() + "," + booking.getTotalPrice() + "\n");
            }
            writer.close();                                                  // Finish writing the file
            JOptionPane.showMessageDialog(this, "Attendee CSV exported.", "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Could not export the CSV: " + exception.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Wraps a value in quotes for CSV, doubling any quotes inside it so commas and quotes are safe.
    private String csv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    // Copies an event (the controller makes the copy), then opens the copy in the editor.
    private void duplicateEvent(Event event) {
        Event copy = eventManager.duplicateEvent(event);
        changedAction.run();
        editAction.accept(copy);
    }

    // Archives (hides) or restores an event after the host confirms.
    private void toggleArchive(Event event) {
        boolean archive = !event.isArchived();             // The opposite of its current state
        String question = archive ? "Archive this event? It will no longer appear in the public catalog."
                : "Restore this event to your dashboard and the public catalog?";
        int answer = JOptionPane.showConfirmDialog(this, question, "Confirm event change", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            eventManager.setArchived(event, archive);
            changedAction.run();
        }
    }

    // Deletes an event for good, after the host confirms. Its bookings are deleted too,
    // so the warning says how many there are. (Archive is the safer choice: it can be undone.)
    private void deleteEvent(Event event) {
        int bookings = orderManager.getBookingsByEvent(event.getEventId()).size();
        String question = "Delete \"" + event.getTitle() + "\" for good?";
        if (bookings > 0) {
            question += "\nIts " + bookings + " bookings will be deleted too. This cannot be undone."
                    + "\n(Tip: use \"Archive event\" instead to just hide it.)";
        }
        int answer = JOptionPane.showConfirmDialog(this, question, "Delete event",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            eventManager.deleteEvent(event);   // Removes it from the database
            orderManager.reload();                           // Our list of bookings is now out of date
            changedAction.run();                             // Refresh the dashboard
        }
    }

    // ---- Small helpers ----

    // Adds a cell to a row in the given column, with the same padding for every cell.
    private void addCell(JPanel row, javax.swing.JComponent cell, int index) {
        cell.setBorder(new EmptyBorder(10, 10, 10, 6));
        row.add(cell, column(index));
    }

    // Layout rules for a column: its position, its share of the width, stretch sideways, stick left.
    private GridBagConstraints column(int index) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = index;
        constraints.weightx = WEIGHTS[index];
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        return constraints;
    }

    // Small monospace text in the given colour (used for numbers and dates).
    private JLabel smallText(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(ViewStyles.DATA_FONT.deriveFont(Font.PLAIN, 11f));
        return label;
    }

    // Text colour for each status.
    private Color statusColor(String status) {
        if ("ON_SALE".equals(status)) return new Color(0x1F, 0x8A, 0x55);     // Green
        if ("SOLD_OUT".equals(status)) return new Color(0xB8, 0x3A, 0x3A);    // Red
        if ("CANCELLED".equals(status)) return new Color(0x73, 0x36, 0x36);   // Dark red
        return new Color(0x8A, 0x67, 0x24);                                   // Amber (paused)
    }
}
