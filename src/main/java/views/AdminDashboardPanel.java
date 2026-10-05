package views;

import controllers.EventManager;
import controllers.OrderManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JDateChooser;
import models.Event;
import models.TicketTier;

public class AdminDashboardPanel extends JPanel {
    private static final String[] EVENT_STATUSES = {"ON_SALE", "PAUSED", "CANCELLED", "SOLD_OUT"};

    private final EventManager eventManager;
    private final OrderManager orderManager;
    private final Runnable logoutAction;
    private final String hostId;
    private final JPanel eventList = new JPanel();
    private final JPanel eventState = new JPanel(new java.awt.CardLayout());
    private final java.awt.CardLayout pageLayout = new java.awt.CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel pageTitle = new JLabel("Active Events Inventory");
    private final JLabel pageDescription = new JLabel("Manage and monitor your events");
    private final JLabel capacityValue = new JLabel("0");
    private final JLabel soldValue = new JLabel("0");
    private final JLabel revenueValue = new JLabel("KES 0");
    private final JLabel capacityHint = new JLabel("across active events");
    private final JLabel soldHint = new JLabel("0% sell-through");
    private final JLabel revenueHint = new JLabel("before platform fees");
    private final JButton createEventButton = new JButton("+  Create New Event");
    private String activePage = "Manage Events";
    private final JTextField titleField = new JTextField(28);
    private final JDateChooser dateChooser = createDateChooser();
    private final JTextField venueField = new JTextField(28);
    private final JDateChooser salesStartChooser = createDateChooser();
    private final JDateChooser salesEndChooser = createDateChooser();
    private final JLabel validationLabel = new JLabel(" ");
    private final JTextField categoryField = new JTextField(28);
    private final JTextField bannerPathField = new JTextField(22);
    private final JTextField customNameField = new JTextField(12);
    private final JTextField customValueField = new JTextField(18);
    private final JTextField tierNameField = new JTextField(12);
    private final JSpinner priceSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 1.0, 100000000.0, 100.0));
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
    private final JComboBox<String> statusBox = new JComboBox<>(EVENT_STATUSES);
    private final DefaultListModel<TicketTier> tierModel = new DefaultListModel<>();
    private final JList<TicketTier> tierList = new JList<>(tierModel);
        private final DefaultListModel<CustomField> customFieldModel = new DefaultListModel<>();
        private final JList<CustomField> customFieldList = new JList<>(customFieldModel);
    private final JButton tierActionButton = new JButton("Add Tier");
        private final JButton accentColorButton = new JButton("Choose accent color");
        private final JTextField searchField = new JTextField(18);
        private final JTextField dateFromFilter = new JTextField(10);
        private final JTextField dateToFilter = new JTextField(10);
        private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {"All statuses", "ON_SALE", "PAUSED", "SOLD_OUT", "CANCELLED"});
        private final JCheckBox archivedToggle = new JCheckBox("Show archived");
        private Color selectedAccent = ViewStyles.ACCENT;
    private Event editingEvent;

    private static JDateChooser createDateChooser() {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("yyyy-MM-dd");
        chooser.setPreferredSize(new Dimension(260, 36));
        chooser.setDate(null);
        return chooser;
    }

    public AdminDashboardPanel(EventManager eventManager, OrderManager orderManager, Runnable logoutAction,
            String hostId, String hostName) {
        this.eventManager = eventManager;
        this.orderManager = orderManager;
        this.logoutAction = logoutAction;
        this.hostId = hostId;
        buildPanel(hostName);
        refreshEvents();
    }

    public void refreshEvents() {
        eventList.removeAll();
        List<Event> allEvents = eventManager.getEventsByHostId(hostId);
        String query = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        String selectedStatus = (String) statusFilter.getSelectedItem();
        List<Event> events = allEvents.stream()
            .filter(event -> archivedToggle.isSelected() || !event.isArchived())
            .filter(event -> "All statuses".equals(selectedStatus) || event.getStatus().equals(selectedStatus))
            .filter(event -> query.isEmpty() || (event.getTitle() + " " + event.getVenue() + " "
                + event.getCategory() + " " + event.getDate()).toLowerCase(java.util.Locale.ROOT).contains(query))
            .filter(this::matchesDateRange)
            .toList();
        for (Event event : events) eventList.add(createInventoryRow(event));
        updateSummary(allEvents);
        java.awt.CardLayout layout = (java.awt.CardLayout) eventState.getLayout();
        layout.show(eventState, allEvents.isEmpty() ? "empty" : events.isEmpty() ? "filtered" : "eventsTable");
        eventList.revalidate();
        eventList.repaint();
    }

    private void buildPanel(String hostName) {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0F, 0x11, 0x17));
        add(buildSidebar(hostName), BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout(0, 18));
        workspace.setBackground(new Color(0x0F, 0x11, 0x17));
        workspace.setBorder(new EmptyBorder(24, 28, 24, 28));
        workspace.add(buildTopBar(), BorderLayout.NORTH);

        pages.setBackground(new Color(0x0F, 0x11, 0x17));
        pages.add(buildOverviewPage(hostName), "Overview");
        pages.add(buildManageEventsPage(), "Manage Events");
        pages.add(buildSettingsPage(hostName), "Settings");
        pages.add(buildManagementForm(), "Editor");
        workspace.add(pages, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        showPage("Overview");
    }

    private JPanel buildSidebar(String name) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(224, 0));
        sidebar.setBackground(new Color(0x17, 0x1B, 0x24));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x2A, 0x30, 0x44)));

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

        JPanel navigation = new JPanel();
        navigation.setLayout(new javax.swing.BoxLayout(navigation, javax.swing.BoxLayout.Y_AXIS));
        navigation.setOpaque(false);
        navigation.setBorder(new EmptyBorder(0, 8, 10, 8));
        for (String item : List.of("Overview", "Manage Events", "Settings")) {
            JButton button = new JButton(item);
            button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(11, 13, 11, 8));
            button.setOpaque(true);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setPreferredSize(new Dimension(196, 44));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.addActionListener(action -> showPage(item));
            navButtons.put(item, button);
            navigation.add(button);
            navigation.add(javax.swing.Box.createVerticalStrut(5));
        }
        sidebar.add(navigation, BorderLayout.CENTER);

        JPanel account = new JPanel(new BorderLayout(9, 0));
        account.setOpaque(false);
        account.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2A, 0x30, 0x44)),
                new EmptyBorder(15, 15, 18, 12)));
        JLabel avatar = new JLabel(name.isBlank() ? "A" : name.substring(0, 1).toUpperCase());
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
        logout.addActionListener(action -> logoutAction.run());
        account.add(logout, BorderLayout.SOUTH);
        sidebar.add(account, BorderLayout.SOUTH);
        return sidebar;
    }

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

    private JPanel buildOverviewPage(String name) {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        JPanel overview = new JPanel(new BorderLayout(0, 6));
        overview.setOpaque(false);
        JLabel welcome = new JLabel("Welcome back, " + name + ".");
        ViewStyles.styleHeading(welcome, 18f);
        JLabel note = new JLabel("Your events, ticket sales and host tools are ready.");
        ViewStyles.styleSecondaryText(note, 12f);
        overview.add(welcome, BorderLayout.NORTH);
        overview.add(note, BorderLayout.SOUTH);
        overview.setAlignmentX(LEFT_ALIGNMENT);
        overview.setMaximumSize(new Dimension(Integer.MAX_VALUE, overview.getPreferredSize().height));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        JPanel summaryCards = buildSummaryCards();
        summaryCards.setAlignmentX(LEFT_ALIGNMENT);
        content.add(overview);
        content.add(javax.swing.Box.createVerticalStrut(17));
        content.add(summaryCards);
        content.add(javax.swing.Box.createVerticalStrut(17));
        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.setAlignmentX(LEFT_ALIGNMENT);
        lower.setPreferredSize(new Dimension(900, 210));
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        lower.add(buildStatusSummary());
        lower.add(buildOverviewAction());
        lower.setMinimumSize(new Dimension(0, 210));
        content.add(lower);
        content.add(javax.swing.Box.createVerticalGlue());
        page.add(content, BorderLayout.CENTER);
        return page;
    }

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

    private JPanel buildManageEventsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);
        page.add(buildFilters(), BorderLayout.NORTH);
        eventList.setLayout(new javax.swing.BoxLayout(eventList, javax.swing.BoxLayout.Y_AXIS));
        eventList.setBackground(new Color(0x17, 0x1B, 0x24));
        eventList.setBorder(new EmptyBorder(2, 2, 2, 2));
        JScrollPane scroll = new JScrollPane(eventList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)));
        scroll.getViewport().setBackground(new Color(0x17, 0x1B, 0x24));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel empty = new JPanel(new java.awt.GridBagLayout());
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
        create.addActionListener(action -> startNewEvent());
        message.add(emptyTitle);
        message.add(javax.swing.Box.createVerticalStrut(8));
        message.add(hint);
        message.add(javax.swing.Box.createVerticalStrut(20));
        message.add(create);
        empty.add(message);
        eventState.add(empty, "empty");

        JPanel filtered = new JPanel(new GridBagLayout());
        filtered.setBackground(new Color(0x17, 0x1B, 0x24));
        JLabel filteredMessage = new JLabel("No events match these filters.");
        ViewStyles.styleSecondaryText(filteredMessage, 14f);
        filtered.add(filteredMessage);
        eventState.add(filtered, "filtered");

        JPanel table = new JPanel(new BorderLayout());
        table.setBackground(new Color(0x17, 0x1B, 0x24));
        table.add(buildInventoryHeader(), BorderLayout.NORTH);
        table.add(scroll, BorderLayout.CENTER);
        eventState.add(table, "eventsTable");
        page.add(eventState, BorderLayout.CENTER);
        return page;
    }

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

    private JPanel buildInventoryHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(new Color(0x17, 0x1B, 0x24));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A, 0x30, 0x44)));
        String[] labels = {"EVENT NAME", "DATE", "CAPACITY", "SOLD", "REMAINING", "STATUS", "ACTION"};
        double[] weights = {.30, .12, .10, .17, .10, .11, .10};
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

    private JPanel createInventoryRow(Event event) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(new Color(0x17, 0x1B, 0x24));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1E, 0x24, 0x33)));
        int capacity = event.getTicketTiers().stream().mapToInt(TicketTier::getInitialCapacity).sum();
        int sold = orderManager.calculateTotalTicketsSold(event.getEventId());
        int remaining = Math.max(0, capacity - sold);
        int percentage = capacity == 0 ? 0 : Math.min(100, (int) Math.round(sold * 100.0 / capacity));

        JPanel eventInfo = new JPanel(new GridLayout(2, 1, 0, 3));
        eventInfo.setOpaque(false);
        JLabel name = new JLabel(event.getTitle());
        ViewStyles.styleHeading(name, 12f);
        JLabel category = new JLabel(event.getCategory());
        ViewStyles.styleSecondaryText(category, 10f);
        eventInfo.add(name);
        eventInfo.add(category);
        row.add(eventInfo, columnConstraints(0, .30));

        JLabel date = monoLabel(event.getDate(), new Color(0x88, 0x96, 0xB3));
        row.add(date, columnConstraints(1, .12));
        row.add(monoLabel(Integer.toString(capacity), new Color(0xF0, 0xF4, 0xFF)), columnConstraints(2, .10));

        JPanel sales = new JPanel(new GridLayout(2, 1, 0, 3));
        sales.setOpaque(false);
        sales.add(monoLabel(Integer.toString(sold), new Color(0xF0, 0xF4, 0xFF)));
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(percentage);
        progress.setStringPainted(false);
        progress.setPreferredSize(new Dimension(90, 4));
        progress.setBorderPainted(false);
        progress.setBackground(new Color(0x2A, 0x30, 0x44));
        progress.setForeground(percentage >= 100 ? new Color(0xFF, 0x4D, 0x6A) : new Color(0x00, 0xB4, 0xFF));
        sales.add(progress);
        row.add(sales, columnConstraints(3, .17));
        row.add(monoLabel(Integer.toString(remaining), remaining == 0
                ? new Color(0xFF, 0x4D, 0x6A) : new Color(0x00, 0xE0, 0xA0)), columnConstraints(4, .10));

        JLabel status = new JLabel(statusText(event.getStatus()));
        status.setForeground(statusColor(event.getStatus()));
        status.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        row.add(status, columnConstraints(5, .11));
        row.add(buildRowActions(event), columnConstraints(6, .10));
        for (java.awt.Component component : row.getComponents()) {
            if (component instanceof javax.swing.JComponent swingComponent) {
                swingComponent.setBorder(new EmptyBorder(10, 10, 10, 6));
            }
        }
        return row;
    }

    private JPanel buildRowActions(Event event) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        actions.setOpaque(false);
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
        menu.add(attendeeItem);
        menu.add(exportItem);
        menu.add(duplicateItem);
        menu.add(archiveItem);
        more.addActionListener(action -> menu.show(more, 0, more.getHeight()));
        actions.add(edit);
        actions.add(more);
        return actions;
    }

    private GridBagConstraints columnConstraints(int column, double weight) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.weightx = weight;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private JLabel monoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(ViewStyles.DATA_FONT.deriveFont(Font.PLAIN, 11f));
        return label;
    }

    private String statusText(String status) {
        return switch (status) {
            case "ON_SALE" -> "ON SALE";
            case "SOLD_OUT" -> "SOLD OUT";
            case "CANCELLED" -> "CANCELLED";
            default -> "PAUSED";
        };
    }

    private void updateSummary(List<Event> events) {
        int capacity = events.stream().filter(event -> !event.isArchived())
                .flatMap(event -> event.getTicketTiers().stream())
                .mapToInt(TicketTier::getInitialCapacity).sum();
        int sold = events.stream().filter(event -> !event.isArchived())
                .mapToInt(event -> orderManager.calculateTotalTicketsSold(event.getEventId())).sum();
        double revenue = events.stream().filter(event -> !event.isArchived())
                .mapToDouble(event -> orderManager.calculateEventRevenue(event.getEventId())).sum();
        capacityValue.setText(String.format("%,d", capacity));
        soldValue.setText(String.format("%,d", sold));
        revenueValue.setText(String.format("KES %,.0f", revenue));
        soldHint.setText((capacity == 0 ? 0 : Math.round(sold * 100.0 / capacity)) + "% sell-through");
        capacityHint.setText(events.stream().filter(event -> !event.isArchived()).count() + " active events");
    }

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

    private void showPage(String page) {
        activePage = page;
        String title = switch (page) {
            case "Overview" -> "Overview";
            case "Settings" -> "Settings";
            case "Editor" -> editingEvent == null ? "Create New Event" : "Edit Event";
            default -> "Active Events Inventory";
        };
        pageTitle.setText(title);
        pageDescription.setText(switch (page) {
            case "Overview" -> "Your ticket sales at a glance";
            case "Settings" -> "Account and appearance";
            case "Editor" -> "Configure event details and ticket tiers";
            default -> "Manage and monitor all active events";
        });
        createEventButton.setText("Editor".equals(page) ? "Back to events" : "+  Create New Event");
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(page) || "Editor".equals(page) && "Manage Events".equals(entry.getKey());
            JButton button = entry.getValue();
            button.setBackground(selected ? new Color(0x00, 0xB4, 0xFF, 28) : new Color(0x17, 0x1B, 0x24));
            button.setForeground(selected ? new Color(0x00, 0xB4, 0xFF) : new Color(0x88, 0x96, 0xB3));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 2, 0, 0,
                            selected ? new Color(0x00, 0xB4, 0xFF) : new Color(0x17, 0x1B, 0x24)),
                    new EmptyBorder(11, 11, 11, 8)));
        }
        if (pages.getComponentCount() > 0) {
            pageLayout.show(pages, page);
        }
        revalidate();
        repaint();
    }

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

    private JButton smallButton(String text) {
        JButton button = new JButton(text);
        ViewStyles.styleSecondaryButton(button);
        button.setFont(button.getFont().deriveFont(12f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE), new EmptyBorder(5, 8, 5, 8)));
        return button;
    }

    private void showAttendees(Event event) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"Receipt", "Guest", "Email", "Tier", "Qty", "Total (KES)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        orderManager.getBookingsByEvent(event.getEventId()).forEach(booking -> model.addRow(new Object[] {
            booking.getBookingId(), booking.getCustomerName(), booking.getCustomerEmail(),
            booking.getTierName(), booking.getQuantity(), booking.getTotalPrice()
        }));
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(780, 330));
        JOptionPane.showMessageDialog(this, scroll, event.getTitle() + " attendees",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void exportAttendees(Event event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(event.getTitle().replaceAll("[^A-Za-z0-9_-]", "_") + "-attendees.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (var writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            writer.write("Receipt,Guest,Email,Tier,Quantity,Total KES");
            writer.newLine();
            for (var booking : orderManager.getBookingsByEvent(event.getEventId())) {
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

    private String csv(String value) {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void duplicateEvent(Event event) {
        Event copy = eventManager.duplicateEvent(event.getEventId(), hostId);
        refreshEvents();
        loadEvent(copy);
    }

    private void toggleArchive(Event event) {
        boolean archive = !event.isArchived();
        int answer = JOptionPane.showConfirmDialog(this,
                archive ? "Archive this event? It will no longer appear in the public catalog."
                        : "Restore this event to your dashboard and the public catalog?",
                "Confirm event change", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            eventManager.setArchived(event.getEventId(), archive);
            refreshEvents();
        }
    }

    private Color statusColor(String status) {
        return switch (status) {
            case "ON_SALE" -> new Color(0x1F, 0x8A, 0x55);
            case "SOLD_OUT" -> new Color(0xB8, 0x3A, 0x3A);
            case "CANCELLED" -> new Color(0x73, 0x36, 0x36);
            default -> new Color(0x8A, 0x67, 0x24);
        };
    }

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
        form.add(buildTierManager(), constraints);

        constraints.gridy = 11;
        form.add(buildCustomFields(), constraints);

        JButton save = new JButton("Save Event");
        ViewStyles.stylePrimaryButton(save);
        save.addActionListener(action -> saveEvent());
        JButton fresh = new JButton("New Event");
        ViewStyles.styleSecondaryButton(fresh);
        fresh.addActionListener(action -> startNewEvent());
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
        customFieldModel.addElement(new CustomField(name, value));
        customNameField.setText("");
        customValueField.setText("");
    }

    private void chooseBanner() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            bannerPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void chooseAccentColor() {
        Color chosen = JColorChooser.showDialog(this, "Choose event accent", selectedAccent);
        if (chosen != null) {
            selectedAccent = chosen;
            accentColorButton.setBackground(chosen);
        }
    }

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
        button.gridheight = 3;
        button.insets = new Insets(0, 8, 0, 0);
        editor.add(tierActionButton, button);
        ViewStyles.stylePrimaryButton(tierActionButton);
        tierActionButton.addActionListener(action -> addOrUpdateTier());

        tierList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tierList.setBackground(ViewStyles.SURFACE);
        tierList.setForeground(ViewStyles.FOREGROUND);
        tierList.setFixedCellHeight(34);
        tierList.setCellRenderer((list, tier, index, selected, focus) -> {
            JLabel row = new JLabel(String.format("%s  |  KES %,.2f  |  %d / %d available",
                    tier.getTierName(), tier.getPrice(), tier.getAvailableSeats(), tier.getInitialCapacity()));
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(4, 8, 4, 8));
            row.setBackground(selected ? ViewStyles.ACCENT : ViewStyles.SURFACE);
            row.setForeground(ViewStyles.FOREGROUND);
            return row;
        });
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

    private void styleDateChooser(JDateChooser chooser) {
        if (chooser.getDateEditor().getUiComponent() instanceof JTextField editor) {
            ViewStyles.styleInput(editor);
        }
        chooser.setBorder(BorderFactory.createLineBorder(ViewStyles.OUTLINE));
        chooser.setPreferredSize(new Dimension(260, 36));
    }

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

    private record CustomField(String name, String value) {
        @Override
        public String toString() {
            return name + ": " + value;
        }
    }
}