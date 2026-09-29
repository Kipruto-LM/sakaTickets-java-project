package views;

import controllers.EventManager;
import controllers.OrderManager;
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrame extends JFrame {
    private static final String CATALOG = "catalog";
    private static final String CHECKOUT = "checkout";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final EventManager eventManager;

    public MainFrame() {
        eventManager = new EventManager();
        OrderManager orderManager = new OrderManager();

        EventCatalogPanel catalogPanel = new EventCatalogPanel(eventManager, this::showCheckout);
        CheckoutPanel checkoutPanel = new CheckoutPanel(eventManager, orderManager, this::showCatalog);
        cardPanel.add(catalogPanel, CATALOG);
        cardPanel.add(checkoutPanel, CHECKOUT);

        setTitle("SakaTickets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(cardPanel);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private void showCheckout(String eventId) {
        ((CheckoutPanel) cardPanel.getComponent(1)).setEvent(eventId);
        cardLayout.show(cardPanel, CHECKOUT);
    }

    private void showCatalog() {
        cardLayout.show(cardPanel, CATALOG);
    }
}