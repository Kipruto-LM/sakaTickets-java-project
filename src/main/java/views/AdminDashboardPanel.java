package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import models.Event;
import models.TicketTier;

/**
 * A simple host screen for viewing, creating, and editing events.
 *
 * This is a Swing view: it collects input and displays Event objects.
 * EventManager owns the event list and applies changes to that shared data.
 *
 * Core concepts:
 * - Inheritance: this screen extends JPanel to become a reusable Swing component.
 * - Encapsulation: private fields hold the form state and selected Event.
 * - Composition: the panel is built from lists, labels, fields, buttons, and panels.
 * - Abstraction: EventManager handles persistence while this class handles presentation.
 * - Callbacks: button listeners respond to user actions without owning database logic.
 */
public class AdminDashboardPanel extends JPanel {
    private static final String[] STATUSES = {"ON_SALE", "PAUSED", "CANCELLED", "SOLD_OUT"};

    private final EventManager eventManager;
    private final String hostId;
    private final Runnable logoutAction;

    // The JList displays these Event objects; selecting a row loads it into the form.
    private final DefaultListModel<Event> eventListModel = new DefaultListModel<>();
    private final JList<Event> eventList = new JList<>(eventListModel);

    // Form controls hold the values currently being created or edited.
    private final JTextField titleField = new JTextField(22);
    private final JTextField dateField = new JTextField(22);
    private final JTextField venueField = new JTextField(22);
    private final JComboBox<String> statusField = new JComboBox<>(STATUSES);
    private final JSpinner priceField = new JSpinner(new SpinnerNumberModel(1000.0, 1.0, 100000000.0, 100.0));
    private final JSpinner capacityField = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
    private final JLabel formMessage = new JLabel(" ");

    // A selected event means the form is editing; null means it is creating a new event.
    private Event editingEvent;

    /** Creates the host dashboard with shared event data and a logout callback. */
    public AdminDashboardPanel(EventManager eventManager, String hostId, String hostName, Runnable logoutAction) {
        this.eventManager = eventManager;
        this.hostId = hostId;
        this.logoutAction = logoutAction;

        buildScreen(hostName);
        refreshEvents();
        startNewEvent();
    }

    /** Reloads the list from EventManager so the screen reflects current shared data. */
    private void refreshEvents() {
        eventListModel.clear();
        // Filter by owner so one host cannot see another host's dashboard entries.
        for (Event event : eventManager.getEventsByHostId(hostId)) {
            eventListModel.addElement(event);
        }
    }

    /** Combines a header, event list, and editor into one straightforward screen. */
    private void buildScreen(String hostName) {
        setLayout(new BorderLayout(16, 16));
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(buildHeader(hostName), BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 18, 0));
        content.setOpaque(false);
        content.add(buildEventList());
        content.add(buildEventForm());
        add(content, BorderLayout.CENTER);
    }

    /** Builds the dashboard title and connects the logout button to MainFrame. */
    private JPanel buildHeader(String hostName) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel welcome = new JPanel(new GridLayout(2, 1, 0, 4));
        welcome.setOpaque(false);
        JLabel title = new JLabel("Your Events");
        ViewStyles.styleHeading(title, 24f);
        JLabel subtitle = new JLabel("Signed in as " + hostName);
        ViewStyles.styleSecondaryText(subtitle, 13f);
        welcome.add(title);
        welcome.add(subtitle);
        header.add(welcome, BorderLayout.WEST);

        JButton logout = new JButton("Log out");
        ViewStyles.styleSecondaryButton(logout);
        logout.addActionListener(event -> logoutAction.run());
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    /** Builds the host-owned event list and its selection/create actions. */
    private JPanel buildEventList() {
        JPanel panel = sectionPanel("Your event list");
        eventList.setBackground(ViewStyles.SURFACE);
        eventList.setForeground(ViewStyles.FOREGROUND);
        eventList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // A renderer controls how each Event object is represented as a row.
        eventList.setCellRenderer((list, event, index, selected, focused) -> {
            String text = event == null ? "" : event.getTitle() + "  |  " + event.getDate();
            JLabel row = new JLabel(text);
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(10, 12, 10, 12));
            row.setBackground(selected ? ViewStyles.ACCENT : ViewStyles.SURFACE);
            row.setForeground(ViewStyles.FOREGROUND);
            return row;
        });
        eventList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedEvent();
            }
        });

        JScrollPane listScroll = new JScrollPane(eventList);
        listScroll.setPreferredSize(new Dimension(300, 400));
        panel.add(listScroll, BorderLayout.CENTER);

        JButton newEvent = new JButton("Create new event");
        ViewStyles.stylePrimaryButton(newEvent);
        newEvent.addActionListener(event -> startNewEvent());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        actions.setOpaque(false);
        actions.add(newEvent);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    /** Builds the editor fields and save action for creating or editing an event. */
    private JPanel buildEventForm() {
        JPanel panel = sectionPanel("Event details");
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 7));
        fields.setOpaque(false);

        addField(fields, "Event title", titleField);
        addField(fields, "Date (YYYY-MM-DD)", dateField);
        addField(fields, "Venue", venueField);
        addField(fields, "Status", statusField);

        JLabel ticketHeading = new JLabel("Starting ticket tier (new events)");
        ViewStyles.styleHeading(ticketHeading, 15f);
        ticketHeading.setBorder(new EmptyBorder(12, 0, 2, 0));
        fields.add(ticketHeading);
        addField(fields, "Ticket price (KES)", priceField);
        addField(fields, "Ticket capacity", capacityField);

        JLabel note = new JLabel("Editing an event keeps its existing ticket tiers.");
        ViewStyles.styleSecondaryText(note, 12f);
        fields.add(note);

        formMessage.setForeground(new Color(0xFF, 0x4D, 0x6A));
        fields.add(formMessage);

        JButton save = new JButton("Save event");
        ViewStyles.stylePrimaryButton(save);
        save.addActionListener(event -> saveEvent());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        actions.setOpaque(false);
        actions.add(save);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    /** Gives the list and form the same simple titled-card appearance. */
    private JPanel sectionPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(ViewStyles.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                new EmptyBorder(16, 16, 16, 16)));
        JLabel heading = new JLabel(title);
        ViewStyles.styleHeading(heading, 17f);
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    /** Adds a label and its input to the form. */
    private void addField(JPanel form, String label, java.awt.Component input) {
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 13f);
        form.add(fieldLabel);
        if (input instanceof JTextField textField) {
            ViewStyles.styleInput(textField);
        }
        form.add(input);
    }

    /** Copies the selected Event's values into the form for editing. */
    private void loadSelectedEvent() {
        Event selected = eventList.getSelectedValue();
        if (selected == null) {
            return;
        }

        editingEvent = selected;
        titleField.setText(selected.getTitle());
        dateField.setText(selected.getDate());
        venueField.setText(selected.getVenue());
        statusField.setSelectedItem(selected.getStatus());
        if (!selected.getTicketTiers().isEmpty()) {
            TicketTier firstTier = selected.getTicketTiers().get(0);
            priceField.setValue(firstTier.getPrice());
            capacityField.setValue(firstTier.getInitialCapacity());
        }
        priceField.setEnabled(false);
        capacityField.setEnabled(false);
        formMessage.setText("Editing " + selected.getTitle());
    }

    /** Clears the selection and prepares the form for a new event. */
    private void startNewEvent() {
        editingEvent = null;
        eventList.clearSelection();
        titleField.setText("");
        dateField.setText("");
        venueField.setText("");
        statusField.setSelectedItem("ON_SALE");
        priceField.setValue(1000.0);
        capacityField.setValue(50);
        priceField.setEnabled(true);
        capacityField.setEnabled(true);
        formMessage.setText(" ");
    }

    /**
     * Validates the simple form, creates or updates an Event, and delegates
     * saving it to EventManager.
     */
    private void saveEvent() {
        String title = titleField.getText().trim();
        String date = dateField.getText().trim();
        String venue = venueField.getText().trim();

        if (title.isEmpty() || venue.isEmpty()) {
            showFormError("Enter an event title and venue.");
            return;
        }
        // Convert the text date to a real date once; parsing rejects invalid calendar dates.
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            showFormError("Enter the date as YYYY-MM-DD.");
            return;
        }

        String status = (String) statusField.getSelectedItem();
        try {
            if (editingEvent == null) {
                createEvent(title, date, venue, status);
            } else {
                updateEvent(title, date, venue, status);
            }
        } catch (SQLException exception) {
            showFormError("Could not save event: " + exception.getMessage());
            return;
        }

        refreshEvents();
        startNewEvent();
        formMessage.setText("Event saved.");
    }

    /** New events start with one Regular tier to keep ticket setup easy to follow. */
    private void createEvent(String title, String date, String venue, String status) throws SQLException {
        double price = ((Number) priceField.getValue()).doubleValue();
        int capacity = ((Number) capacityField.getValue()).intValue();
        TicketTier regular = new TicketTier("Regular", price, capacity);
        Event event = new Event("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                title, date, venue, List.of(regular), status, hostId);
        eventManager.addEvent(event);
    }

    /** Editing changes basic details and leaves already-booked ticket tiers alone. */
    private void updateEvent(String title, String date, String venue, String status) throws SQLException {
        Event updatedEvent = new Event(editingEvent.getEventId(), title, date, venue,
                editingEvent.getTicketTiers(), status, editingEvent.getHostId());
        updatedEvent.setCategory(editingEvent.getCategory());
        eventManager.updateEvent(updatedEvent);
    }

    /** Displays a validation or persistence error beside the form controls. */
    private void showFormError(String message) {
        formMessage.setText(message);
    }
}
