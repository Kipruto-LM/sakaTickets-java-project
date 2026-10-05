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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
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
    private final JTextField tierNameField = new JTextField(12);
    private final JSpinner priceSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 1.0, 100000000.0, 100.0));
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
    private final JComboBox<String> statusBox = new JComboBox<>(EVENT_STATUSES);
    private final DefaultListModel<TicketTier> tierModel = new DefaultListModel<>();
    private final JList<TicketTier> tierList = new JList<>(tierModel);
    private final JButton tierActionButton = new JButton("Add Tier");
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
        List<Event> events = eventManager.getEventsByHostId(hostId);
        for (Event event : events) {
            eventList.add(createEventCard(event));
            eventList.add(javax.swing.Box.createVerticalStrut(12));
        }
        java.awt.CardLayout layout = (java.awt.CardLayout) eventState.getLayout();
        layout.show(eventState, events.isEmpty() ? "empty" : "events");
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
        return eventState;
    }

    private JPanel createEventCard(Event event) {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(ViewStyles.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE), new EmptyBorder(14, 16, 14, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));

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
                "%s  |  %s  |  Tickets sold: %d / %d  |  Revenue: KES %,.2f",
                event.getDate(), event.getVenue(), orderManager.calculateTotalTicketsSold(event.getEventId()),
                capacity, orderManager.calculateEventRevenue(event.getEventId())));
        ViewStyles.styleSecondaryText(metrics, 13f);
        card.add(metrics, BorderLayout.CENTER);

        JButton manage = new JButton("Edit / Manage");
        ViewStyles.styleSecondaryButton(manage);
        manage.addActionListener(action -> loadEvent(event));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(manage);
        card.add(actions, BorderLayout.SOUTH);
        return card;
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

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 0, 5, 0);
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
        constraints.gridy = 7;
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
        ViewStyles.styleInput(tierNameField);
        styleSpinner(priceSpinner);
        styleSpinner(capacitySpinner);
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
        if (editingEvent == null) {
            Event created = new Event("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    title, date, venue, tiers, status, hostId);
                created.setSalesStartDate(salesStart);
                created.setSalesEndDate(salesEnd);
            eventManager.addEvent(created);
        } else {
            editingEvent.setTitle(title);
            editingEvent.setDate(date);
            editingEvent.setVenue(venue);
            editingEvent.setTicketTiers(tiers);
            editingEvent.setStatus(status);
            editingEvent.setSalesStartDate(salesStart);
            editingEvent.setSalesEndDate(salesEnd);
            eventManager.updateEvent(editingEvent);
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
}