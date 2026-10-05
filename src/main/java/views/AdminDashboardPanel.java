package views;

import controllers.EventManager;
import controllers.OrderManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
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
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField titleField = new JTextField(28);
    private final JTextField dateField = new JTextField(28);
    private final JTextField venueField = new JTextField(28);
    private final JTextField salesStartField = new JTextField(28);
    private final JTextField salesEndField = new JTextField(28);
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
        for (Event event : events) {
            eventList.add(createEventCard(event));
            eventList.add(javax.swing.Box.createVerticalStrut(12));
        }
        if (events.isEmpty() && !allEvents.isEmpty()) {
            JLabel noMatches = new JLabel("No events match these filters.", JLabel.CENTER);
            ViewStyles.styleSecondaryText(noMatches, 15f);
            noMatches.setAlignmentX(CENTER_ALIGNMENT);
            eventList.add(noMatches);
        }
        java.awt.CardLayout layout = (java.awt.CardLayout) eventState.getLayout();
        layout.show(eventState, allEvents.isEmpty() ? "empty" : "events");
        eventList.revalidate();
        eventList.repaint();
    }

    private void buildPanel(String hostName) {
        setLayout(new BorderLayout(0, 14));
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(20, 26, 22, 26));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel headings = new JPanel(new java.awt.GridLayout(2, 1, 0, 3));
        headings.setOpaque(false);
        JLabel title = new JLabel("Host dashboard");
        ViewStyles.styleHeading(title, 24f);
        JLabel host = new JLabel(hostName);
        ViewStyles.styleSecondaryText(host, 14f);
        headings.add(title);
        headings.add(host);
        header.add(headings, BorderLayout.WEST);
        JButton logout = new JButton("Log out");
        ViewStyles.styleSecondaryButton(logout);
        logout.addActionListener(action -> logoutAction.run());
        header.add(logout, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs.addTab("My Events", buildOverview());
        tabs.addTab("Create / Edit Event", buildManagementForm());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildOverview() {
        eventList.setLayout(new javax.swing.BoxLayout(eventList, javax.swing.BoxLayout.Y_AXIS));
        eventList.setBackground(ViewStyles.BACKGROUND);
        eventList.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane scroll = new JScrollPane(eventList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ViewStyles.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        eventState.add(scroll, "events");

        JPanel empty = new JPanel(new java.awt.GridBagLayout());
        empty.setBackground(ViewStyles.BACKGROUND);
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

        ViewStyles.styleInput(searchField);
        ViewStyles.styleInput(dateFromFilter);
        ViewStyles.styleInput(dateToFilter);
        archivedToggle.setOpaque(false);
        archivedToggle.setForeground(ViewStyles.FOREGROUND);
        JButton applyFilters = new JButton("Filter");
        ViewStyles.styleSecondaryButton(applyFilters);
        applyFilters.addActionListener(action -> refreshEvents());
        searchField.addActionListener(action -> refreshEvents());
        statusFilter.addActionListener(action -> refreshEvents());
        archivedToggle.addActionListener(action -> refreshEvents());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filters.setOpaque(false);
        filters.add(new JLabel("Search"));
        filters.add(searchField);
        filters.add(statusFilter);
        filters.add(new JLabel("From"));
        filters.add(dateFromFilter);
        filters.add(new JLabel("To"));
        filters.add(dateToFilter);
        filters.add(archivedToggle);
        filters.add(applyFilters);

        JPanel overview = new JPanel(new BorderLayout(0, 10));
        overview.setBackground(ViewStyles.BACKGROUND);
        overview.add(filters, BorderLayout.NORTH);
        overview.add(eventState, BorderLayout.CENTER);
        return overview;
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

    private JPanel createEventCard(Event event) {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(ViewStyles.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 5, 1, 1, eventAccent(event)),
            new EmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 176));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 17f);
        titleRow.add(title, BorderLayout.WEST);
        JLabel status = new JLabel(event.getStatus().replace('_', ' '));
        status.setOpaque(true);
        status.setForeground(Color.WHITE);
        status.setBackground(statusColor(event.getStatus()));
        status.setBorder(new EmptyBorder(4, 9, 4, 9));
        titleRow.add(status, BorderLayout.EAST);
        card.add(titleRow, BorderLayout.NORTH);

        int capacity = event.getTicketTiers().stream().mapToInt(TicketTier::getInitialCapacity).sum();
        JLabel metrics = new JLabel(String.format(
                "%s  |  %s  |  %s  |  Tickets sold: %d / %d  |  Revenue: KES %,.2f",
                event.getCategory(), event.getDate(), event.getVenue(),
                orderManager.calculateTotalTicketsSold(event.getEventId()), capacity,
                orderManager.calculateEventRevenue(event.getEventId())));
        ViewStyles.styleSecondaryText(metrics, 13f);
        card.add(metrics, BorderLayout.CENTER);

        JButton manage = smallButton("Edit");
        manage.addActionListener(action -> loadEvent(event));
        JButton attendees = smallButton("Attendees");
        attendees.addActionListener(action -> showAttendees(event));
        JButton export = smallButton("Export CSV");
        export.addActionListener(action -> exportAttendees(event));
        JButton duplicate = smallButton("Duplicate");
        duplicate.addActionListener(action -> duplicateEvent(event));
        JButton archive = smallButton(event.isArchived() ? "Restore" : "Archive");
        archive.addActionListener(action -> toggleArchive(event));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(manage);
        actions.add(attendees);
        actions.add(export);
        actions.add(duplicate);
        actions.add(archive);
        card.add(actions, BorderLayout.SOUTH);

        if (!event.getBannerImagePath().isBlank()) {
            java.io.File imageFile = new java.io.File(event.getBannerImagePath());
            if (imageFile.isFile()) {
                ImageIcon source = new ImageIcon(event.getBannerImagePath());
                ImageIcon scaled = new ImageIcon(source.getImage().getScaledInstance(112, 78,
                        java.awt.Image.SCALE_SMOOTH));
                JLabel banner = new JLabel(scaled);
                banner.setBorder(new EmptyBorder(0, 0, 0, 12));
                card.add(banner, BorderLayout.WEST);
            }
        }
        return card;
    }

    private JButton smallButton(String text) {
        JButton button = new JButton(text);
        ViewStyles.styleSecondaryButton(button);
        button.setFont(button.getFont().deriveFont(12f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE), new EmptyBorder(5, 8, 5, 8)));
        return button;
    }

    private Color eventAccent(Event event) {
        try {
            return Color.decode(event.getAccentColorHex());
        } catch (RuntimeException exception) {
            return ViewStyles.ACCENT;
        }
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
        addFormField(form, "Event title", titleField, 0);
        addFormField(form, "Date (YYYY-MM-DD)", dateField, 1);
        addFormField(form, "Venue", venueField, 2);
        addFormField(form, "Sales start (optional)", salesStartField, 3);
        addFormField(form, "Sales end (optional)", salesEndField, 4);
        addFormField(form, "Sales status", statusBox, 5);
        addFormField(form, "Category", categoryField, 6);
        addFormField(form, "Banner image", buildBannerField(), 7);
        addFormField(form, "Card accent", accentColorButton, 8);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 0, 5, 0);
        form.add(buildCustomFields(), constraints);

        constraints.gridy = 10;
        form.add(buildTierManager(), constraints);

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
        constraints.gridy = 11;
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
        addTierField(editor, "Price (KES)", priceSpinner, 0, 1);
        addTierField(editor, "Capacity", capacitySpinner, 0, 2);
        GridBagConstraints button = new GridBagConstraints();
        button.gridx = 3;
        button.gridy = 0;
        button.gridheight = 2;
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
        ViewStyles.styleInput(dateField);
        ViewStyles.styleInput(venueField);
        ViewStyles.styleInput(salesStartField);
        ViewStyles.styleInput(salesEndField);
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
        String title = titleField.getText().trim();
        String date = dateField.getText().trim();
        String venue = venueField.getText().trim();
        if (title.isEmpty() || date.isEmpty() || venue.isEmpty()) {
            showError("Complete the title, date, and venue fields.");
            return;
        }
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            showError("Enter the date in YYYY-MM-DD format.");
            return;
        }
        LocalDate salesStart = parseOptionalDate(salesStartField.getText(), "sales start");
        LocalDate salesEnd = parseOptionalDate(salesEndField.getText(), "sales end");
        if ((!salesStartField.getText().isBlank() && salesStart == null)
                || (!salesEndField.getText().isBlank() && salesEnd == null)) {
            return;
        }
        if (salesStart != null && salesEnd != null && salesEnd.isBefore(salesStart)) {
            showError("Sales end date cannot be before sales start date.");
            return;
        }
        if (tierModel.isEmpty()) {
            showError("Add at least one ticket tier before saving.");
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
                    title, date, venue, tiers, status, hostId);
        } else {
            savedEvent = editingEvent;
        }
        savedEvent.setTitle(title);
        savedEvent.setDate(date);
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
        startNewEvent();
        tabs.setSelectedIndex(0);
        JOptionPane.showMessageDialog(this, "Event saved successfully.", "Event Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadEvent(Event event) {
        editingEvent = event;
        titleField.setText(event.getTitle());
        dateField.setText(event.getDate());
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
        salesStartField.setText(event.getSalesStartDate() == null ? "" : event.getSalesStartDate().toString());
        salesEndField.setText(event.getSalesEndDate() == null ? "" : event.getSalesEndDate().toString());
        statusBox.setSelectedItem(event.getStatus());
        tierModel.clear();
        for (TicketTier tier : event.getTicketTiers()) {
            tierModel.addElement(new TicketTier(tier.getTierName(), tier.getPrice(),
                    tier.getInitialCapacity(), tier.getAvailableSeats()));
        }
        clearTierEditor();
        tabs.setSelectedIndex(1);
    }

    private void startNewEvent() {
        editingEvent = null;
        titleField.setText("");
        dateField.setText("");
        venueField.setText("");
        categoryField.setText("General");
        bannerPathField.setText("");
        selectedAccent = ViewStyles.ACCENT;
        accentColorButton.setBackground(selectedAccent);
        customFieldModel.clear();
        salesStartField.setText("");
        salesEndField.setText("");
        statusBox.setSelectedItem("ON_SALE");
        tierModel.clear();
        tierList.clearSelection();
        clearTierEditor();
        tabs.setSelectedIndex(1);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check Event Details", JOptionPane.WARNING_MESSAGE);
    }

    private LocalDate parseOptionalDate(String value, String label) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            showError("Enter " + label + " in YYYY-MM-DD format, or leave it blank.");
            return null;
        }
    }

    private record CustomField(String name, String value) {
        @Override
        public String toString() {
            return name + ": " + value;
        }
    }
}