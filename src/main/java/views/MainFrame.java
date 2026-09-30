package views;

import controllers.EventManager;
import controllers.OrderManager;
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrame extends JFrame {
    private static final String WELCOME = "Welcome";
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";
    private static final String ADMIN_LOGIN = "AdminLogin";
    private static final String ADMIN_DASHBOARD = "AdminDashboard";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final EventManager eventManager;
    private final CheckoutPanel checkoutPanel;

    public MainFrame() {
        eventManager = new EventManager();
        OrderManager orderManager = new OrderManager();

        EventCatalogPanel catalogPanel = new EventCatalogPanel(eventManager, this::showCheckout,
            () -> showCard(WELCOME));
        checkoutPanel = new CheckoutPanel(eventManager, orderManager, this::showCatalog);
        WelcomePanel welcomePanel = new WelcomePanel(() -> showCard(CATALOG), () -> showCard(ADMIN_LOGIN));
        AdminLoginPanel adminLoginPanel = new AdminLoginPanel(() -> showCard(ADMIN_DASHBOARD),
            () -> showCard(WELCOME));
        AdminDashboardPanel adminDashboardPanel = new AdminDashboardPanel(() -> showCard(WELCOME));

        cardPanel.add(welcomePanel, WELCOME);
        cardPanel.add(catalogPanel, CATALOG);
        cardPanel.add(checkoutPanel, CHECKOUT);
        cardPanel.add(adminLoginPanel, ADMIN_LOGIN);
        cardPanel.add(adminDashboardPanel, ADMIN_DASHBOARD);

        setTitle("SakaTickets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(cardPanel);
        setSize(900, 600);
        setLocationRelativeTo(null);
        cardLayout.show(cardPanel, WELCOME);
    }

    private void showCheckout(String eventId) {
        checkoutPanel.setEvent(eventId);
        cardLayout.show(cardPanel, CHECKOUT);
    }

    private void showCatalog() {
        showCard(CATALOG);
    }

    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
    }
}