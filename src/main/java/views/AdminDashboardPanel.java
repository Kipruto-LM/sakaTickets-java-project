// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.EventManager;  // Holds all events
import controllers.OrderManager;  // Holds all bookings and sales numbers
import java.awt.BorderLayout;  // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;  // An RGB colour
import java.awt.Dimension;  // A width and height
import java.awt.FlowLayout;  // Layout that places items in a row
import java.awt.Font;  // Font style constants
import java.awt.GridBagConstraints;  // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;  // A flexible grid layout
import java.awt.Insets;  // Spacing around an item: top, left, bottom, right
import java.awt.GridLayout;  // Layout with equal-sized cells
import java.io.IOException;  // Error thrown when writing a file fails
import java.nio.charset.StandardCharsets;  // Text encodings such as UTF-8
import java.nio.file.Files;  // Helpers for reading and writing files
import java.util.LinkedHashMap;  // A map that keeps insertion order
import java.time.LocalDate;  // A date without a time
import java.time.ZoneId;  // A time zone (needed to convert old-style Date objects)
import java.time.format.DateTimeParseException;  // Error thrown when text isn't a valid date
import java.util.ArrayList;  // A resizable list
import java.util.Date;  // The older Java date type that JDateChooser uses
import java.util.List;  // The general list type
import java.util.Map;  // The general map type
import java.util.UUID;  // Random unique IDs for new events
import javax.swing.BorderFactory;  // Helper for making borders
import javax.swing.JCheckBox;  // A tick box
import javax.swing.JColorChooser;  // A pop-up colour picker
import javax.swing.DefaultListModel;  // Holds the items shown in a JList
import javax.swing.JButton;  // A clickable button
import javax.swing.JFileChooser;  // A pop-up file picker
import javax.swing.JComboBox;  // A drop-down list
import javax.swing.JLabel;  // A piece of text
import javax.swing.JList;  // A scrollable list of items
import javax.swing.JMenuItem;  // One item in a pop-up menu
import javax.swing.JOptionPane;  // Pop-up message and question boxes
import javax.swing.JPanel;  // A container
import javax.swing.JPopupMenu;  // A menu that pops up (the "···" menu)
import javax.swing.JProgressBar;  // A bar that fills up (sales progress)
import javax.swing.JScrollPane;  // Adds scroll bars
import javax.swing.JSpinner;  // A number box with up/down arrows
import javax.swing.JTable;  // A table with rows and columns
import javax.swing.JTextField;  // A one-line text box
import javax.swing.ListSelectionModel;  // Rules for selecting items in a JList
import javax.swing.SpinnerNumberModel;  // Value, minimum, maximum and step for a JSpinner
import javax.swing.filechooser.FileNameExtensionFilter;  // Limits the file picker to certain file types
import javax.swing.border.EmptyBorder;  // Invisible padding
import javax.swing.table.DefaultTableModel;  // Holds a table's data
import com.toedter.calendar.JDateChooser;  // A date picker with a calendar (outside library from pom.xml)
import models.Event;  // The event model
import models.TicketTier;  // A ticket price level

/**
 * The host's workspace: sales stats, the event list, and the form for creating or editing events.
 *
 * Layout: a sidebar on the left (logo, menu, account) and a workspace on the right with a top bar
 * and one of these "pages" (switched with a CardLayout):
 *   Overview      - totals, two charts, inventory status
 *   Manage Events - search/filters and one row per event
 *   Insights      - EventAnalyticsPanel for one event
 *   Editor        - the create/edit event form
 *   Settings      - account details
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel.
 * - MVC: this View works with Event and TicketTier objects (Model), and gets and saves them
 *   through EventManager and OrderManager (Controllers).
 * - Encapsulation: the screen's state (form fields, the event being edited, the active page)
 *   is in private fields. Only refreshEvents() is public.
 * - Abstraction by splitting work up: the screen is built from many small private
 *   build...() methods, each making one part of the page.
 * - Records and anonymous classes are used near the bottom of the file (CustomField, the table model).
 */
public class AdminDashboardPanel extends JPanel {
    // The statuses a host can pick for an event.
    private static final String[] EVENT_STATUSES = {"ON_SALE", "PAUSED", "CANCELLED", "SOLD_OUT"};

    // Dependencies passed in through the constructor (dependency injection).
    private final EventManager eventManager;  // Events (read and save)
    private final OrderManager orderManager;  // Bookings and sales numbers
    private final Runnable logoutAction;  // Callback: log out and go back to the welcome page
    private final String hostId;  // Which host is logged in (only their events are shown)
    // UI components: kept as fields so every method in this class can reach them.
    private final JPanel eventList = new JPanel();  // Holds one row per event on the Manage Events page
    private final JPanel eventState = new JPanel(new java.awt.CardLayout());  // Switches between the event table, "no events" and "no matches"
    private final java.awt.CardLayout pageLayout = new java.awt.CardLayout();  // Switches between the dashboard pages
    private final JPanel pages = new JPanel(pageLayout);  // Holds every page
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();  // Sidebar buttons, by page name
    private final JLabel pageTitle = new JLabel("Active Events Inventory");  // Title in the top bar
    private final JLabel pageDescription = new JLabel("Manage and monitor your events");  // Line under the title
    // The three Overview stat cards: a big value and a small hint each.
    private final JLabel capacityValue = new JLabel("0");
    private final JLabel soldValue = new JLabel("0");
    private final JLabel revenueValue = new JLabel("KES 0");
    private final JLabel capacityHint = new JLabel("across active events");
    private final JLabel soldHint = new JLabel("0% sell-through");
    private final JLabel revenueHint = new JLabel("before platform fees");
    private final JButton createEventButton = new JButton("+  Create New Event");  // Top-right button
    private String activePage = "Manage Events";  // Name of the page currently showing
    // --- Event editor form fields ---
    private final JTextField titleField = new JTextField(28);
    private final JDateChooser dateChooser = createDateChooser();
    private final JTextField venueField = new JTextField(28);
    private final JDateChooser salesStartChooser = createDateChooser();
    private final JDateChooser salesEndChooser = createDateChooser();
    private final JLabel validationLabel = new JLabel(" ");  // Red error message on the form
    private final JTextField categoryField = new JTextField(28);
    private final JTextField bannerPathField = new JTextField(22);  // Path of the chosen banner picture
    private final JTextField customNameField = new JTextField(12);  // New custom detail: name
    private final JTextField customValueField = new JTextField(18);  // New custom detail: value
    private final JTextField tierNameField = new JTextField(12);  // Tier editor: name
    private final JSpinner priceSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 1.0, 100000000.0, 100.0));  // Tier price: starts 1000, min 1, steps of 100
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));  // Tier seats: starts 50, min 1
    private final JComboBox<String> statusBox = new JComboBox<>(EVENT_STATUSES);  // Event status drop-down
    private final DefaultListModel<TicketTier> tierModel = new DefaultListModel<>();  // The tiers being edited...
    private final JList<TicketTier> tierList = new JList<>(tierModel);  // ...and the list that shows them
        private final DefaultListModel<CustomField> customFieldModel = new DefaultListModel<>();  // The custom details being edited...
        private final JList<CustomField> customFieldList = new JList<>(customFieldModel);  // ...and the list that shows them
    private final JButton tierActionButton = new JButton("Add Tier");  // Says "Add Tier" or "Update Tier"
        private final JButton accentColorButton = new JButton("Choose accent color");  // Opens the colour picker
        // --- Manage Events filters ---
        private final JTextField searchField = new JTextField(18);
        private final JTextField dateFromFilter = new JTextField(10);  // yyyy-mm-dd
        private final JTextField dateToFilter = new JTextField(10);  // yyyy-mm-dd
        private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {"All statuses", "ON_SALE", "PAUSED", "SOLD_OUT", "CANCELLED"});
        private final JCheckBox archivedToggle = new JCheckBox("Show archived");  // Include archived events?
        private Color selectedAccent = ViewStyles.ACCENT;  // Accent colour chosen in the editor
    // The event currently open in the form, or null when creating a new one.
    private Event editingEvent;
    // The per-event insights page, and the overview charts that are rebuilt whenever sales change.
    private EventAnalyticsPanel insightsPanel;
    private final JPanel overviewCharts = new JPanel(new GridBagLayout());

    // Static helper: it doesn't use any fields, so it belongs to the class, not to an object.
    private static JDateChooser createDateChooser() {
        JDateChooser chooser = new JDateChooser();  // A date box with a calendar button
        chooser.setDateFormatString("yyyy-MM-dd");  // Show dates as 2026-10-15
        chooser.setPreferredSize(new Dimension(260, 36));
        chooser.setDate(null);  // Start empty
        return chooser;
    }

    // Constructor: stores the dependencies, then builds the screen.
    public AdminDashboardPanel(EventManager eventManager, OrderManager orderManager, Runnable logoutAction,
            String hostId, String hostName) {
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.logoutAction = logoutAction;
        this.hostId = hostId;
        // Build every part of the screen, then fill in the event list and numbers.
        buildPanel(hostName);
        refreshEvents();
    }

    // The only public method: rebuilds the event list using the current search and filters.
    public void refreshEvents() {
        eventList.removeAll();  // Clear the old rows
        List<Event> allEvents = eventManager.getEventsByHostId(hostId);  // Every event this host owns
        String query = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);  // Search text, lower-case
        String selectedStatus = (String) statusFilter.getSelectedItem();  // Chosen status filter
        // Keep only the events that pass every filter, one .filter() step per rule:
        //  1. archived events only if "Show archived" is ticked
        //  2. the chosen status (or all)
        //  3. the search text appears in the title, venue, category or date
        //  4. the date is inside the From/To range
        List<Event> events = allEvents.stream()
            .filter(event -> archivedToggle.isSelected() || !event.isArchived())
            .filter(event -> "All statuses".equals(selectedStatus) || event.getStatus().equals(selectedStatus))
            .filter(event -> query.isEmpty() || (event.getTitle() + " " + event.getVenue() + " "
                + event.getCategory() + " " + event.getDate()).toLowerCase(java.util.Locale.ROOT).contains(query))
            .filter(this::matchesDateRange)
            .toList();
        for (Event event : events) eventList.add(createInventoryRow(event));  // One row per matching event
        updateSummary(allEvents);  // Update the three Overview stat cards
        rebuildOverviewCharts(allEvents);  // Redraw the Overview charts
        // Show the right card: "empty" (no events at all), "filtered" (none match) or the table.
        java.awt.CardLayout layout = (java.awt.CardLayout) eventState.getLayout();
        layout.show(eventState, allEvents.isEmpty() ? "empty" : events.isEmpty() ? "filtered" : "eventsTable");
        eventList.revalidate();
        eventList.repaint();
    }

    // Builds the whole screen: sidebar on the left, workspace (top bar + pages) on the right.
    private void buildPanel(String hostName) {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0F, 0x11, 0x17));
        add(buildSidebar(hostName), BorderLayout.WEST);  // Sidebar down the left edge

        JPanel workspace = new JPanel(new BorderLayout(0, 18));  // Everything to the right of the sidebar
        workspace.setBackground(new Color(0x0F, 0x11, 0x17));
        workspace.setBorder(new EmptyBorder(24, 28, 24, 28));
        workspace.add(buildTopBar(), BorderLayout.NORTH);  // Page title + "Create New Event" button

        pages.setBackground(new Color(0x0F, 0x11, 0x17));
        // Add every page to the CardLayout, each with a name used by showPage().
        pages.add(buildOverviewPage(hostName), "Overview");
        pages.add(buildManageEventsPage(), "Manage Events");
        pages.add(buildSettingsPage(hostName), "Settings");
        pages.add(buildManagementForm(), "Editor");
        // The insights page gets three callbacks: Back, Edit (loadEvent) and Export (exportAttendees).
        // "this::loadEvent" is a method reference: it hands our own method over as an object.
        insightsPanel = new EventAnalyticsPanel(orderManager, () -> showPage("Manage Events"),
                this::loadEvent, this::exportAttendees);
        pages.add(insightsPanel, "Insights");
        workspace.add(pages, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        showPage("Overview");  // Start on the Overview page
    }

    // Builds the left sidebar: logo at the top, menu buttons in the middle, account + log out at the bottom.
    private JPanel buildSidebar(String name) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(224, 0));  // 224px wide (height is decided by the layout)
        sidebar.setBackground(new Color(0x17, 0x1B, 0x24));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x2A, 0x30, 0x44)));  // 1px line on the right edge only

        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 5));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(24, 18, 24, 14));
        JLabel eyebrow = new JLabel("ADMIN PANEL");
        eyebrow.setForeground(new Color(0x88, 0x96, 0xB3));
        eyebrow.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        JLabel logo = new JLabel("SakaTickets");
        ViewStyles.styleHeading(logo, 16f);
        brand.add(eyebrow);
        brand.add(logo);
        sidebar.add(brand, BorderLayout.NORTH);

        // The menu: one button per page, stacked vertically.
        JPanel navigation = new JPanel();
        navigation.setLayout(new javax.swing.BoxLayout(navigation, javax.swing.BoxLayout.Y_AXIS));
        navigation.setOpaque(false);
        navigation.setBorder(new EmptyBorder(0, 8, 10, 8));
        for (String item : List.of("Overview", "Manage Events", "Settings")) {  // List.of makes a fixed list of the three page names
            JButton button = new JButton(item);
            button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);  // Text on the left of the button
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(11, 13, 11, 8));
            button.setOpaque(true);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setPreferredSize(new Dimension(196, 44));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.addActionListener(action -> showPage(item));  // Clicking shows that page
            navButtons.put(item, button);  // Remember it so showPage() can highlight it
            navigation.add(button);
            navigation.add(javax.swing.Box.createVerticalStrut(5));
        }
        sidebar.add(navigation, BorderLayout.CENTER);

        // Account box: a coloured square with the host's first letter, their name, and a Log out button.
        JPanel account = new JPanel(new BorderLayout(9, 0));
        account.setOpaque(false);
        account.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(15, 15, 18, 12)));
        JLabel avatar = new JLabel(name.isBlank() ? "A" : name.substring(0, 1).toUpperCase());  // First letter of the name ("A" if empty)
        avatar.setOpaque(true);
        avatar.setBackground(new Color(0x00, 0xB4, 0xFF));
        avatar.setForeground(Color.WHITE);
        avatar.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(30, 30));
        JLabel accountName = new JLabel(name);
        ViewStyles.styleHeading(accountName, 12f);
        JLabel accountLabel = new JLabel("Event host");
        ViewStyles.styleSecondaryText(accountLabel, 10f);
        JPanel accountText = new JPanel(new GridLayout(2, 1, 0, 2));
        accountText.setOpaque(false);
        accountText.add(accountName);
        accountText.add(accountLabel);
        account.add(avatar, BorderLayout.WEST);
        account.add(accountText, BorderLayout.CENTER);
        JButton logout = smallButton("Log out");
        logout.addActionListener(action -> logoutAction.run());  // Callback to MainFrame
        account.add(logout, BorderLayout.SOUTH);
        sidebar.add(account, BorderLayout.SOUTH);
        return sidebar;
    }

    // Builds the top bar: page title and description on the left, a "LIVE" tag and the main button on the right.
    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(14, 0));
        top.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 3));
        labels.setOpaque(false);
        ViewStyles.styleHeading(pageTitle, 20f);
        ViewStyles.styleSecondaryText(pageDescription, 12f);
        labels.add(pageTitle);
        labels.add(pageDescription);
        top.add(labels, BorderLayout.WEST);
        ViewStyles.stylePrimaryButton(createEventButton);
        // The same button does two jobs: in the editor it says "Back to events", elsewhere it starts a new event.
        createEventButton.addActionListener(action -> {
            if ("Editor".equals(activePage)) {
                showPage("Manage Events");
            } else {
                startNewEvent();
            }
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JPanel liveTag = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        liveTag.setOpaque(false);
        JLabel liveDot = new JLabel("●");
        liveDot.setForeground(new Color(0x00, 0xE0, 0xA0));
        liveDot.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        JLabel liveText = new JLabel("HOST STUDIO  /  LIVE");
        liveText.setForeground(new Color(0x88, 0x96, 0xB3));
        liveText.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        liveTag.add(liveDot);
        liveTag.add(liveText);
        right.add(liveTag);
        right.add(createEventButton);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    // Builds the Overview page, top to bottom: welcome text, three stat cards, two charts,
    // then "Inventory status" and "Event workspace" boxes. The page scrolls if the window is short.
    private JPanel buildOverviewPage(String name) {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        JPanel overview = new JPanel(new BorderLayout(0, 6));
        overview.setOpaque(false);
        JLabel welcome = new JLabel("Welcome back, " + name + ".");  // e.g. "Welcome back, Talel."
        ViewStyles.styleHeading(welcome, 18f);
        JLabel note = new JLabel("Your events, ticket sales and host tools are ready.");
        ViewStyles.styleSecondaryText(note, 12f);
        overview.add(welcome, BorderLayout.NORTH);
        overview.add(note, BorderLayout.SOUTH);
        overview.setAlignmentX(LEFT_ALIGNMENT);
        overview.setMaximumSize(new Dimension(Integer.MAX_VALUE, overview.getPreferredSize().height));
        JPanel content = new ScrollableColumn();  // Stretches to the page width, scrolls vertically
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        JPanel summaryCards = buildSummaryCards();
        summaryCards.setAlignmentX(LEFT_ALIGNMENT);
        content.add(overview);
        content.add(javax.swing.Box.createVerticalStrut(17));
        content.add(summaryCards);
        content.add(javax.swing.Box.createVerticalStrut(17));
        overviewCharts.setOpaque(false);
        overviewCharts.setAlignmentX(LEFT_ALIGNMENT);
        content.add(overviewCharts);  // Filled in by rebuildOverviewCharts()
        content.add(javax.swing.Box.createVerticalStrut(17));
        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));  // Two equal boxes side by side
        lower.setOpaque(false);
        lower.setAlignmentX(LEFT_ALIGNMENT);
        lower.setPreferredSize(new Dimension(900, 210));
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        lower.add(buildStatusSummary());
        lower.add(buildOverviewAction());
        lower.setMinimumSize(new Dimension(0, 210));
        content.add(lower);
        content.add(javax.swing.Box.createVerticalGlue());  // Spare space goes at the bottom
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    // The row of three stat cards on the Overview page. Their labels are fields, so updateSummary() can change the numbers.
    private JPanel buildSummaryCards() {
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(900, 128));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
        cards.add(createStatCard("TOTAL CAPACITY", capacityValue, capacityHint));
        cards.add(createStatCard("TICKETS SOLD", soldValue, soldHint));
        cards.add(createStatCard("EST. REVENUE", revenueValue, revenueHint));
        return cards;
    }

    // One stat card: caption, big blue number, small hint.
    private JPanel createStatCard(String label, JLabel value, JLabel hint) {
        JPanel card = new JPanel(new GridLayout(3, 1, 0, 5));
        card.setBackground(new Color(0x17, 0x1B, 0x24));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(15, 16, 14, 16)));
        JLabel caption = new JLabel(label);
        caption.setForeground(new Color(0x88, 0x96, 0xB3));
        caption.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 10f));
        value.setForeground(new Color(0x00, 0xB4, 0xFF));
        value.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 22f));
        ViewStyles.styleSecondaryText(hint, 11f);
        card.add(caption);
        card.add(value);
        card.add(hint);
        return card;
    }

    // "Inventory status" box: how many of this host's active events are on sale and sold out.
    // .count() counts the events that pass the filter; (int) turns the result into an int.
    private JPanel buildStatusSummary() {
        JPanel panel = sectionPanel("Inventory status");
        int onSale = (int) eventManager.getEventsByHostId(hostId).stream()
                .filter(event -> !event.isArchived() && "ON_SALE".equals(event.getStatus())).count();
        int soldOut = (int) eventManager.getEventsByHostId(hostId).stream()
                .filter(event -> !event.isArchived() && "SOLD_OUT".equals(event.getStatus())).count();
        addSummaryLine(panel, "On sale", Integer.toString(onSale), new Color(0x00, 0xE0, 0xA0));
        addSummaryLine(panel, "Sold out", Integer.toString(soldOut), new Color(0xFF, 0x4D, 0x6A));
        return panel;
    }

    // One line in a summary box: label on the left, coloured number on the right.
    private void addSummaryLine(JPanel panel, String label, String value, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel name = new JLabel(label);
        ViewStyles.styleSecondaryText(name, 13f);
        JLabel count = new JLabel(value);
        count.setForeground(color);
        count.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 16f));
        row.add(name, BorderLayout.WEST);
        row.add(count, BorderLayout.EAST);
        panel.add(row);
    }

    // "Event workspace" box with a button that jumps to the Manage Events page.
    private JPanel buildOverviewAction() {
        JPanel panel = sectionPanel("Event workspace");
        JLabel message = new JLabel("Review your inventory, manage ticket tiers, and follow sales.");
        ViewStyles.styleSecondaryText(message, 13f);
        message.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(message);
        JButton open = new JButton("Open event inventory");
        ViewStyles.stylePrimaryButton(open);
        open.setAlignmentX(LEFT_ALIGNMENT);
        open.setMaximumSize(new Dimension(230, 42));
        open.addActionListener(action -> showPage("Manage Events"));
        panel.add(open);
        return panel;
    }

    // An empty card with a heading, used by the two Overview boxes above. Items added later stack under the heading.
    private JPanel sectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBackground(new Color(0x17, 0x1B, 0x24));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(16, 17, 16, 17)));
        JLabel heading = new JLabel(title);
        ViewStyles.styleHeading(heading, 15f);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(heading);
        return panel;
    }

    // Builds the Manage Events page: filters on top, then one of three "cards" underneath:
    //   "empty"       - the host has no events yet (with a create button)
    //   "filtered"    - no events match the filters
    //   "eventsTable" - a header row plus one row per event
    private JPanel buildManageEventsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);
        page.add(buildFilters(), BorderLayout.NORTH);  // Search box and filters across the top
        eventList.setLayout(new javax.swing.BoxLayout(eventList, javax.swing.BoxLayout.Y_AXIS));  // Rows stacked top to bottom
        eventList.setBackground(new Color(0x17, 0x1B, 0x24));
        eventList.setBorder(new EmptyBorder(2, 2, 2, 2));
        JScrollPane scroll = new JScrollPane(eventList);  // The rows scroll if there are many
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)));
        scroll.getViewport().setBackground(new Color(0x17, 0x1B, 0x24));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel empty = new JPanel(new java.awt.GridBagLayout());  // Card 1: no events yet (GridBagLayout centres the message)
        empty.setBackground(new Color(0x17, 0x1B, 0x24));
        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new javax.swing.BoxLayout(message, javax.swing.BoxLayout.Y_AXIS));
        JLabel emptyTitle = new JLabel("You haven't created any events yet.");
        ViewStyles.styleHeading(emptyTitle, 21f);
        emptyTitle.setAlignmentX(CENTER_ALIGNMENT);
        JLabel hint = new JLabel("Start with an event and build your ticket tiers.");
        ViewStyles.styleSecondaryText(hint, 14f);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        JButton create = new JButton("+ Create Your First Event");
        ViewStyles.stylePrimaryButton(create);
        create.setAlignmentX(CENTER_ALIGNMENT);
        create.addActionListener(action -> startNewEvent());  // Opens an empty editor
        message.add(emptyTitle);
        message.add(javax.swing.Box.createVerticalStrut(8));
        message.add(hint);
        message.add(javax.swing.Box.createVerticalStrut(20));
        message.add(create);
        empty.add(message);
        eventState.add(empty, "empty");  // Register card 1 under the name "empty"

        JPanel filtered = new JPanel(new GridBagLayout());  // Card 2: nothing matches the filters
        filtered.setBackground(new Color(0x17, 0x1B, 0x24));
        JLabel filteredMessage = new JLabel("No events match these filters.");
        ViewStyles.styleSecondaryText(filteredMessage, 14f);
        filtered.add(filteredMessage);
        eventState.add(filtered, "filtered");  // Register card 2

        JPanel table = new JPanel(new BorderLayout());  // Card 3: the real list
        table.setBackground(new Color(0x17, 0x1B, 0x24));
        table.add(buildInventoryHeader(), BorderLayout.NORTH);  // Column titles
        table.add(scroll, BorderLayout.CENTER);
        eventState.add(table, "eventsTable");  // Register card 3
        page.add(eventState, BorderLayout.CENTER);
        return page;
    }

    // Builds the filter bar. Pressing Enter in the search box, changing the status, ticking
    // "Show archived" or clicking "Apply filters" all call refreshEvents() to redo the list.
    private JPanel buildFilters() {
        ViewStyles.styleInput(searchField);
        ViewStyles.styleInput(dateFromFilter);
        ViewStyles.styleInput(dateToFilter);
        archivedToggle.setOpaque(false);
        archivedToggle.setForeground(new Color(0xF0, 0xF4, 0xFF));
        JButton applyFilters = smallButton("Apply filters");
        applyFilters.addActionListener(action -> refreshEvents());
        searchField.addActionListener(action -> refreshEvents());
        statusFilter.addActionListener(action -> refreshEvents());
        archivedToggle.addActionListener(action -> refreshEvents());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(statusFilter);
        filters.add(new JLabel("From"));
        filters.add(dateFromFilter);
        filters.add(new JLabel("To"));
        filters.add(dateToFilter);
        filters.add(archivedToggle);
        filters.add(applyFilters);
        return filters;
    }

    // Builds the row of column titles above the event list.
    private JPanel buildInventoryHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(new Color(0x17, 0x1B, 0x24));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A, 0x30, 0x44)));
        // Column titles and how much of the width each column gets (the weights add up to 1).
        // The same weights are used in createInventoryRow() so the rows line up with these titles.
        String[] labels = {"EVENT NAME", "DATE", "CAPACITY", "SOLD", "REMAINING", "STATUS", "ACTION"};
        double[] weights = {.27, .11, .09, .16, .09, .10, .18};
        for (int index = 0; index < labels.length; index++) {
            JLabel label = new JLabel(labels[index]);
            label.setForeground(new Color(0x88, 0x96, 0xB3));
            label.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
            GridBagConstraints constraints = columnConstraints(index, weights[index]);
            constraints.insets = new Insets(12, 10, 12, 6);
            header.add(label, constraints);
        }
        return header;
    }

    // Builds one row of the event list: name/category, date, capacity, sold (with a progress bar),
    // seats remaining, status and action buttons.
    private JPanel createInventoryRow(Event event) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0x17, 0x1B, 0x24));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E, 0x24, 0x33)));
        int capacity = event.getTicketTiers().stream().mapToInt(TicketTier::getInitialCapacity).sum();  // Total seats across all tiers
        int sold = orderManager.calculateTotalTicketsSold(event.getEventId());
        int remaining = Math.max(0, capacity - sold);  // Never below 0
        int percentage = capacity == 0 ? 0 : Math.min(100, (int) Math.round(sold * 100.0 / capacity));  // Percent sold, 0-100

        JPanel eventInfo = new JPanel(new GridLayout(2, 1, 0, 3));
        eventInfo.setOpaque(false);
        JLabel name = new JLabel(event.getTitle());
        ViewStyles.styleHeading(name, 12f);
        JLabel category = new JLabel(event.getCategory());
        ViewStyles.styleSecondaryText(category, 10f);
        eventInfo.add(name);
        eventInfo.add(category);
        row.add(eventInfo, columnConstraints(0, .27));

        JLabel date = monoLabel(event.getDate(), new Color(0x88, 0x96, 0xB3));
        row.add(date, columnConstraints(1, .11));
        row.add(monoLabel(Integer.toString(capacity), new Color(0xF0, 0xF4, 0xFF)), columnConstraints(2, .09));

        JPanel sales = new JPanel(new GridLayout(2, 1, 0, 3));
        sales.setOpaque(false);
        sales.add(monoLabel(Integer.toString(sold), new Color(0xF0, 0xF4, 0xFF)));
        JProgressBar progress = new JProgressBar(0, 100);  // A bar that goes from 0 to 100
        progress.setValue(percentage);
        progress.setStringPainted(false);
        progress.setPreferredSize(new Dimension(90, 4));
        progress.setBorderPainted(false);
        progress.setBackground(new Color(0x2A, 0x30, 0x44));
        progress.setForeground(percentage >= 100 ? new Color(0xFF, 0x4D, 0x6A) : new Color(0x00, 0xB4, 0xFF));  // Red when full, blue otherwise
        sales.add(progress);
        row.add(sales, columnConstraints(3, .16));
        row.add(monoLabel(Integer.toString(remaining), remaining == 0
                ? new Color(0xFF, 0x4D, 0x6A) : new Color(0x00, 0xE0, 0xA0)), columnConstraints(4, .09));

        JLabel status = new JLabel(statusText(event.getStatus()));
        status.setForeground(statusColor(event.getStatus()));
        status.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        row.add(status, columnConstraints(5, .10));
        row.add(buildRowActions(event), columnConstraints(6, .18));
        // Give every cell in the row the same padding. "instanceof ... swingComponent" checks the
        // type and gives a variable of that type in one step.
        for (java.awt.Component component : row.getComponents()) {
            if (component instanceof javax.swing.JComponent swingComponent) {
                swingComponent.setBorder(new EmptyBorder(10, 10, 10, 6));
            }
        }
        return row;
    }

    // Builds the action buttons at the end of a row: Insights, Edit, and a "···" button that
    // opens a pop-up menu with more options. Each one calls a method with this row's event.
    private JPanel buildRowActions(Event event) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        actions.setOpaque(false);
        JButton insights = smallButton("Insights");
        insights.addActionListener(action -> showInsights(event));
        JButton edit = smallButton("Edit");
        edit.addActionListener(action -> loadEvent(event));
        JButton more = smallButton("···");
        JPopupMenu menu = new JPopupMenu();
        JMenuItem attendeeItem = new JMenuItem("View attendees");
        attendeeItem.addActionListener(action -> showAttendees(event));
        JMenuItem exportItem = new JMenuItem("Export CSV");
        exportItem.addActionListener(action -> exportAttendees(event));
        JMenuItem duplicateItem = new JMenuItem("Duplicate event");
        duplicateItem.addActionListener(action -> duplicateEvent(event));
        JMenuItem archiveItem = new JMenuItem(event.isArchived() ? "Restore event" : "Archive event");
        archiveItem.addActionListener(action -> toggleArchive(event));
        JMenuItem insightsItem = new JMenuItem("View insights");
        insightsItem.addActionListener(action -> showInsights(event));
        menu.add(insightsItem);
        menu.add(attendeeItem);
        menu.add(exportItem);
        menu.add(duplicateItem);
        menu.add(archiveItem);
        more.addActionListener(action -> menu.show(more, 0, more.getHeight()));  // Show the menu just below the "···" button
        actions.add(insights);
        actions.add(edit);
        actions.add(more);
        return actions;
    }

    // Layout rules for one column of a row: which column, its share of the width, stretch sideways, stick left.
    private GridBagConstraints columnConstraints(int column, double weight) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.weightx = weight;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    // A small label in the monospace font (used for numbers and dates in the rows).
    private JLabel monoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(ViewStyles.DATA_FONT.deriveFont(Font.PLAIN, 11f));
        return label;
    }

    // Turns a status code into display text, e.g. "ON_SALE" -> "ON SALE".
    private String statusText(String status) {
        return switch (status) {
            case "ON_SALE" -> "ON SALE";
            case "SOLD_OUT" -> "SOLD OUT";
            case "CANCELLED" -> "CANCELLED";
            default -> "PAUSED";
        };
    }

    // Recalculates the three Overview stat cards from all the host's active (not archived) events.
    private void updateSummary(List<Event> events) {
        int capacity = events.stream().filter(event -> !event.isArchived())
                .flatMap(event -> event.getTicketTiers().stream())  // flatMap: turn each event into its tiers, all in one stream
                .mapToInt(TicketTier::getInitialCapacity).sum();
        int sold = events.stream().filter(event -> !event.isArchived())
                .mapToInt(event -> orderManager.calculateTotalTicketsSold(event.getEventId())).sum();
        double revenue = events.stream().filter(event -> !event.isArchived())
                .mapToDouble(event -> orderManager.calculateEventRevenue(event.getEventId())).sum();
        capacityValue.setText(String.format("%,d", capacity));
        soldValue.setText(String.format("%,d", sold));
        revenueValue.setText(String.format("KES %,.0f", revenue));
        soldHint.setText((capacity == 0 ? 0 : Math.round(sold * 100.0 / capacity)) + "% sell-through");  // e.g. "56% sell-through"
        capacityHint.setText(events.stream().filter(event -> !event.isArchived()).count() + " active events");
    }

    // Builds the Settings page: who is signed in, their account ID and the theme.
    private JPanel buildSettingsPage(String name) {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        JPanel settings = sectionPanel("Host account");
        JLabel nameLabel = new JLabel("Signed in as " + name);
        ViewStyles.styleSecondaryText(nameLabel, 13f);
        settings.add(nameLabel);
        JLabel idLabel = new JLabel("Account ID: " + hostId);
        ViewStyles.styleSecondaryText(idLabel, 12f);
        settings.add(idLabel);
        JLabel themeLabel = new JLabel("Appearance: Dark");
        ViewStyles.styleSecondaryText(themeLabel, 12f);
        settings.add(themeLabel);
        page.add(settings, BorderLayout.NORTH);
        return page;
    }

    // Switches to a page by name, and updates the top bar and the sidebar highlight to match.
    private void showPage(String page) {
        activePage = page;  // Remember which page is showing
        // Pick the title for this page. A "switch expression" returns a value for each case.
        String title = switch (page) {
            case "Overview" -> "Overview";
            case "Settings" -> "Settings";
            case "Editor" -> editingEvent == null ? "Create New Event" : "Edit Event";
            case "Insights" -> "Event Insights";
            default -> "Active Events Inventory";
        };
        pageTitle.setText(title);
        pageDescription.setText(switch (page) {
            case "Overview" -> "Your ticket sales at a glance";
            case "Settings" -> "Account and appearance";
            case "Editor" -> "Configure event details and ticket tiers";
            case "Insights" -> "Sales, tiers and bookings for one event";
            default -> "Manage and monitor all active events";
        });
        createEventButton.setText("Editor".equals(page) ? "Back to events" : "+  Create New Event");  // Same button, different job in the editor
        // Highlight the sidebar button for this page. The Editor and Insights pages belong under
        // "Manage Events", so that button stays highlighted for them too.
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(page)
                    || ("Editor".equals(page) || "Insights".equals(page)) && "Manage Events".equals(entry.getKey());
            JButton button = entry.getValue();
            button.setBackground(selected ? new Color(0x00, 0xB4, 0xFF, 28) : new Color(0x17, 0x1B, 0x24));
            button.setForeground(selected ? new Color(0x00, 0xB4, 0xFF) : new Color(0x88, 0x96, 0xB3));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 2, 0, 0,
                            selected ? new Color(0x00, 0xB4, 0xFF) : new Color(0x17, 0x1B, 0x24)),
                    new EmptyBorder(11, 11, 11, 8)));
        }
        if (pages.getComponentCount() > 0) {  // Only once the pages exist
            pageLayout.show(pages, page);
        }
        revalidate();
        repaint();
    }

    // Opens the insights page for one event.
    private void showInsights(Event event) {
        insightsPanel.showEvent(event);
        showPage("Insights");
    }

    // Overview charts across all of this host's active events. Rebuilt on every refresh so they
    // always match the latest sales.
    private void rebuildOverviewCharts(List<Event> allEvents) {
        overviewCharts.removeAll();  // Clear the old charts
        List<Event> active = allEvents.stream().filter(event -> !event.isArchived()).toList();  // Skip archived events
        if (active.isEmpty()) {
            overviewCharts.revalidate();
            return;
        }
        // Chart 1: revenue per event, as one-colour bars. Two matching arrays: names[i] and revenue[i].
        String[] names = active.stream().map(Event::getTitle).toArray(String[]::new);
        double[] revenue = active.stream()
                .mapToDouble(event -> orderManager.calculateEventRevenue(event.getEventId())).toArray();
        HorizontalBarChart revenueChart = new HorizontalBarChart(names, revenue, null,
                value -> "KES " + ChartPanel.compact(value),
                i -> String.format("%s: KES %,.0f from %,d tickets. Click for insights.", names[i], revenue[i],
                        orderManager.calculateTotalTicketsSold(active.get(i).getEventId())),
                ChartPanel.SERIES[0]);
        revenueChart.setClickAction(i -> showInsights(active.get(i)));  // Clicking bar i opens that event's insights

        // Chart 2: tickets sold per day across ALL active events, added together day by day.
        int days = 30;
        double[] daily = new double[days];
        for (Event event : active) {
            int[] eventDaily = orderManager.getDailyTicketSales(event.getEventId(), days);
            for (int i = 0; i < days; i++) daily[i] += eventDaily[i];  // Add this event's day i to the total for day i
        }
        String[] labels = new String[days];
        // Date labels like "3 Oct" for each of the 30 days, oldest first.
        java.time.format.DateTimeFormatter format =
                java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH);
        for (int i = 0; i < days; i++) labels[i] = LocalDate.now().minusDays(days - 1L - i).format(format);
        ColumnChart dailyChart = new ColumnChart(daily, labels,
                i -> String.format("%s: %,.0f tickets across all events", labels[i], daily[i]),
                ChartPanel.SERIES[0], 5, false, revenueChart.getPreferredSize().height);

        JPanel revenueCard = EventAnalyticsPanel.card("Revenue by event",
                "Click an event to open its insights", revenueChart);
        JPanel dailyCard = EventAnalyticsPanel.card("Daily ticket sales",
                "All active events, last 30 days", dailyChart);
        // Place the two cards side by side: the revenue card gets 1 share of the width, the daily chart 1.2.
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 7);
        GridBagConstraints right = (GridBagConstraints) left.clone();
        right.gridx = 1;
        right.weightx = 1.2;
        right.insets = new Insets(0, 7, 0, 0);
        revenueCard.setPreferredSize(new Dimension(1, revenueCard.getPreferredSize().height));
        dailyCard.setPreferredSize(new Dimension(1, revenueCard.getPreferredSize().height));
        overviewCharts.add(revenueCard, left);
        overviewCharts.add(dailyCard, right);
        overviewCharts.setMaximumSize(new Dimension(Integer.MAX_VALUE, overviewCharts.getPreferredSize().height));
        overviewCharts.revalidate();
        overviewCharts.repaint();
    }

    // True if the event's date is inside the From/To filter boxes (empty boxes mean no limit).
    // If any date can't be read, the event is hidden.
    private boolean matchesDateRange(Event event) {
        try {
            LocalDate eventDate = LocalDate.parse(event.getDate());
            String from = dateFromFilter.getText().trim();
            String to = dateToFilter.getText().trim();
            return (from.isEmpty() || !eventDate.isBefore(LocalDate.parse(from)))
                    && (to.isEmpty() || !eventDate.isAfter(LocalDate.parse(to)));
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    // A compact secondary button, used in the rows and sidebar.
    private JButton smallButton(String text) {
        JButton button = new JButton(text);
        ViewStyles.styleSecondaryButton(button);
        button.setFont(button.getFont().deriveFont(12f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE), new EmptyBorder(5, 8, 5, 8)));
        return button;
    }

    // Pops up a window with a table of everyone who booked this event.
    private void showAttendees(Event event) {
        // Anonymous subclass: overrides isCellEditable() so the attendee table is read-only.
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Receipt", "Guest", "Email", "Tier", "Qty", "Total (KES)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        // One table row per booking.
        orderManager.getBookingsByEvent(event.getEventId()).forEach(booking -> model.addRow(new Object[] {
            booking.getBookingId(), booking.getCustomerName(), booking.getCustomerEmail(),
            booking.getTierName(), booking.getQuantity(), booking.getTotalPrice()
        }));
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);  // Table background fills the whole scroll area
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(780, 330));
        // A message box can show any component - here, the scrolling table.
        JOptionPane.showMessageDialog(this, scroll, event.getTitle() + " attendees",
                JOptionPane.PLAIN_MESSAGE);
    }

    // Saves this event's bookings to a CSV file (opens in Excel or Google Sheets).
    private void exportAttendees(Event event) {
        JFileChooser chooser = new JFileChooser();
        // Suggest a file name from the title, swapping anything that isn't a letter, digit, _ or - for "_".
        chooser.setSelectedFile(new java.io.File(event.getTitle().replaceAll("[^A-Za-z0-9_-]", "_") + "-attendees.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {  // The host clicked Cancel
            return;
        }
        // try-with-resources: the file is closed automatically, even if an error happens.
        // "var" lets Java work out the variable's type itself.
        try (var writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            writer.write("Receipt,Guest,Email,Tier,Quantity,Total KES");  // Header line
            writer.newLine();
            for (var booking : orderManager.getBookingsByEvent(event.getEventId())) {
                // One line per booking, values separated by commas.
                writer.write(String.join(",", csv(booking.getBookingId()), csv(booking.getCustomerName()),
                        csv(booking.getCustomerEmail()), csv(booking.getTierName()),
                        csv(Integer.toString(booking.getQuantity())), csv(Double.toString(booking.getTotalPrice()))));
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Attendee CSV exported.", "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            showError("Could not export the CSV: " + exception.getMessage());
        }
    }

    // Makes a value safe for CSV: wraps it in quotes and doubles any quotes inside it
    // (so a name like  Jo "DJ" Kim  doesn't break the columns).
    private String csv(String value) {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    // Copies an event (the controller makes the copy), then opens the copy in the editor.
    private void duplicateEvent(Event event) {
        Event copy = eventManager.duplicateEvent(event.getEventId(), hostId);
        refreshEvents();
        loadEvent(copy);
    }

    // Archives (hides) or restores an event, after asking the host to confirm.
    private void toggleArchive(Event event) {
        boolean archive = !event.isArchived();  // The opposite of its current state
        int answer = JOptionPane.showConfirmDialog(this,
                archive ? "Archive this event? It will no longer appear in the public catalog."
                        : "Restore this event to your dashboard and the public catalog?",
                "Confirm event change", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {  // Only if the host clicked Yes
            eventManager.setArchived(event.getEventId(), archive);
            refreshEvents();
        }
    }

    // Text colour for each status in the event rows.
    private Color statusColor(String status) {
        return switch (status) {
            case "ON_SALE" -> new Color(0x1F, 0x8A, 0x55);
            case "SOLD_OUT" -> new Color(0xB8, 0x3A, 0x3A);
            case "CANCELLED" -> new Color(0x73, 0x36, 0x36);
            default -> new Color(0x8A, 0x67, 0x24);
        };
    }

    // Builds the event editor page: a red error line, nine labelled fields, the ticket tier
    // manager, the custom details section and the Save / New buttons. The whole form scrolls.
    private JPanel buildManagementForm() {
        styleInputs();
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.BACKGROUND);
        form.setBorder(new EmptyBorder(14, 8, 18, 8));
        validationLabel.setForeground(new Color(0xFF, 0x4D, 0x6A));
        validationLabel.setFont(ViewStyles.BODY_FONT.deriveFont(java.awt.Font.BOLD, 12f));
        validationLabel.setVisible(false);
        GridBagConstraints validationConstraints = new GridBagConstraints();
        validationConstraints.gridx = 0;
        validationConstraints.gridy = 0;
        validationConstraints.gridwidth = 2;
        validationConstraints.anchor = GridBagConstraints.WEST;
        validationConstraints.fill = GridBagConstraints.HORIZONTAL;
        validationConstraints.insets = new Insets(0, 0, 14, 0);
        form.add(validationLabel, validationConstraints);
        // Rows 1-9: one labelled field each. "*" marks required fields.
        addFormField(form, "Event title *", titleField, 1);
        addFormField(form, "Event date *", dateChooser, 2);
        addFormField(form, "Venue *", venueField, 3);
        addFormField(form, "Sales start (optional)", salesStartChooser, 4);
        addFormField(form, "Sales end (optional)", salesEndChooser, 5);
        addFormField(form, "Sales status", statusBox, 6);
        addFormField(form, "Category", categoryField, 7);
        addFormField(form, "Banner image", buildBannerField(), 8);
        addFormField(form, "Card accent", accentColorButton, 9);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 0, 5, 0);
        form.add(buildTierManager(), constraints);  // Row 10: ticket tiers

        constraints.gridy = 11;
        form.add(buildCustomFields(), constraints);  // Row 11: custom details

        JButton save = new JButton("Save Event");
        ViewStyles.stylePrimaryButton(save);
        save.addActionListener(action -> saveEvent());  // Validate and save
        JButton fresh = new JButton("New Event");
        ViewStyles.styleSecondaryButton(fresh);
        fresh.addActionListener(action -> startNewEvent());  // Clear the form
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(fresh);
        actions.add(save);
        constraints.gridy = 12;
        constraints.insets = new Insets(16, 0, 0, 0);
        form.add(actions, constraints);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ViewStyles.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ViewStyles.BACKGROUND);
        wrapper.add(scroll);
        return wrapper;
    }

    // The banner row: a text box for the picture's path plus a "Browse..." button.
    private JPanel buildBannerField() {
        JPanel field = new JPanel(new BorderLayout(8, 0));
        field.setOpaque(false);
        JButton browse = new JButton("Browse...");
        ViewStyles.styleSecondaryButton(browse);
        browse.addActionListener(action -> chooseBanner());
        field.add(bannerPathField, BorderLayout.CENTER);
        field.add(browse, BorderLayout.EAST);
        return field;
    }

    // The custom details section: name + value boxes with an "Add detail" button,
    // the list of details added so far, and a "Remove selected detail" button.
    private JPanel buildCustomFields() {
        JPanel section = new JPanel(new BorderLayout(8, 8));
        section.setOpaque(false);
        JLabel heading = new JLabel("Custom event details");
        ViewStyles.styleHeading(heading, 16f);
        section.add(heading, BorderLayout.NORTH);

        JPanel editor = new JPanel(new BorderLayout(8, 0));
        editor.setOpaque(false);
        JPanel inputs = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        inputs.setOpaque(false);
        ViewStyles.styleInput(customNameField);
        ViewStyles.styleInput(customValueField);
        inputs.add(customNameField);
        inputs.add(customValueField);
        JButton add = new JButton("Add detail");
        ViewStyles.styleSecondaryButton(add);
        add.addActionListener(action -> addCustomField());
        editor.add(inputs, BorderLayout.CENTER);
        editor.add(add, BorderLayout.EAST);

        customFieldList.setBackground(ViewStyles.SURFACE);
        customFieldList.setForeground(ViewStyles.FOREGROUND);
        JScrollPane detailsScroll = new JScrollPane(customFieldList);
        detailsScroll.setPreferredSize(new Dimension(600, 78));
        JButton remove = new JButton("Remove selected detail");
        ViewStyles.styleSecondaryButton(remove);
        remove.addActionListener(action -> {
            // Remove whichever detail is selected in the list (-1 means nothing is selected).
            int selected = customFieldList.getSelectedIndex();
            if (selected >= 0) customFieldModel.remove(selected);
        });
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(editor, BorderLayout.NORTH);
        body.add(detailsScroll, BorderLayout.CENTER);
        body.add(remove, BorderLayout.SOUTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    // Adds the typed name/value to the details list. If a detail with that name already exists
    // (ignoring capitals), it's replaced instead of added twice.
    private void addCustomField() {
        String name = customNameField.getText().trim();
        String value = customValueField.getText().trim();
        if (name.isEmpty() || value.isEmpty()) {
            showError("Enter both a custom detail name and value.");
            return;
        }
        for (int index = 0; index < customFieldModel.size(); index++) {
            if (customFieldModel.get(index).name().equalsIgnoreCase(name)) {
                customFieldModel.set(index, new CustomField(name, value));
                customNameField.setText("");
                customValueField.setText("");
                return;
            }
        }
        customFieldModel.addElement(new CustomField(name, value));  // Not found: add it as a new detail
        customNameField.setText("");
        customValueField.setText("");
    }

    // Opens a file picker (images only) and puts the chosen file's path in the banner box.
    private void chooseBanner() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            bannerPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Opens a colour picker. If the host picks a colour, remember it and show it on the button.
    private void chooseAccentColor() {
        Color chosen = JColorChooser.showDialog(this, "Choose event accent", selectedAccent);
        if (chosen != null) {  // null means the host cancelled
            selectedAccent = chosen;
            accentColorButton.setBackground(chosen);
        }
    }

    // The ticket tier section: name/price/capacity inputs with an Add/Update button,
    // the list of tiers, and a "Remove Selected Tier" button.
    private JPanel buildTierManager() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        JLabel heading = new JLabel("Ticket tiers");
        ViewStyles.styleHeading(heading, 17f);
        panel.add(heading, BorderLayout.NORTH);

        JPanel editor = new JPanel(new GridBagLayout());
        editor.setBackground(ViewStyles.SURFACE);
        editor.setBorder(new EmptyBorder(12, 12, 12, 12));
        addTierField(editor, "Tier name", tierNameField, 0, 0);
        addTierField(editor, "Price (KES)", priceSpinner, 1, 0);
        addTierField(editor, "Capacity", capacitySpinner, 2, 0);
        GridBagConstraints button = new GridBagConstraints();
        button.gridx = 2;
        button.gridy = 0;
        button.gridheight = 3;  // The button spans 3 rows, beside the inputs
        button.insets = new Insets(0, 8, 0, 0);
        editor.add(tierActionButton, button);
        ViewStyles.stylePrimaryButton(tierActionButton);
        tierActionButton.addActionListener(action -> addOrUpdateTier());

        tierList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // Only one tier can be selected at a time
        tierList.setBackground(ViewStyles.SURFACE);
        tierList.setForeground(ViewStyles.FOREGROUND);
        tierList.setFixedCellHeight(34);
        // A "renderer" decides how each list item looks, e.g. "VIP  |  KES 3,750.00  |  12 / 18 available".
        tierList.setCellRenderer((list, tier, index, selected, focus) -> {
            JLabel row = new JLabel(String.format("%s  |  KES %,.2f  |  %d / %d available",
                    tier.getTierName(), tier.getPrice(), tier.getAvailableSeats(), tier.getInitialCapacity()));
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(4, 8, 4, 8));
            row.setBackground(selected ? ViewStyles.ACCENT : ViewStyles.SURFACE);
            row.setForeground(ViewStyles.FOREGROUND);
            return row;
        });
        // Clicking a tier copies it into the inputs so it can be edited.
        // getValueIsAdjusting() is true while the mouse is still being dragged, so we wait until it's false.
        tierList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedTier();
            }
        });
        JScrollPane listScroll = new JScrollPane(tierList);
        listScroll.setPreferredSize(new Dimension(600, 110));
        listScroll.setBorder(BorderFactory.createLineBorder(ViewStyles.OUTLINE));
        JButton remove = new JButton("Remove Selected Tier");
        ViewStyles.styleSecondaryButton(remove);
        remove.addActionListener(action -> removeSelectedTier());
        JPanel listPanel = new JPanel(new BorderLayout(0, 8));
        listPanel.setOpaque(false);
        listPanel.add(listScroll, BorderLayout.CENTER);
        listPanel.add(remove, BorderLayout.SOUTH);
        JPanel contents = new JPanel(new BorderLayout(0, 10));
        contents.setOpaque(false);
        contents.add(editor, BorderLayout.NORTH);
        contents.add(listPanel, BorderLayout.CENTER);
        panel.add(contents, BorderLayout.CENTER);
        return panel;
    }

    // Applies the dark input style to every box in the editor.
    private void styleInputs() {
        ViewStyles.styleInput(titleField);
        styleDateChooser(dateChooser);
        ViewStyles.styleInput(venueField);
        styleDateChooser(salesStartChooser);
        styleDateChooser(salesEndChooser);
        ViewStyles.styleInput(categoryField);
        ViewStyles.styleInput(bannerPathField);
        ViewStyles.styleInput(tierNameField);
        styleSpinner(priceSpinner);
        styleSpinner(capacitySpinner);
        accentColorButton.setOpaque(true);
        accentColorButton.setForeground(Color.WHITE);
        accentColorButton.setBackground(selectedAccent);
        accentColorButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        accentColorButton.addActionListener(action -> chooseAccentColor());
    }

    // Styles a date picker: its text box gets the normal input style.
    private void styleDateChooser(JDateChooser chooser) {
        if (chooser.getDateEditor().getUiComponent() instanceof JTextField editor) {
            ViewStyles.styleInput(editor);
        }
        chooser.setBorder(BorderFactory.createLineBorder(ViewStyles.OUTLINE));
        chooser.setPreferredSize(new Dimension(260, 36));
    }

    // Adds one editor row: label in column 0, input stretching across column 1.
    private void addFormField(JPanel form, String label, java.awt.Component input, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(0, 0, 12, 14);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 14f);
        form.add(fieldLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        form.add(input, constraints);
    }

    // Adds one tier input: label at (column, row), input just to its right.
    private void addTierField(JPanel panel, String label, java.awt.Component input, int row, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 4, 3, 8);
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 12f);
        panel.add(fieldLabel, constraints);
        constraints.gridx = column + 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(3, 0, 3, 12);
        panel.add(input, constraints);
    }

    // Styles the text box inside a number spinner.
    private void styleSpinner(JSpinner spinner) {
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            ViewStyles.styleInput(editor.getTextField());
        }
    }

    private void addOrUpdateTier() {
        String name = tierNameField.getText().trim();
        double price = ((Number) priceSpinner.getValue()).doubleValue();
        int capacity = ((Number) capacitySpinner.getValue()).intValue();
        if (name.isEmpty() || price <= 0 || capacity <= 0) {
            showError("Enter a tier name, positive price, and positive capacity.");
            return;
        }
        int selected = tierList.getSelectedIndex();
        for (int index = 0; index < tierModel.size(); index++) {
            if (index != selected && tierModel.get(index).getTierName().equalsIgnoreCase(name)) {
                showError("Each ticket tier must have a unique name.");
                return;
            }
        }
        int sold = 0;
        if (selected >= 0) {
            TicketTier current = tierModel.get(selected);
            sold = current.getInitialCapacity() - current.getAvailableSeats();
            if (sold > 0 && !current.getTierName().equalsIgnoreCase(name)) {
                showError("A ticket tier with sales cannot be renamed.");
                return;
            }
            if (capacity < sold) {
                showError("Capacity cannot be lower than the number of tickets already sold.");
                return;
            }
        }
        TicketTier updated = new TicketTier(name, price, capacity, capacity - sold);
        if (selected >= 0) {
            tierModel.set(selected, updated);
        } else {
            tierModel.addElement(updated);
        }
        tierList.clearSelection();
        clearTierEditor();
    }

    private void loadSelectedTier() {
        int selected = tierList.getSelectedIndex();
        if (selected < 0) {
            return;
        }
        TicketTier tier = tierModel.get(selected);
        tierNameField.setText(tier.getTierName());
        priceSpinner.setValue(tier.getPrice());
        capacitySpinner.setValue(tier.getInitialCapacity());
        tierActionButton.setText("Update Tier");
    }

    private void removeSelectedTier() {
        int selected = tierList.getSelectedIndex();
        if (selected >= 0) {
            TicketTier tier = tierModel.get(selected);
            int ticketsSold = editingEvent == null ? 0 : orderManager.getBookingsByEvent(editingEvent.getEventId())
                    .stream()
                    .filter(booking -> tier.getTierName().equals(booking.getTierName()))
                    .mapToInt(models.Booking::getQuantity)
                    .sum();
            if (ticketsSold > 0) {
                showError("This tier has bookings and cannot be removed.");
                return;
            }
            tierModel.remove(selected);
            tierList.clearSelection();
            clearTierEditor();
        }
    }

    private void clearTierEditor() {
        tierNameField.setText("");
        priceSpinner.setValue(1000.0);
        capacitySpinner.setValue(50);
        tierActionButton.setText("Add Tier");
    }

    // Checks the form, then either updates the Event being edited or creates a new one,
    // and hands it to the EventManager.
    private void saveEvent() {
        clearValidation();
        String title = titleField.getText().trim();
        String venue = venueField.getText().trim();
        LocalDate eventDate = toLocalDate(dateChooser.getDate());
        if (title.isEmpty() || eventDate == null || venue.isEmpty()) {
            List<java.awt.Component> missingFields = new ArrayList<>();
            if (title.isEmpty()) missingFields.add(titleField);
            if (eventDate == null) missingFields.add(dateChooser);
            if (venue.isEmpty()) missingFields.add(venueField);
            showValidationError("Required: add an event title, event date, and venue.",
                    missingFields.toArray(java.awt.Component[]::new));
            return;
        }
        LocalDate salesStart = toLocalDate(salesStartChooser.getDate());
        LocalDate salesEnd = toLocalDate(salesEndChooser.getDate());
        if (salesStart != null && salesEnd != null && salesEnd.isBefore(salesStart)) {
            showValidationError("Sales end cannot be before sales start.", salesStartChooser, salesEndChooser);
            return;
        }
        if (tierModel.isEmpty()) {
            showValidationError("Add at least one ticket tier before saving.", tierList);
            return;
        }

        List<TicketTier> tiers = new ArrayList<>();
        for (int index = 0; index < tierModel.size(); index++) {
            TicketTier tier = tierModel.get(index);
            tiers.add(new TicketTier(tier.getTierName(), tier.getPrice(), tier.getInitialCapacity(),
                    tier.getAvailableSeats()));
        }
        String status = (String) statusBox.getSelectedItem();
        Event savedEvent;
        if (editingEvent == null) {
            savedEvent = new Event("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    title, eventDate.toString(), venue, tiers, status, hostId);
        } else {
            savedEvent = editingEvent;
        }
        savedEvent.setTitle(title);
        savedEvent.setDate(eventDate.toString());
        savedEvent.setVenue(venue);
        savedEvent.setTicketTiers(tiers);
        savedEvent.setStatus(status);
        savedEvent.setSalesStartDate(salesStart);
        savedEvent.setSalesEndDate(salesEnd);
        savedEvent.setCategory(categoryField.getText().trim());
        savedEvent.setBannerImagePath(bannerPathField.getText().trim());
        savedEvent.setAccentColorHex(String.format("#%06X", selectedAccent.getRGB() & 0xFFFFFF));
        Map<String, String> customDetails = new LinkedHashMap<>();
        for (int index = 0; index < customFieldModel.size(); index++) {
            CustomField customField = customFieldModel.get(index);
            customDetails.put(customField.name(), customField.value());
        }
        savedEvent.setCustomDetails(customDetails);
        if (editingEvent == null) {
            eventManager.addEvent(savedEvent);
        } else {
            eventManager.updateEvent(savedEvent);
        }
        refreshEvents();
        clearValidation();
        startNewEvent();
        showPage("Manage Events");
        JOptionPane.showMessageDialog(this, "Event saved successfully.", "Event Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // The reverse of saveEvent(): copies an Event object's data into the form fields using its getters.
    private void loadEvent(Event event) {
        editingEvent = event;
        titleField.setText(event.getTitle());
        dateChooser.setDate(toDate(LocalDate.parse(event.getDate())));
        venueField.setText(event.getVenue());
        categoryField.setText(event.getCategory());
        bannerPathField.setText(event.getBannerImagePath());
        try {
            selectedAccent = Color.decode(event.getAccentColorHex());
        } catch (RuntimeException exception) {
            selectedAccent = ViewStyles.ACCENT;
        }
        accentColorButton.setBackground(selectedAccent);
        customFieldModel.clear();
        event.getCustomDetails().forEach((name, value) -> customFieldModel.addElement(new CustomField(name, value)));
        salesStartChooser.setDate(toDate(event.getSalesStartDate()));
        salesEndChooser.setDate(toDate(event.getSalesEndDate()));
        statusBox.setSelectedItem(event.getStatus());
        tierModel.clear();
        for (TicketTier tier : event.getTicketTiers()) {
            tierModel.addElement(new TicketTier(tier.getTierName(), tier.getPrice(),
                    tier.getInitialCapacity(), tier.getAvailableSeats()));
        }
        clearTierEditor();
        showPage("Editor");
    }

    private void startNewEvent() {
        editingEvent = null;
        titleField.setText("");
        dateChooser.setDate(null);
        venueField.setText("");
        categoryField.setText("General");
        bannerPathField.setText("");
        selectedAccent = ViewStyles.ACCENT;
        accentColorButton.setBackground(selectedAccent);
        customFieldModel.clear();
        salesStartChooser.setDate(null);
        salesEndChooser.setDate(null);
        statusBox.setSelectedItem("ON_SALE");
        tierModel.clear();
        tierList.clearSelection();
        clearTierEditor();
        clearValidation();
        showPage("Editor");
    }

    private void showError(String message) {
        showValidationError(message);
    }

    private void showValidationError(String message, java.awt.Component... invalidFields) {
        validationLabel.setText(message);
        validationLabel.setVisible(true);
        for (java.awt.Component field : invalidFields) {
            if (field instanceof javax.swing.JComponent component) {
                component.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xFF, 0x4D, 0x6A), 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            }
        }
        validationLabel.revalidate();
        validationLabel.repaint();
    }

    private void clearValidation() {
        validationLabel.setText(" ");
        validationLabel.setVisible(false);
        ViewStyles.styleInput(titleField);
        ViewStyles.styleInput(venueField);
        ViewStyles.styleInput(tierNameField);
        styleDateChooser(dateChooser);
        styleDateChooser(salesStartChooser);
        styleDateChooser(salesEndChooser);
        tierList.setBorder(null);
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // A record is a short way to write a small, unchangeable data class. Java writes the
    // constructor and getters for us. toString() is overridden so the list shows "name: value".
    private record CustomField(String name, String value) {
        @Override
        public String toString() {
            return name + ": " + value;
        }
    }
}