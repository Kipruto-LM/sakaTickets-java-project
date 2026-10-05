package views;

import controllers.EventManager;
import controllers.OrderManager;
import controllers.AccountManager;
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import models.User;

public class MainFrame extends JFrame {
    private static final String WELCOME = "Welcome";
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";
    private static final String ADMIN_LOGIN = "AdminLogin";
    private static final String ADMIN_DASHBOARD = "AdminDashboard";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final EventManager eventManager;
    private final AccountManager accountManager;
    private final OrderManager orderManager;
    private final CheckoutPanel checkoutPanel;
    private final EventCatalogPanel catalogPanel;
    private AdminDashboardPanel adminDashboardPanel;

    public MainFrame() {
        eventManager = new EventManager();
        accountManager = new AccountManager();
        orderManager = new OrderManager();

        catalogPanel = new EventCatalogPanel(eventManager, this::showCheckout,
            () -> showCard(WELCOME));
        checkoutPanel = new CheckoutPanel(eventManager, orderManager, this::showCatalog);
        WelcomePanel welcomePanel = new WelcomePanel(this::showCatalog, () -> showCard(ADMIN_LOGIN));
        AdminLoginPanel adminLoginPanel = new AdminLoginPanel(accountManager, this::showAdminDashboard,
            () -> showCard(WELCOME));

        cardPanel.add(welcomePanel, WELCOME);
        cardPanel.add(catalogPanel, CATALOG);
        cardPanel.add(checkoutPanel, CHECKOUT);
        cardPanel.add(adminLoginPanel, ADMIN_LOGIN);

        setTitle("SakaTickets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(cardPanel);
        setMinimumSize(new java.awt.Dimension(900, 600));
        setSize(1200, 800);
        setLocationByPlatform(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        if (adminDashboardPanel != null) {
            cardPanel.remove(adminDashboardPanel);
        }
        adminDashboardPanel = new AdminDashboardPanel(eventManager, orderManager,
                () -> showCard(WELCOME), user.getUserId(), user.getName());
        cardPanel.add(adminDashboardPanel, ADMIN_DASHBOARD);
        adminDashboardPanel.refreshEvents();
        showCard(ADMIN_DASHBOARD);
    }

    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
    }
}