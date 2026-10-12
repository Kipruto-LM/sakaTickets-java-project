// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.AccountManager;          // Host accounts (for the Settings page)
import controllers.EventManager;            // Holds all events
import controllers.OrderManager;            // Holds all bookings and sales numbers
import java.awt.BorderLayout;               // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.CardLayout;                 // Shows one page at a time
import java.awt.Color;                      // An RGB colour
import java.awt.Dimension;                  // A width and height
import java.awt.GridLayout;                 // Layout with equal-sized cells
import java.util.LinkedHashMap;             // A map that keeps insertion order
import java.util.Map;                       // The general map type
import javax.swing.BorderFactory;           // Helper for making borders
import javax.swing.Box;                     // Invisible spacers
import javax.swing.BoxLayout;               // Stacks items in one column
import javax.swing.JButton;                 // A clickable button
import javax.swing.JLabel;                  // A piece of text
import javax.swing.JPanel;                  // A container
import javax.swing.SwingConstants;          // Alignment constants such as LEFT
import javax.swing.border.EmptyBorder;      // Invisible padding
import models.Event;                        // The event model
import models.User;                         // The logged-in host

/**
 * The host's dashboard. It is now just the "frame" around the pages: a sidebar on the left
 * (logo, menu, account) and a top bar, with one page showing at a time:
 *
 *   "Overview"      - OverviewPage        (totals and the revenue chart)
 *   "Manage Events" - EventListPage       (filters and one row per event)
 *   "Insights"      - EventAnalyticsPanel (charts for one event)
 *   "Editor"        - EventEditorPage     (create or edit an event)
 *   "Settings"      - SettingsPage        (account details, deleting accounts)
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel.
 * - Composition: the dashboard HAS four page objects and tells them what to do. Each page is
 *   its own class with one job, instead of one giant class doing everything.
 * - Callbacks: the pages get small actions (like this::showInsights) so they can ask the dashboard
 *   to switch pages without knowing about each other (loose coupling).
 * - Encapsulation: only refreshEvents() is public; everything else is private.
 */
public class AdminDashboardPanel extends JPanel {
    private final EventManager eventManager;
    private final String hostId;                                       // Which host is logged in
    // Page switching.
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();   // Sidebar buttons by page name
    // Top bar.
    private final JLabel pageTitle = new JLabel(" ");
    private final JLabel pageDescription = new JLabel(" ");
    private final JButton createEventButton = new JButton("+  Create New Event");
    private String activePage = "Overview";                            // Name of the page showing now
    // The page objects (composition).
    private final OverviewPage overviewPage;
    private final EventListPage eventListPage;
    private final EventEditorPage editorPage;
    private final EventAnalyticsPanel insightsPage;
    private final SettingsPage settingsPage;

    // Constructor: creates the pages, wiring each one's buttons to dashboard methods, then
    // builds the sidebar and top bar around them.
    // host = the User who logged in. Their ID decides which events are shown.
    public AdminDashboardPanel(EventManager eventManager, OrderManager orderManager, AccountManager accountManager,
            Runnable logoutAction, User host) {
        this.eventManager = eventManager;
        this.hostId = host.getUserId();
        String hostName = host.getName();

        // "this::showInsights" is a method reference: it hands one of our methods to the page as an object.
        overviewPage = new OverviewPage(hostName, orderManager, () -> showPage("Manage Events"));
        eventListPage = new EventListPage(eventManager, orderManager, hostId,
                this::editEvent, this::showInsights, this::newEvent, this::refreshEvents);
        editorPage = new EventEditorPage(eventManager, hostId, () -> {   // After saving: refresh and show the list
            refreshEvents();
            showPage("Manage Events");
        });
        insightsPage = new EventAnalyticsPanel(orderManager, () -> showPage("Manage Events"),
                this::editEvent, eventListPage::exportAttendees);
        settingsPage = new SettingsPage(accountManager, eventManager, orderManager, host,
                logoutAction, this::refreshEvents);

        // Put the pages into the CardLayout, each with a name.
        pages.setOpaque(false);
        pages.add(overviewPage, "Overview");
        pages.add(eventListPage, "Manage Events");
        pages.add(insightsPage, "Insights");
        pages.add(editorPage, "Editor");
        pages.add(settingsPage, "Settings");

        // Workspace = top bar + the pages, to the right of the sidebar.
        JPanel workspace = new JPanel(new BorderLayout(0, 18));
        workspace.setBackground(ViewStyles.PAGE);
        workspace.setBorder(new EmptyBorder(24, 28, 24, 28));
        workspace.add(buildTopBar(), BorderLayout.NORTH);
        workspace.add(pages, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBackground(ViewStyles.PAGE);
        add(buildSidebar(hostName, logoutAction), BorderLayout.WEST);
        add(workspace, BorderLayout.CENTER);
        refreshEvents();
        showPage("Overview");
    }

    // Updates the Overview numbers and the event list. Called after any change to events or sales.
    public void refreshEvents() {
        overviewPage.refresh(eventManager.getEventsByHostId(hostId));
        eventListPage.refresh();
    }

    // ---- Moving between pages ----

    // Opens the insights page for one event.
    private void showInsights(Event event) {
        insightsPage.showEvent(event);
        showPage("Insights");
    }

    // Opens the editor with an existing event's details.
    private void editEvent(Event event) {
        editorPage.loadEvent(event);
        showPage("Editor");
    }

    // Opens an empty editor for a new event.
    private void newEvent() {
        editorPage.startNewEvent();
        showPage("Editor");
    }

    // Shows a page by name, and updates the top bar and the sidebar highlight to match.
    private void showPage(String page) {
        activePage = page;
        // Title and description for each page.
        if (page.equals("Overview")) {
            setTitles("Overview", "Your ticket sales at a glance");
        } else if (page.equals("Insights")) {
            setTitles("Event Insights", "Sales, tiers and bookings for one event");
        } else if (page.equals("Editor")) {
            setTitles(editorPage.isEditingExistingEvent() ? "Edit Event" : "Create New Event",
                    "Configure event details and ticket tiers");
        } else if (page.equals("Settings")) {
            setTitles("Settings", "Your account and host accounts");
            settingsPage.refresh();                     // Show the latest list of hosts
        } else {
            setTitles("Active Events Inventory", "Manage and monitor all your events");
        }
        // In the editor, the top-right button becomes "Back to events".
        createEventButton.setText(page.equals("Editor") ? "Back to events" : "+  Create New Event");
        // Highlight the matching sidebar button. Editor and Insights count as part of "Manage Events".
        String menuItem = (page.equals("Editor") || page.equals("Insights")) ? "Manage Events" : page;
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(menuItem);
            JButton button = entry.getValue();
            button.setBackground(selected ? new Color(0x00, 0xB4, 0xFF, 28) : ViewStyles.CARD);
            button.setForeground(selected ? new Color(0x00, 0xB4, 0xFF) : ViewStyles.LABEL);
        }
        pageLayout.show(pages, page);
    }

    // Sets the two lines of text in the top bar.
    private void setTitles(String title, String description) {
        pageTitle.setText(title);
        pageDescription.setText(description);
    }

    // ---- Building the frame ----

    // The left sidebar: logo at the top, one button per page, then the account box and Log out.
    private JPanel buildSidebar(String hostName, Runnable logoutAction) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(224, 0));                     // 224px wide
        sidebar.setBackground(ViewStyles.CARD);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ViewStyles.CARD_BORDER));   // Line on the right

        JLabel logo = new JLabel("SakaTickets");
        ViewStyles.styleHeading(logo, 16f);
        logo.setBorder(new EmptyBorder(24, 18, 24, 14));
        sidebar.add(logo, BorderLayout.NORTH);

        // The menu: one button per page, stacked.
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(0, 8, 10, 8));
        for (String item : new String[] {"Overview", "Manage Events", "Settings"}) {
            JButton button = new JButton(item);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBorder(new EmptyBorder(11, 13, 11, 8));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.addActionListener(action -> showPage(item));              // Clicking shows that page
            navButtons.put(item, button);                                    // Remember it for highlighting
            menu.add(button);
            menu.add(Box.createVerticalStrut(5));
        }
        sidebar.add(menu, BorderLayout.CENTER);

        // Account box: name, role and a Log out button.
        JLabel name = new JLabel(hostName);
        ViewStyles.styleHeading(name, 12f);
        JLabel role = new JLabel("Event host");
        ViewStyles.styleSecondaryText(role, 10f);
        JButton logout = ViewStyles.smallButton("Log out");
        logout.addActionListener(action -> logoutAction.run());               // Callback to MainFrame
        JPanel account = new JPanel(new GridLayout(3, 1, 0, 4));
        account.setOpaque(false);
        account.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ViewStyles.CARD_BORDER),
                new EmptyBorder(15, 15, 18, 12)));
        account.add(name);
        account.add(role);
        account.add(logout);
        sidebar.add(account, BorderLayout.SOUTH);
        return sidebar;
    }

    // The top bar: page title and description on the left, the main button on the right.
    private JPanel buildTopBar() {
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 3));
        labels.setOpaque(false);
        ViewStyles.styleHeading(pageTitle, 20f);
        ViewStyles.styleSecondaryText(pageDescription, 12f);
        labels.add(pageTitle);
        labels.add(pageDescription);

        // One button, two jobs: in the editor it goes back to the list; elsewhere it starts a new event.
        ViewStyles.stylePrimaryButton(createEventButton);
        createEventButton.addActionListener(action -> {
            if (activePage.equals("Editor")) {
                showPage("Manage Events");
            } else {
                newEvent();
            }
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(labels, BorderLayout.WEST);
        top.add(createEventButton, BorderLayout.EAST);
        return top;
    }
}
