package views;

import controllers.EventManager;
import controllers.OrderManager;
import controllers.AccountManager;
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import models.Booking;
import models.User;

/**
 * The main application window. It holds every screen and switches between them.
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
    private static final String WELCOME = "Welcome";
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";
    private static final String ADMIN_LOGIN = "AdminLogin";
    private static final String ADMIN_DASHBOARD = "AdminDashboard";
    private static final String TICKET_SUMMARY = "TicketSummary";

    // CardLayout stacks the screens like a deck of cards and shows one at a time.
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final EventManager eventManager;
    private final AccountManager accountManager;
    private final OrderManager orderManager;
    private final CheckoutPanel checkoutPanel;
    private final TicketSummaryPanel ticketSummaryPanel;
    private final EventCatalogPanel catalogPanel;
    private AdminDashboardPanel adminDashboardPanel;

    public MainFrame() {
        // The controllers (the C in MVC) are created once and shared by every screen.
        eventManager = new EventManager();
        accountManager = new AccountManager();
        orderManager = new OrderManager();

        // Each screen object gets the controllers it needs, plus callbacks for moving between pages.
        // "this::showCheckout" is a method reference: it passes our method in as an object.
        catalogPanel = new EventCatalogPanel(eventManager, this::showCheckout,
            () -> showCard(WELCOME));
        checkoutPanel = new CheckoutPanel(eventManager, orderManager, this::showCatalog, this::showTicketSummary);
        ticketSummaryPanel = new TicketSummaryPanel(eventManager, this::showCatalog);
        WelcomePanel welcomePanel = new WelcomePanel(this::showCatalog, () -> showCard(ADMIN_LOGIN));
        AdminLoginPanel adminLoginPanel = new AdminLoginPanel(accountManager, this::showAdminDashboard,
            () -> showCard(WELCOME));

        cardPanel.add(welcomePanel, WELCOME);
        cardPanel.add(catalogPanel, CATALOG);
        cardPanel.add(checkoutPanel, CHECKOUT);
        cardPanel.add(ticketSummaryPanel, TICKET_SUMMARY);
        cardPanel.add(adminLoginPanel, ADMIN_LOGIN);

        // These methods are inherited from JFrame.
        setTitle("SakaTickets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(cardPanel);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setSize(1200, 800);
        setLocationRelativeTo(null);
        cardLayout.show(cardPanel, WELCOME);
    }

    private void showCheckout(String eventId) {
        checkoutPanel.setEvent(eventId);
        cardLayout.show(cardPanel, CHECKOUT);
    }

    private void showCatalog() {
        catalogPanel.refreshEvents();
        showCard(CATALOG);
    }

    // A new dashboard object is made for each login, so it shows the signed-in host's own events.
    private void showAdminDashboard(User user) {
        if (adminDashboardPanel != null) {
            cardPanel.remove(adminDashboardPanel);
        }
        adminDashboardPanel = new AdminDashboardPanel(eventManager, orderManager,
                () -> showCard(WELCOME), user.getUserId(), user.getName());
        cardPanel.add(adminDashboardPanel, ADMIN_DASHBOARD);
        adminDashboardPanel.refreshEvents();
        showCard(ADMIN_DASHBOARD);
    }

    private void showTicketSummary(Booking booking) {
        ticketSummaryPanel.setBooking(booking);
        showCard(TICKET_SUMMARY);
    }

    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
        cardPanel.revalidate();
        cardPanel.repaint();
    }
}