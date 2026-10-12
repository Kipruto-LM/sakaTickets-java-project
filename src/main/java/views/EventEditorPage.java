// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import com.toedter.calendar.JDateChooser;     // A date picker with a calendar (outside library from pom.xml)
import controllers.EventManager;              // Saves new and edited events
import java.awt.BorderLayout;                 // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                        // An RGB colour
import java.awt.Dimension;                    // A width and height
import java.awt.FlowLayout;                   // Layout that places items in a row
import java.awt.Font;                         // Font style constants
import java.awt.GridBagConstraints;           // Position/size rules for one item in a GridBagLayout
import java.awt.GridBagLayout;                // A flexible grid layout
import java.awt.GridLayout;                   // Layout with equal-sized cells
import java.awt.Insets;                       // Spacing around an item
import java.time.LocalDate;                   // A date without a time
import java.time.ZoneId;                      // A time zone (needed to convert Date <-> LocalDate)
import java.util.ArrayList;                   // A resizable list
import java.util.Date;                        // The older date type that JDateChooser uses
import java.util.LinkedHashMap;               // A map that keeps insertion order
import java.util.List;                        // The general list type
import java.util.Map;                         // The general map type
import java.util.UUID;                        // Random unique IDs for new events
import javax.swing.BorderFactory;             // Helper for making borders
import javax.swing.DefaultListModel;          // Holds the items shown in a JList
import javax.swing.JButton;                   // A clickable button
import javax.swing.JComboBox;                 // A drop-down list
import javax.swing.JFileChooser;              // A pop-up file picker
import javax.swing.JLabel;                    // A piece of text
import javax.swing.JList;                     // A list of items
import javax.swing.JOptionPane;               // Pop-up message boxes
import javax.swing.JPanel;                    // A container
import javax.swing.JScrollPane;               // Adds scroll bars
import javax.swing.JSpinner;                  // A number box with up/down arrows
import javax.swing.JTextField;                // A one-line text box
import javax.swing.ListSelectionModel;        // Rules for selecting items in a JList
import javax.swing.SpinnerNumberModel;        // Value, minimum, maximum and step for a JSpinner
import javax.swing.border.EmptyBorder;        // Invisible padding
import javax.swing.filechooser.FileNameExtensionFilter;   // Limits the file picker to certain file types
import models.Event;                          // The event being created or edited
import models.TicketTier;                     // A ticket price level

/**
 * The dashboard's event editor: one form for both creating a new event and editing an existing one.
 *
 * Form, top to bottom: an error line, the event fields (title, date, venue, sales dates, status,
 * category, banner), the ticket tiers, the custom details, and the New / Save buttons.
 *
 * OOP ideas shown here:
 * - Single responsibility: this class only handles the form. Saving is done by EventManager.
 * - Objects working together: the form builds TicketTier and Event objects from what the host
 *   typed, then hands the Event to the controller.
 * - Records: CustomField (at the bottom) is a tiny data class Java writes for us.
 */
class EventEditorPage extends JPanel {
    private static final String[] STATUSES = {"ON_SALE", "PAUSED", "CANCELLED", "SOLD_OUT"};

    private final EventManager eventManager;
    private final String hostId;                 // New events belong to this host
    private final Runnable savedAction;          // Callback: run after a successful save

    // Event fields.
    private final JLabel errorLabel = new JLabel(" ");
    private final JTextField titleField = new JTextField(28);
    private final JDateChooser dateChooser = new JDateChooser();
    private final JTextField venueField = new JTextField(28);
    private final JDateChooser salesStartChooser = new JDateChooser();
    private final JDateChooser salesEndChooser = new JDateChooser();
    private final JComboBox<String> statusBox = new JComboBox<>(STATUSES);
    private final JTextField categoryField = new JTextField(28);
    private final JTextField bannerPathField = new JTextField(22);
    // Ticket tier editor: inputs, the list of tiers, and the Add/Update button.
    private final JTextField tierNameField = new JTextField(12);
    private final JSpinner priceSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 1.0, 100000000.0, 100.0));
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(50, 1, 100000, 1));
    private final DefaultListModel<TicketTier> tierModel = new DefaultListModel<>();
    private final JList<TicketTier> tierList = new JList<>(tierModel);
    private final JButton tierButton = new JButton("Add Tier");
    // Custom details editor.
    private final JTextField detailNameField = new JTextField(12);
    private final JTextField detailValueField = new JTextField(18);
    private final DefaultListModel<CustomField> detailModel = new DefaultListModel<>();
    private final JList<CustomField> detailList = new JList<>(detailModel);
    // The event being edited, or null when creating a new one.
    private Event editingEvent;

    // Constructor: builds the form once. startNewEvent() / loadEvent() fill it in later.
    EventEditorPage(EventManager eventManager, String hostId, Runnable savedAction) {
        this.eventManager = eventManager;
        this.hostId = hostId;
        this.savedAction = savedAction;
        setLayout(new BorderLayout());
        setBackground(ViewStyles.BACKGROUND);
        styleInputs();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ViewStyles.BACKGROUND);
        form.setBorder(new EmptyBorder(14, 8, 18, 8));
        // Row 0: the red error message (a space keeps its height when empty).
        errorLabel.setForeground(new Color(0xFF, 0x4D, 0x6A));
        errorLabel.setFont(ViewStyles.BODY_FONT.deriveFont(Font.BOLD, 12f));
        form.add(errorLabel, fullWidth(0, 14));
        // Rows 1-7: the event fields. "*" marks required fields.
        addField(form, "Event title *", titleField, 1);
        addField(form, "Event date *", dateChooser, 2);
        addField(form, "Venue *", venueField, 3);
        addField(form, "Sales start (optional)", salesStartChooser, 4);
        addField(form, "Sales end (optional)", salesEndChooser, 5);
        addField(form, "Sales status", statusBox, 6);
        addField(form, "Category", categoryField, 7);
        addField(form, "Banner image", buildBannerField(), 8);
        // Rows 9-10: tiers and custom details.
        form.add(buildTierSection(), fullWidth(9, 5));
        form.add(buildDetailSection(), fullWidth(10, 5));
        // Row 11: buttons on the right.
        JButton fresh = new JButton("New Event");
        ViewStyles.styleSecondaryButton(fresh);
        fresh.addActionListener(action -> startNewEvent());
        JButton save = new JButton("Save Event");
        ViewStyles.stylePrimaryButton(save);
        save.addActionListener(action -> saveEvent());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(fresh);
        buttons.add(save);
        form.add(buttons, fullWidth(11, 0));

        JScrollPane scroll = new JScrollPane(form);              // The form scrolls if the window is short
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ViewStyles.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // True when an existing event is open (used for the page title "Edit Event" vs "Create New Event").
    boolean isEditingExistingEvent() {
        return editingEvent != null;
    }

    // Empties the form, ready for a brand-new event.
    void startNewEvent() {
        editingEvent = null;                         // null = "creating, not editing"
        titleField.setText("");
        dateChooser.setDate(null);
        venueField.setText("");
        salesStartChooser.setDate(null);
        salesEndChooser.setDate(null);
        statusBox.setSelectedItem("ON_SALE");        // New events start on sale
        categoryField.setText("General");
        bannerPathField.setText("");
        tierModel.clear();
        detailModel.clear();
        clearTierInputs();
        errorLabel.setText(" ");
    }

    // Fills the form from an existing Event, using its getters.
    void loadEvent(Event event) {
        editingEvent = event;                        // Remember which event we're editing
        titleField.setText(event.getTitle());
        dateChooser.setDate(toDate(LocalDate.parse(event.getDate())));   // Text -> LocalDate -> Date
        venueField.setText(event.getVenue());
        salesStartChooser.setDate(toDate(event.getSalesStartDate()));
        salesEndChooser.setDate(toDate(event.getSalesEndDate()));
        statusBox.setSelectedItem(event.getStatus());
        categoryField.setText(event.getCategory());
        bannerPathField.setText(event.getBannerImagePath());
        // Copy each tier into the list. Copies mean nothing changes on the real event until Save.
        tierModel.clear();
        for (TicketTier tier : event.getTicketTiers()) {
            tierModel.addElement(new TicketTier(tier.getTierName(), tier.getPrice(),
                    tier.getInitialCapacity(), tier.getAvailableSeats()));
        }
        // Copy each custom detail into the list as a CustomField record.
        detailModel.clear();
        for (Map.Entry<String, String> detail : event.getCustomDetails().entrySet()) {
            detailModel.addElement(new CustomField(detail.getKey(), detail.getValue()));
        }
        clearTierInputs();
        errorLabel.setText(" ");
    }

    // ---- Saving ----

    // Checks the form, then creates a new Event or updates the one being edited, and saves it.
    private void saveEvent() {
        String title = titleField.getText().trim();
        String venue = venueField.getText().trim();
        LocalDate eventDate = toLocalDate(dateChooser.getDate());          // null if no date picked
        LocalDate salesStart = toLocalDate(salesStartChooser.getDate());
        LocalDate salesEnd = toLocalDate(salesEndChooser.getDate());
        // Check 1: required fields.
        if (title.isEmpty() || eventDate == null || venue.isEmpty()) {
            showError("Required: add an event title, event date, and venue.");
            return;
        }
        // Check 2: the sales window can't end before it starts.
        if (salesStart != null && salesEnd != null && salesEnd.isBefore(salesStart)) {
            showError("Sales end cannot be before sales start.");
            return;
        }
        // Check 3: at least one ticket tier.
        if (tierModel.isEmpty()) {
            showError("Add at least one ticket tier before saving.");
            return;
        }

        // Gather the tiers and custom details from the two lists.
        List<TicketTier> tiers = new ArrayList<>();
        for (int i = 0; i < tierModel.size(); i++) {
            tiers.add(tierModel.get(i));
        }
        Map<String, String> details = new LinkedHashMap<>();
        for (int i = 0; i < detailModel.size(); i++) {
            details.put(detailModel.get(i).name(), detailModel.get(i).value());
        }
        String status = (String) statusBox.getSelectedItem();

        // New event: make an Event object with a random ID like "EVT-3F9A21BC". Editing: reuse it.
        Event event = editingEvent;
        if (event == null) {
            String id = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            event = new Event(id, title, eventDate.toString(), venue, tiers, status, hostId);
        }
        // Copy everything from the form into the Event with its setters.
        event.setTitle(title);
        event.setDate(eventDate.toString());
        event.setVenue(venue);
        event.setTicketTiers(tiers);
        event.setStatus(status);
        event.setSalesStartDate(salesStart);
        event.setSalesEndDate(salesEnd);
        event.setCategory(categoryField.getText().trim());
        event.setBannerImagePath(bannerPathField.getText().trim());
        event.setCustomDetails(details);
        // Hand it to the controller: add a new event, or replace the old version.
        if (editingEvent == null) {
            eventManager.addEvent(event);
        } else {
            eventManager.updateEvent(event);
        }
        startNewEvent();                             // Reset the form
        savedAction.run();                           // Tell the dashboard (it refreshes and shows the list)
        JOptionPane.showMessageDialog(this, "Event saved successfully.", "Event Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    // Shows a red message at the top of the form.
    private void showError(String message) {
        errorLabel.setText(message);
    }

    // ---- Ticket tiers ----

    // The tier section: name/price/capacity inputs with an Add/Update button, the list, and Remove.
    private JPanel buildTierSection() {
        JPanel inputs = new JPanel(new GridLayout(2, 3, 10, 4));    // Row 1: labels, row 2: inputs
        inputs.setBackground(ViewStyles.SURFACE);
        inputs.setBorder(new EmptyBorder(12, 12, 12, 12));
        inputs.add(smallLabel("Tier name"));
        inputs.add(smallLabel("Price (KES)"));
        inputs.add(smallLabel("Capacity"));
        inputs.add(tierNameField);
        inputs.add(priceSpinner);
        inputs.add(capacitySpinner);
        ViewStyles.stylePrimaryButton(tierButton);
        tierButton.addActionListener(action -> addOrUpdateTier());
        JPanel editor = new JPanel(new BorderLayout(10, 0));
        editor.setOpaque(false);
        editor.add(inputs, BorderLayout.CENTER);
        editor.add(tierButton, BorderLayout.EAST);

        // How each tier looks in the list, e.g. "VIP  |  KES 3,750  |  12 / 18 available".
        tierList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tierList.setBackground(ViewStyles.SURFACE);
        tierList.setFixedCellHeight(34);
        tierList.setCellRenderer((list, tier, index, selected, focus) -> {
            JLabel row = new JLabel(String.format("%s  |  KES %,.0f  |  %d / %d available",
                    tier.getTierName(), tier.getPrice(), tier.getAvailableSeats(), tier.getInitialCapacity()));
            row.setOpaque(true);
            row.setBorder(new EmptyBorder(4, 8, 4, 8));
            row.setBackground(selected ? ViewStyles.ACCENT : ViewStyles.SURFACE);
            row.setForeground(ViewStyles.FOREGROUND);
            return row;
        });
        // Clicking a tier copies it into the inputs so it can be edited.
        tierList.addListSelectionListener(event -> loadSelectedTier());
        JScrollPane listScroll = new JScrollPane(tierList);
        listScroll.setPreferredSize(new Dimension(600, 110));
        JButton remove = new JButton("Remove Selected Tier");
        ViewStyles.styleSecondaryButton(remove);
        remove.addActionListener(action -> removeSelectedTier());

        return section("Ticket tiers", editor, listScroll, remove);
    }

    // Adds a new tier, or updates the selected one, after checking the rules.
    private void addOrUpdateTier() {
        String name = tierNameField.getText().trim();
        double price = ((Number) priceSpinner.getValue()).doubleValue();
        int capacity = ((Number) capacitySpinner.getValue()).intValue();
        int selected = tierList.getSelectedIndex();          // -1 = adding a new tier
        if (name.isEmpty()) {
            showError("Enter a tier name.");
            return;
        }
        // No other tier may already use this name.
        for (int i = 0; i < tierModel.size(); i++) {
            if (i != selected && tierModel.get(i).getTierName().equalsIgnoreCase(name)) {
                showError("Each ticket tier must have a unique name.");
                return;
            }
        }
        // When updating, keep the seats already sold and don't allow capacity below them.
        int sold = 0;
        if (selected >= 0) {
            TicketTier current = tierModel.get(selected);
            sold = current.getInitialCapacity() - current.getAvailableSeats();
            if (capacity < sold) {
                showError("Capacity cannot be lower than the " + sold + " tickets already sold.");
                return;
            }
        }
        TicketTier tier = new TicketTier(name, price, capacity, capacity - sold);
        if (selected >= 0) {
            tierModel.set(selected, tier);                   // Replace the selected tier
        } else {
            tierModel.addElement(tier);                      // Add a new one
        }
        clearTierInputs();
        errorLabel.setText(" ");
    }

    // Copies the selected tier into the inputs and relabels the button "Update Tier".
    private void loadSelectedTier() {
        TicketTier tier = tierList.getSelectedValue();       // null if nothing is selected
        if (tier == null) {
            return;
        }
        tierNameField.setText(tier.getTierName());
        priceSpinner.setValue(tier.getPrice());
        capacitySpinner.setValue(tier.getInitialCapacity());
        tierButton.setText("Update Tier");
    }

    // Removes the selected tier, unless guests have already bought tickets in it.
    private void removeSelectedTier() {
        TicketTier tier = tierList.getSelectedValue();
        if (tier == null) {
            return;
        }
        if (tier.getAvailableSeats() < tier.getInitialCapacity()) {   // Some seats have been sold
            showError("This tier has bookings and cannot be removed.");
            return;
        }
        tierModel.removeElement(tier);
        clearTierInputs();
    }

    // Resets the tier inputs and the button text.
    private void clearTierInputs() {
        tierList.clearSelection();
        tierNameField.setText("");
        priceSpinner.setValue(1000.0);
        capacitySpinner.setValue(50);
        tierButton.setText("Add Tier");
    }

    // ---- Custom details ----

    // The custom details section: name + value boxes with an Add button, the list, and Remove.
    private JPanel buildDetailSection() {
        JButton add = new JButton("Add detail");
        ViewStyles.styleSecondaryButton(add);
        add.addActionListener(action -> addDetail());
        JPanel inputs = new JPanel(new GridLayout(1, 2, 8, 0));
        inputs.setOpaque(false);
        inputs.add(detailNameField);
        inputs.add(detailValueField);
        JPanel editor = new JPanel(new BorderLayout(8, 0));
        editor.setOpaque(false);
        editor.add(inputs, BorderLayout.CENTER);
        editor.add(add, BorderLayout.EAST);

        detailList.setBackground(ViewStyles.SURFACE);
        detailList.setForeground(ViewStyles.FOREGROUND);
        JScrollPane listScroll = new JScrollPane(detailList);
        listScroll.setPreferredSize(new Dimension(600, 78));
        JButton remove = new JButton("Remove selected detail");
        ViewStyles.styleSecondaryButton(remove);
        remove.addActionListener(action -> detailModel.removeElement(detailList.getSelectedValue()));

        return section("Custom event details (e.g. Dress code: Smart casual)", editor, listScroll, remove);
    }

    // Adds the typed detail, or replaces an existing detail with the same name.
    private void addDetail() {
        String name = detailNameField.getText().trim();
        String value = detailValueField.getText().trim();
        if (name.isEmpty() || value.isEmpty()) {
            showError("Enter both a custom detail name and value.");
            return;
        }
        for (int i = 0; i < detailModel.size(); i++) {
            if (detailModel.get(i).name().equalsIgnoreCase(name)) {
                detailModel.remove(i);                       // Remove the old one with this name
                break;
            }
        }
        detailModel.addElement(new CustomField(name, value));
        detailNameField.setText("");
        detailValueField.setText("");
        errorLabel.setText(" ");
    }

    // ---- Layout helpers ----

    // The banner row: a path box plus a "Browse..." button that opens a picture file picker.
    private JPanel buildBannerField() {
        JButton browse = new JButton("Browse...");
        ViewStyles.styleSecondaryButton(browse);
        browse.addActionListener(action -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                bannerPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel field = new JPanel(new BorderLayout(8, 0));
        field.setOpaque(false);
        field.add(bannerPathField, BorderLayout.CENTER);
        field.add(browse, BorderLayout.EAST);
        return field;
    }

    // A titled section: heading, then the editor row, the list and the remove button stacked.
    private JPanel section(String title, JPanel editor, JScrollPane list, JButton remove) {
        JLabel heading = new JLabel(title);
        ViewStyles.styleHeading(heading, 16f);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(editor, BorderLayout.NORTH);
        body.add(list, BorderLayout.CENTER);
        body.add(remove, BorderLayout.SOUTH);
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        section.add(heading, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    // Adds one form row: label in column 0, input stretching across column 1.
    private void addField(JPanel form, String label, java.awt.Component input, int row) {
        GridBagConstraints labelRules = new GridBagConstraints();
        labelRules.gridx = 0;
        labelRules.gridy = row;
        labelRules.anchor = GridBagConstraints.WEST;
        labelRules.insets = new Insets(0, 0, 12, 14);
        JLabel fieldLabel = new JLabel(label);
        ViewStyles.styleSecondaryText(fieldLabel, 14f);
        form.add(fieldLabel, labelRules);
        GridBagConstraints inputRules = new GridBagConstraints();
        inputRules.gridx = 1;
        inputRules.gridy = row;
        inputRules.weightx = 1;
        inputRules.fill = GridBagConstraints.HORIZONTAL;
        inputRules.insets = new Insets(0, 0, 12, 0);
        form.add(input, inputRules);
    }

    // Layout rules for something that spans both columns of the form on the given row.
    private GridBagConstraints fullWidth(int row, int gapBelow) {
        GridBagConstraints rules = new GridBagConstraints();
        rules.gridx = 0;
        rules.gridy = row;
        rules.gridwidth = 2;
        rules.weightx = 1;
        rules.fill = GridBagConstraints.HORIZONTAL;
        rules.insets = new Insets(10, 0, gapBelow, 0);
        return rules;
    }

    // A small grey label.
    private JLabel smallLabel(String text) {
        JLabel label = new JLabel(text);
        ViewStyles.styleSecondaryText(label, 12f);
        return label;
    }

    // Gives every input the shared dark style, and sets the date pickers' format.
    private void styleInputs() {
        for (JTextField field : new JTextField[] {titleField, venueField, categoryField, bannerPathField,
                tierNameField, detailNameField, detailValueField}) {
            ViewStyles.styleInput(field);
        }
        for (JDateChooser chooser : new JDateChooser[] {dateChooser, salesStartChooser, salesEndChooser}) {
            chooser.setDateFormatString("yyyy-MM-dd");               // Show dates as 2026-10-15
            chooser.setPreferredSize(new Dimension(260, 36));
        }
    }

    // ---- Date conversions ----
    // JDateChooser uses the older java.util.Date type; the rest of the app uses LocalDate.
    // These two helpers convert between them (null stays null).

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // A record is a short way to write a small, unchangeable data class. Java writes the
    // constructor and the name() / value() getters for us. toString() is overridden so the
    // list shows "name: value".
    private record CustomField(String name, String value) {
        @Override
        public String toString() {
            return name + ": " + value;
        }
    }
}
