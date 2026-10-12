// This file belongs to the "views" package: the screens (the V in MVC).
package views;

import controllers.EventManager;            // Where the events come from
import java.awt.BorderLayout;               // Layout with NORTH/SOUTH/EAST/WEST/CENTER areas
import java.awt.Color;                      // An RGB colour
import java.awt.Dimension;                  // A width and height
import java.awt.FlowLayout;                 // Layout that places items in a row
import java.awt.Font;                       // Font style constants
import java.awt.GridLayout;                 // Layout with equal-sized cells
import java.util.function.Consumer;         // An object holding an action that takes one value
import javax.swing.BorderFactory;           // Helper for making borders
import javax.swing.BoxLayout;               // Layout that stacks items in one column or row
import javax.swing.JButton;                 // A clickable button
import javax.swing.JLabel;                  // A piece of text
import javax.swing.JPanel;                  // A container
import javax.swing.JScrollPane;             // Adds scroll bars around a component
import javax.swing.ScrollPaneConstants;     // Settings for when scroll bars appear
import javax.swing.border.EmptyBorder;      // Invisible padding
import models.Event;                        // The event model

/**
 * The guest-facing list of upcoming events, shown as cards.
 *
 * Layout: a header ("Upcoming Events" + a back button), then a scrolling grid of event
 * cards, three per row. Each card shows a picture, title, category, date, venue, the lowest
 * available price, seats left, and "View details" / "Book Now" buttons.
 *
 * OOP ideas shown here:
 * - Inheritance: extends JPanel.
 * - MVC: this is a View. It reads events from the EventManager (Controller) and only displays them.
 * - Callbacks: when "Book Now" is clicked, it passes the event ID to checkoutAction
 *   (a Consumer<String>), and MainFrame decides what happens next. "View details" does the
 *   same with detailsAction.
 * - Nested class: RoundedCardPanel at the bottom is a small JPanel subclass used only here.
 */
public class EventCatalogPanel extends JPanel {
    private final EventManager eventManager;          // Source of the events to show
    private final Consumer<String> checkoutAction;    // Callback: open checkout for an event ID
    private final Consumer<String> detailsAction;     // Callback: open the details page for an event ID
    private final JPanel eventList = new JPanel();    // The grid that holds the cards

    // Constructor: builds the header and the scrolling grid, then fills it with cards.
    public EventCatalogPanel(EventManager eventManager, Consumer<String> checkoutAction,
            Consumer<String> detailsAction, Runnable backToHome) {
        this.eventManager = eventManager;
        this.checkoutAction = checkoutAction;
        this.detailsAction = detailsAction;
        setLayout(new BorderLayout(0, 16));            // 16px gap between header and grid
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(22, 30, 22, 30));    // Page padding

        // --- Header: headings on the left, "Back to Home" on the right ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel headings = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));   // 2 rows: heading, subheading
        headings.setOpaque(false);
        JLabel heading = new JLabel("Upcoming Events");
        ViewStyles.styleHeading(heading, 24f);
        JLabel subheading = new JLabel("Find something good happening around Nairobi.");
        ViewStyles.styleSecondaryText(subheading, 12f);
        headings.add(heading);
        headings.add(subheading);
        header.add(headings, BorderLayout.WEST);
        JButton backButton = new JButton("Back to Home");
        ViewStyles.styleSecondaryButton(backButton);
        backButton.addActionListener(action -> backToHome.run());   // Callback to MainFrame
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // --- The grid of cards ---
        // GridLayout(0, 3, ...): any number of rows (0), 3 columns, 15px gaps.
        eventList.setLayout(new GridLayout(0, 3, 15, 15));
        eventList.setBackground(ViewStyles.BACKGROUND);
        eventList.setBorder(new EmptyBorder(2, 1, 14, 1));
        refreshEvents();                               // Fill it with cards

        // Put the grid in a scroll pane: vertical scroll bar only when needed, never horizontal.
        JScrollPane scrollPane = new JScrollPane(eventList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ViewStyles.BACKGROUND);
        scrollPane.getViewport().setBackground(ViewStyles.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);    // Scroll 16px per mouse-wheel step
        add(scrollPane, BorderLayout.CENTER);
    }

    // Public method other classes can call to rebuild the cards after the data changes.
    public void refreshEvents() {
        eventList.removeAll();                                     // Clear the old cards
        for (Event event : eventManager.getPublicEvents()) {       // One card per public event
            JPanel card = createEventCard(event, checkoutAction);
            eventList.add(card);
        }
        // No events at all: show a friendly message instead.
        if (eventManager.getPublicEvents().isEmpty()) {
            JLabel empty = new JLabel("No public events right now. Please check back soon.", JLabel.CENTER);
            ViewStyles.styleSecondaryText(empty, 15f);
            eventList.add(empty);
        }
        eventList.revalidate();                                    // Re-do the layout
        eventList.repaint();                                       // Redraw
    }

    // Builds one event card.
    private JPanel createEventCard(Event event, Consumer<String> checkoutAction) {
        JPanel card = new RoundedCardPanel();                      // Our rounded-corner panel (see bottom)
        card.setLayout(new BorderLayout(0, 9));
        card.setBackground(new Color(0x17, 0x1B, 0x24));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2A, 0x30, 0x44)),   // Thin outline
            BorderFactory.createEmptyBorder(8, 8, 10, 8)));                // Padding inside
        card.putClientProperty("FlatLaf.style", "arc: 10");
        card.setPreferredSize(new Dimension(280, 285));
        card.add(EventVisuals.event(event, 132), BorderLayout.NORTH);   // Picture at the top

        // --- Left side of the card: the text details, stacked vertically ---
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 15f);
        title.setAlignmentX(LEFT_ALIGNMENT);

        // Category in small teal capitals, e.g. "MUSIC".
        JLabel category = new JLabel(event.getCategory().toUpperCase(java.util.Locale.ROOT));
        category.setForeground(new Color(0x00, 0xE5, 0xC4));
        category.setFont(ViewStyles.DATA_FONT.deriveFont(java.awt.Font.BOLD, 9f));
        category.setAlignmentX(LEFT_ALIGNMENT);
        details.add(title);
        details.add(category);
        JLabel date = new JLabel(event.getDate());
        ViewStyles.styleSecondaryText(date, 11f);
        date.setBorder(new EmptyBorder(5, 0, 1, 0));               // A little space above the date
        date.setAlignmentX(LEFT_ALIGNMENT);
        JLabel venue = new JLabel(event.getVenue());
        ViewStyles.styleSecondaryText(venue, 11f);
        venue.setAlignmentX(LEFT_ALIGNMENT);
        details.add(date);
        details.add(venue);
        // These two lines repeat the two above. Adding a component that is already in the
        // panel just keeps it where it is, so they have no visible effect.
        details.add(date);
        details.add(venue);
        // Show up to 2 of the host's custom details, e.g. "Dress code: Smart casual".
        int shown = 0;
        for (String name : event.getCustomDetails().keySet()) {
            if (shown == 2) {
                break;                                     // Stop after two
            }
            JLabel custom = new JLabel(name + ": " + event.getCustomDetails().get(name));
            ViewStyles.styleSecondaryText(custom, 12f);
            custom.setAlignmentX(LEFT_ALIGNMENT);
            details.add(custom);
            shown++;
        }

        // --- Right side of the card: price, seats and buttons, stacked vertically ---
        JPanel purchase = new JPanel();
        purchase.setOpaque(false);
        purchase.setLayout(new BoxLayout(purchase, BoxLayout.Y_AXIS));

        // The Event works out its own cheapest price (see Event.getLowestPrice()).
        double lowestPrice = event.getLowestPrice();
        // %,.0f = number with thousands commas and no decimals, e.g. "From KES 1,050".
        JLabel price = new JLabel(String.format("From KES %,.0f", lowestPrice));
        price.setForeground(ViewStyles.FOREGROUND);
        price.setFont(price.getFont().deriveFont(Font.BOLD, 16f));
        price.setAlignmentX(RIGHT_ALIGNMENT);

        JLabel seats = new JLabel(event.getAvailableSeats() + " tickets remaining");
        ViewStyles.styleSecondaryText(seats, 11f);
        seats.setAlignmentX(RIGHT_ALIGNMENT);
        seats.setBorder(new EmptyBorder(4, 0, 10, 0));

        // "Book Now" is blue when tickets can be bought; otherwise grey, disabled, and says "Sales Closed".
        JButton bookButton = new JButton("Book Now");
        boolean salesOpen = event.canPurchase() && event.getAvailableSeats() > 0;
        if (salesOpen) {
            ViewStyles.stylePrimaryButton(bookButton);
        } else {
            ViewStyles.styleSecondaryButton(bookButton);
            bookButton.setText("Sales Closed");
            bookButton.setEnabled(false);
        }
        bookButton.setAlignmentX(RIGHT_ALIGNMENT);
        bookButton.addActionListener(action -> checkoutAction.accept(event.getEventId()));   // Callback

        JButton detailsButton = new JButton("View details");
        ViewStyles.styleSecondaryButton(detailsButton);
        detailsButton.setFont(detailsButton.getFont().deriveFont(12f));
        detailsButton.setAlignmentX(RIGHT_ALIGNMENT);
        detailsButton.addActionListener(action -> detailsAction.accept(event.getEventId()));  // Callback


        purchase.add(price);
        purchase.add(seats);
        purchase.add(detailsButton);
        purchase.add(javax.swing.Box.createVerticalStrut(6));       // 6px gap between the buttons
        purchase.add(bookButton);
        // A FlowLayout wrapper keeps the purchase column at its natural size on the right.
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(purchase);
        // Put the details (left) and purchase column (right) together under the picture.
        JPanel info = new JPanel(new BorderLayout(0, 5));
        info.setOpaque(false);
        info.add(details, BorderLayout.CENTER);
        info.add(east, BorderLayout.EAST);
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    // Private static nested class: a JPanel with rounded corners. It overrides paintComponent()
    // to change how the parent class draws itself (inheritance + method overriding).
    private static class RoundedCardPanel extends JPanel {
        // Constructor: transparent, so only our rounded shape gets painted.
        private RoundedCardPanel() {
            setOpaque(false);
        }

        // Fills a rounded rectangle in the background colour, then lets JPanel draw the children.
        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D graphics2D = (java.awt.Graphics2D) graphics.create();
            graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);                  // Smooth corners
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);     // 30 = corner roundness
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }
}
