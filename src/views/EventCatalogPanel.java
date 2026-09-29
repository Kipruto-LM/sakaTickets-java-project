package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import models.Event;

public class EventCatalogPanel extends JPanel {
    public EventCatalogPanel(EventManager eventManager, Consumer<String> checkoutAction) {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(new JLabel("SakaTickets Event Catalog"), BorderLayout.NORTH);

        JPanel eventList = new JPanel(new GridLayout(0, 1, 12, 12));
        for (Event event : eventManager.getEvents()) {
            eventList.add(createEventRow(event, checkoutAction));
        }
        add(eventList, BorderLayout.CENTER);
    }

    private JPanel createEventRow(Event event, Consumer<String> checkoutAction) {
        JPanel row = new JPanel(new BorderLayout(12, 8));
        JPanel details = new JPanel(new GridLayout(0, 1));
        details.add(new JLabel(event.getTitle()));
        details.add(new JLabel(event.getDate() + " | " + event.getVenue()));
        details.add(new JLabel(event.getAvailableSeats() + " seats | KSh " + event.getTicketPrice()));

        JButton bookButton = new JButton("Book Now");
        bookButton.addActionListener(eventAction -> checkoutAction.accept(event.getEventId()));
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(bookButton);
        row.add(details, BorderLayout.CENTER);
        row.add(actionPanel, BorderLayout.EAST);
        return row;
    }
}