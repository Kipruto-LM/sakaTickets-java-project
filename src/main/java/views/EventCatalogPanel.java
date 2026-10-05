package views;

import controllers.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import models.Event;
import models.TicketTier;

/** 
 * Public, guest-facing event browser. 
 * Fetches the active catalog from the EventManager and displays responsive event cards 
 * in a scrollable grid layout. 
 */
public class EventCatalogPanel extends JPanel {
    private final EventManager eventManager;
    private final Consumer<String> checkoutAction;
    private final JPanel eventList = new JPanel();

    /** 
     * Constructs the catalog, sets up the scrolling viewport, and builds the initial event grid.
     * 
     * @param eventManager   The global catalog controller.
     * @param checkoutAction Callback to trigger the Checkout panel, passing the selected event ID.
     * @param backToHome     Callback to return to the Welcome screen.
     */
    public EventCatalogPanel(EventManager eventManager, Consumer<String> checkoutAction, Runnable backToHome) {
        this.eventManager = eventManager;
        this.checkoutAction = checkoutAction;
        setLayout(new BorderLayout(0, 16));
        setBackground(ViewStyles.BACKGROUND);
        setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JPanel headings = new JPanel(new GridLayout(2, 1, 0, 4));
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
        backButton.addActionListener(action -> backToHome.run());
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // A flexible grid layout that automatically wraps items based on the available width
        eventList.setLayout(new GridLayout(0, 3, 15, 15));
        eventList.setBackground(ViewStyles.BACKGROUND);
        eventList.setBorder(new EmptyBorder(2, 1, 14, 1));
        refreshEvents();

        JScrollPane scrollPane = new JScrollPane(eventList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ViewStyles.BACKGROUND);
        scrollPane.getViewport().setBackground(ViewStyles.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        // Listen for window resizes to recalculate the aspect ratio of the event cover images
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                resizeEventArtwork();
            }
        });
    }

    /** 
     * Clears the current grid and completely rebuilds it by fetching 
     * the latest unarchived events from the EventManager.
     */
    public void refreshEvents() {
        eventList.removeAll();
        for (Event event : eventManager.getPublicEvents()) {
            eventList.add(createEventCard(event, checkoutAction));
        }
        
        if (eventManager.getPublicEvents().isEmpty()) {
            JLabel empty = new JLabel("No public events right now. Please check back soon.", JLabel.CENTER);
            ViewStyles.styleSecondaryText(empty, 15f);
            eventList.add(empty);
        }
        
        eventList.revalidate();
        eventList.repaint();
        resizeEventArtwork();
    }

    /** 
     * Dynamically adjusts the height of the image masks inside each card 
     * to maintain a consistent aspect ratio when the grid columns stretch.
     */
    private void resizeEventArtwork() {
        if (eventList.getWidth() <= 0) return;
        int cardWidth = Math.max(220, eventList.getWidth() / 3 - 12);
        int imageHeight = Math.max(132, Math.min(390, (int) (cardWidth * 0.54)));
        for (java.awt.Component component : eventList.getComponents()) {
            if (component instanceof javax.swing.JComponent card
                    && card.getClientProperty("eventArtwork") instanceof JPanel artwork) {
                artwork.setPreferredSize(new Dimension(480, imageHeight));
            }
        }
        eventList.revalidate();
    }

    /** 
     * Constructs a single event card displaying artwork, schedule details, and pricing.
     */
    private JPanel createEventCard(Event event, Consumer<String> checkoutAction) {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 9));
        card.setBackground(ViewStyles.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ViewStyles.OUTLINE),
                BorderFactory.createEmptyBorder(8, 8, 10, 8)));
        card.putClientProperty("FlatLaf.style", "arc: 10");
        
        JPanel artwork = EventVisuals.event(event, 132, 8);
        card.putClientProperty("eventArtwork", artwork); // Tag the artwork for resizing logic
        card.add(artwork, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel(event.getTitle());
        ViewStyles.styleHeading(title, 15f);
        title.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel category = new JLabel(event.getCategory().toUpperCase(java.util.Locale.ROOT));
        category.setForeground(new Color(0x00, 0xE5, 0xC4));
        category.setFont(ViewStyles.DATA_FONT.deriveFont(Font.BOLD, 9f));
        category.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel date = new JLabel(event.getDate());
        ViewStyles.styleSecondaryText(date, 11f);
        date.setBorder(new EmptyBorder(5, 0, 1, 0));
        date.setAlignmentX(LEFT_ALIGNMENT);
        
        JLabel venue = new JLabel(event.getVenue());
        ViewStyles.styleSecondaryText(venue, 11f);
        venue.setAlignmentX(LEFT_ALIGNMENT);
        
        details.add(title);
        details.add(category);
        details.add(date);
        details.add(venue);

        JPanel purchase = new JPanel();
        purchase.setOpaque(false);
        purchase.setLayout(new BoxLayout(purchase, BoxLayout.Y_AXIS));
        
        // Find the cheapest available ticket tier to show as the "From KES X" price
        double lowestPrice = event.getTicketTiers().stream().mapToDouble(TicketTier::getPrice)
                .min().orElse(0.0);
                
        JLabel price = new JLabel(String.format("From KES %,.0f", lowestPrice));
        price.setForeground(ViewStyles.FOREGROUND);
        price.setFont(price.getFont().deriveFont(Font.BOLD, 16f));
        price.setAlignmentX(RIGHT_ALIGNMENT);
        
        JLabel seats = new JLabel(event.getAvailableSeats() + " tickets remaining");
        ViewStyles.styleSecondaryText(seats, 11f);
        seats.setAlignmentX(RIGHT_ALIGNMENT);
        seats.setBorder(new EmptyBorder(4, 0, 10, 0));
        
        JButton bookButton = new JButton("Book Now");
        boolean salesOpen = event.canPurchaseOn(java.time.LocalDate.now()) && event.getAvailableSeats() > 0;
        if (salesOpen) {
            ViewStyles.stylePrimaryButton(bookButton);
        } else {
            ViewStyles.styleSecondaryButton(bookButton);
            bookButton.setText("Sales Closed");
            bookButton.setEnabled(false);
        }
        
        bookButton.setAlignmentX(RIGHT_ALIGNMENT);
        bookButton.addActionListener(action -> checkoutAction.accept(event.getEventId()));
        
        purchase.add(price);
        purchase.add(seats);
        purchase.add(bookButton);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(purchase);
        
        JPanel info = new JPanel(new BorderLayout(0, 5));
        info.setOpaque(false);
        info.add(details, BorderLayout.CENTER);
        info.add(east, BorderLayout.EAST);
        card.add(info, BorderLayout.CENTER);
        
        return card;
    }

    /** 
     * A custom panel that overrides Swing's default paint cycle to ensure 
     * the physical background of the card has cleanly rounded corners 
     * before any child components are drawn on top of it.
     */
    private static class RoundedCardPanel extends JPanel {
        private RoundedCardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D graphics2D = (java.awt.Graphics2D) graphics.create();
            graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }
}