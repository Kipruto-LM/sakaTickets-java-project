package views;

import controllers.EventManager;
import controllers.OrderManager;
import controllers.AccountManager;
import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;
import models.Booking;
import models.User;

/** Top-level window and CardLayout router for guest, host, checkout, and receipt screens. */
public class MainFrame extends JFrame {
    // Card identifiers keep navigation independent from panel implementation classes.
    private static final String WELCOME = "Welcome";
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";
    private static final String ADMIN_LOGIN = "AdminLogin";
    private static final String ADMIN_DASHBOARD = "AdminDashboard";
    private static final String TICKET_SUMMARY = "TicketSummary";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final EventManager eventManager;
    private final AccountManager accountManager;
    private final OrderManager orderManager;
    private CheckoutPanel checkoutPanel;
    private TicketSummaryPanel ticketSummaryPanel;
    private EventCatalogPanel catalogPanel;
    private User activeUser;
    private AdminDashboardPanel adminDashboardPanel;

    /** Builds shared services and opens the persisted welcome screen at laptop size. */
    public MainFrame() {
        eventManager = new EventManager();
        accountManager = new AccountManager();
        orderManager = new OrderManager();

        setTitle("SakaTickets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(cardPanel);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setSize(1200, 800);
        setLocationRelativeTo(null);
        buildScreens();
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

    private void showAdminDashboard(User user) {
        activeUser = user;
        createAdminDashboard();
        showCard(ADMIN_DASHBOARD);
    }

    /** Creates the current host's dashboard and supplies appearance-change callbacks. */
    private void createAdminDashboard() {
        if (adminDashboardPanel != null) cardPanel.remove(adminDashboardPanel);
        adminDashboardPanel = new AdminDashboardPanel(eventManager, orderManager,
                this::logout, activeUser.getUserId(), activeUser.getName(), this::changeDarkMode,
                this::changeAccentColor);
        cardPanel.add(adminDashboardPanel, ADMIN_DASHBOARD);
        adminDashboardPanel.refreshEvents();
        ThemeManager.applyPalette(adminDashboardPanel);
    }

    /** Rebuilds cards after appearance changes while retaining shared domain services. */
    private void buildScreens() {
        cardPanel.removeAll();
        catalogPanel = new EventCatalogPanel(eventManager, this::showCheckout, this::showHome);
        checkoutPanel = new CheckoutPanel(eventManager, orderManager, this::showCatalog, this::showTicketSummary);
        ticketSummaryPanel = new TicketSummaryPanel(eventManager, this::showCatalog);
        WelcomePanel welcomePanel = new WelcomePanel(this::showCatalog, () -> showCard(ADMIN_LOGIN));
        AdminLoginPanel adminLoginPanel = new AdminLoginPanel(accountManager, this::showAdminDashboard, this::showHome);
        cardPanel.add(welcomePanel, WELCOME);
        cardPanel.add(catalogPanel, CATALOG);
        cardPanel.add(checkoutPanel, CHECKOUT);
        cardPanel.add(ticketSummaryPanel, TICKET_SUMMARY);
        cardPanel.add(adminLoginPanel, ADMIN_LOGIN);
        adminDashboardPanel = null;
        if (activeUser != null) createAdminDashboard();
        ThemeManager.applyPalette(cardPanel);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /** Applies a saved mode and rebuilds all panels so custom colors follow the theme. */
    private void changeDarkMode(boolean dark) {
        ThemeManager.setDarkMode(dark);
        buildScreens();
        showCard(activeUser == null ? WELCOME : ADMIN_DASHBOARD);
    }

    /** Persists an accent choice and rebuilds buttons/navigation using the new palette. */
    private void changeAccentColor(Color color) {
        ThemeManager.setAccentColor(color);
        buildScreens();
        showCard(activeUser == null ? WELCOME : ADMIN_DASHBOARD);
    }

    private void logout() {
        activeUser = null;
        showHome();
    }

    private void showHome() {
        showCard(WELCOME);
    }

    private void showTicketSummary(Booking booking) {
        ticketSummaryPanel.setBooking(booking);
        showCard(TICKET_SUMMARY);
    }

    /** Switches the visible screen and forces layout/paint after each navigation change. */
    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
        cardPanel.revalidate();
        cardPanel.repaint();
    }
}