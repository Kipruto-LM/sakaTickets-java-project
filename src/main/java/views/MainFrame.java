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

/**
 * Top-level application window and screen router. It creates one shared set of
 * controllers, then passes them into view panels (dependency injection). The
 * CardLayout switches between screens without creating a separate window each time.
 */
public class MainFrame extends JFrame {
    // Card identifiers keep navigation independent from panel implementation classes.
    private static final String WELCOME = "Welcome";
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";
    private static final String ADMIN_LOGIN = "AdminLogin";
    private static final String ADMIN_DASHBOARD = "AdminDashboard";
    private static final String TICKET_SUMMARY = "TicketSummary";

    // Core layout and layout manager
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    
    // Shared controllers are created once and passed to the panels that need them.
    private final EventManager eventManager;
    private final AccountManager accountManager;
    private final OrderManager orderManager;
    
    // View panels
    private CheckoutPanel checkoutPanel;
    private TicketSummaryPanel ticketSummaryPanel;
    private EventCatalogPanel catalogPanel;
    private AdminDashboardPanel adminDashboardPanel;
    
    // Tracks the currently authenticated host
    private User activeUser;

    /** 
     * Initializes shared services, configures window dimensions, and opens the welcome screen. 
     */
    public MainFrame() {
        eventManager = new EventManager();
        accountManager = new AccountManager();
        orderManager = new OrderManager();

        setTitle("SakaTickets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(cardPanel);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setSize(1200, 800);
        setLocationRelativeTo(null); // Centers window on screen
        buildScreens();
        
        // Start application on the Welcome screen
        cardLayout.show(cardPanel, WELCOME);
    }

    /** Navigates to the guest checkout form for a specific event. */
    private void showCheckout(String eventId) {
        checkoutPanel.setEvent(eventId);
        cardLayout.show(cardPanel, CHECKOUT);
    }

    /** Forces the catalog to fetch fresh data before displaying it. */
    private void showCatalog() {
        catalogPanel.refreshEvents();
        showCard(CATALOG);
    }

    /** Authenticates a user and constructs their personalized admin dashboard. */
    private void showAdminDashboard(User user) {
        activeUser = user;
        createAdminDashboard();
        showCard(ADMIN_DASHBOARD);
    }

    /** 
     * Re-creates the host's dashboard entirely to ensure it reflects current 
     * theme/accent preferences and passes in the logout callback.
     */
    private void createAdminDashboard() {
        if (adminDashboardPanel != null) cardPanel.remove(adminDashboardPanel);
        adminDashboardPanel = new AdminDashboardPanel(eventManager, orderManager,
                this::logout, activeUser.getUserId(), activeUser.getName(), this::changeDarkMode,
                this::changeAccentColor);
        cardPanel.add(adminDashboardPanel, ADMIN_DASHBOARD);
        adminDashboardPanel.refreshEvents();
        ThemeManager.applyPalette(adminDashboardPanel);
    }

    /**
     * Creates the screens and gives each one callbacks for navigation.
     * CardLayout keeps all screens in one window and displays one card at a time.
     */
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
        
        // Force the FlatLaf color palette to cascade through all loaded panels
        ThemeManager.applyPalette(cardPanel);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /** Applies a saved dark/light mode preference and completely rebuilds all panels. */
    private void changeDarkMode(boolean dark) {
        ThemeManager.setDarkMode(dark);
        buildScreens();
        showCard(activeUser == null ? WELCOME : ADMIN_DASHBOARD);
    }

    /** Persists a custom accent color choice and rebuilds buttons/navigation. */
    private void changeAccentColor(Color color) {
        ThemeManager.setAccentColor(color);
        buildScreens();
        showCard(activeUser == null ? WELCOME : ADMIN_DASHBOARD);
    }

    /** Clears the active session and returns to the landing page. */
    private void logout() {
        activeUser = null;
        showHome();
    }

    private void showHome() {
        showCard(WELCOME);
    }

    /** Navigates to the digital receipt screen after a successful purchase. */
    private void showTicketSummary(Booking booking) {
        ticketSummaryPanel.setBooking(booking);
        showCard(TICKET_SUMMARY);
    }

    /** 
     * Centralized navigation method that switches the visible card 
     * and forces a UI repaint to prevent visual artifacting.
     */
    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
        cardPanel.revalidate();
        cardPanel.repaint();
    }
}