// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.EventManager;      // Holds all events
import controllers.OrderManager;      // Holds all bookings and sales numbers
import controllers.AccountManager;    // Holds host accounts and logins
import database.Database;             // Opens the PostgreSQL connection
import java.sql.Connection;           // An open link to the database
import java.awt.CardLayout;           // Layout that shows one "card" (screen) at a time
import javax.swing.JFrame;            // A desktop window
import javax.swing.JPanel;            // A container for other components
import models.Booking;                // One purchase
import models.User;                   // A host account

/**
 * The main application window. It holds every screen and switches between them.
 *
 * Screens and how you move between them:
 *   Welcome -> Catalogue -> Event details -> Checkout -> Ticket summary (receipt)
 *   Welcome -> Host login -> Host dashboard
 *
 * OOP ideas shown here:
 * - Inheritance ("is-a"): MainFrame extends JFrame, so it IS a window. It gets methods like
 *   setTitle() and setSize() for free from its parent class.
 * - Composition: the window HAS the three controllers and all the screen panels as fields.
 * - Dependency injection: the controllers are created once here and passed into each screen's
 *   constructor, so every screen shares the same data.
 * - Callbacks: screens get small actions (like this::showCatalog) to ask the window to change
 *   page. The screens never need to know about MainFrame itself (loose coupling).
 */
public class MainFrame extends JFrame {
    // Names of the pages ("cards") that the CardLayout can show.
    // Using constants avoids typos: a misspelt constant won't compile, a misspelt string would.
    private static final String WELCOME = "Welcome";
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";
    private static final String ADMIN_LOGIN = "AdminLogin";
    private static final String ADMIN_DASHBOARD = "AdminDashboard";
    private static final String TICKET_SUMMARY = "TicketSummary";
    private static final String EVENT_DETAILS = "EventDetails";

    // CardLayout stacks the screens like a deck of cards and shows one at a time.
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);   // The panel that holds the "deck"
    // The three controllers, shared by all screens.
    private final EventManager eventManager;
    private final AccountManager accountManager;
    private final OrderManager orderManager;
    // Screens we need to talk to again later (to pass them an event, a booking, etc.).
    private final CheckoutPanel checkoutPanel;
    private final TicketSummaryPanel ticketSummaryPanel;
    private final EventCatalogPanel catalogPanel;
    private final EventDetailsPanel detailsPanel;
    // Not final: a new dashboard is created each time a host logs in.
    private AdminDashboardPanel adminDashboardPanel;

    // Constructor: builds every screen and shows the welcome page.
    public MainFrame() {
        // The controllers (the C in MVC) are created once and shared by every screen.
        // Open the PostgreSQL connection once and share it with every controller (dependency injection).
        Connection connection = Database.connect();
        accountManager = new AccountManager(connection);
        eventManager = new EventManager();
        orderManager = new OrderManager();
        // Fill the sample events with realistic past sales.
        orderManager.seedSampleSales(eventManager.getEvents());

        // Each screen object gets the controllers it needs, plus callbacks for moving between pages.
        // "this::showCheckout" is a method reference: it passes our method in as an object.
        catalogPanel = new EventCatalogPanel(eventManager, this::showCheckout, this::showEventDetails,
            () -> showCard(WELCOME));
        checkoutPanel = new CheckoutPanel(eventManager, orderManager, this::showCatalog, this::showTicketSummary);
        ticketSummaryPanel = new TicketSummaryPanel(eventManager, this::showCatalog);
        // The lambda (event, tier) -> ... is a BiConsumer: an object holding a two-argument action.
        // When a guest picks a tier on the details page: open checkout with that tier chosen.
        detailsPanel = new EventDetailsPanel(orderManager, this::showCatalog, (event, tierName) -> {
            checkoutPanel.setEvent(event, tierName);
            showCard(CHECKOUT);
        });
        // These two are only needed here, so they are local variables rather than fields.
        WelcomePanel welcomePanel = new WelcomePanel(this::showCatalog, () -> showCard(ADMIN_LOGIN));
        AdminLoginPanel adminLoginPanel = new AdminLoginPanel(accountManager, this::showAdminDashboard,
            () -> showCard(WELCOME));

        // Put every screen into the deck, each with its name.
        cardPanel.add(welcomePanel, WELCOME);
        cardPanel.add(catalogPanel, CATALOG);
        cardPanel.add(checkoutPanel, CHECKOUT);
        cardPanel.add(detailsPanel, EVENT_DETAILS);
        cardPanel.add(ticketSummaryPanel, TICKET_SUMMARY);
        cardPanel.add(adminLoginPanel, ADMIN_LOGIN);

        // These methods are inherited from JFrame.
        setTitle("SakaTickets");                              // Text in the window's title bar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);       // Closing the window ends the program
        setContentPane(cardPanel);                            // The deck fills the window
        setMinimumSize(new java.awt.Dimension(900, 600));     // Can't be resized smaller than this
        setSize(1200, 800);                                   // Starting size
        setLocationRelativeTo(null);                          // null = centre on the screen
        cardLayout.show(cardPanel, WELCOME);                  // Start on the welcome page
    }

    // Opens checkout for an event (from the catalogue's "Book Now" button).
    private void showCheckout(String eventId) {
        checkoutPanel.setEvent(eventId);
        cardLayout.show(cardPanel, CHECKOUT);
    }

    // Opens the guest details page for an event.
    private void showEventDetails(String eventId) {
        detailsPanel.showEvent(eventManager.getEventById(eventId));
        showCard(EVENT_DETAILS);
    }

    // Goes back to the catalogue, refreshing it first so seat counts are up to date.
    private void showCatalog() {
        catalogPanel.refreshEvents();
        showCard(CATALOG);
    }

    // A new dashboard object is made for each login, so it shows the signed-in host's own events.
    private void showAdminDashboard(User user) {
        if (adminDashboardPanel != null) {
            cardPanel.remove(adminDashboardPanel);           // Throw away the previous host's dashboard
        }
        adminDashboardPanel = new AdminDashboardPanel(eventManager, orderManager,
                () -> showCard(WELCOME), user.getUserId(), user.getName());
        cardPanel.add(adminDashboardPanel, ADMIN_DASHBOARD);
        adminDashboardPanel.refreshEvents();
        showCard(ADMIN_DASHBOARD);
    }

    // Shows the receipt after a successful purchase.
    private void showTicketSummary(Booking booking) {
        ticketSummaryPanel.setBooking(booking);
        showCard(TICKET_SUMMARY);
    }

    // Switches to the named screen, then asks Swing to lay it out and redraw it.
    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
        cardPanel.revalidate();
        cardPanel.repaint();
    }
}
