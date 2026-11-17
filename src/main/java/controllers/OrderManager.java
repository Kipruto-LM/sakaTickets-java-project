package controllers;

import database.Database;
import java.sql.SQLException;
import java.util.Optional;
import models.Booking;
import models.Event;
import models.TicketTier;
import java.util.UUID;

/**
 * Saves guest bookings and updates ticket inventory in SQLite.
 *
 * Core concepts:
 * - Abstraction: the checkout view calls one booking operation instead of issuing SQL.
 * - Composition: a Booking records the Event and TicketTier chosen by the guest.
 * - Optional: a rejected purchase is represented explicitly without a fake receipt.
 */
public class OrderManager {
    /**
     * Makes a receipt and asks the database to save it with the inventory update.
     * An empty result means the event or tier could not accept this purchase.
     *
     * @param event event being purchased
     * @param customerName guest name for the receipt
     * @param customerEmail guest email for the receipt
     * @param tierName selected ticket tier
     * @param quantity positive number of tickets requested
     * @param totalPrice calculated price for the requested tickets
     * @return a saved receipt, or empty when sales are closed or inventory is insufficient
     * @throws IllegalArgumentException if quantity is not positive
     * @throws SQLException if SQLite reports a database error
     */
    public Optional<Booking> createBooking(Event event, String customerName, String customerEmail,
            String tierName, int quantity, double totalPrice) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ticket quantity must be positive.");
        }
        String uniqueBookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = new Booking(uniqueBookingId, event.getEventId(), customerName, customerEmail, tierName,
                quantity, totalPrice);

        Database.PurchaseResult result = Database.saveBooking(booking, tierName, quantity);
        if (!result.purchased()) {
            return Optional.empty();
        }

        // Keep the screen's Event object aligned with the inventory saved in SQLite.
        for (TicketTier tier : event.getTicketTiers()) {
            if (tierName.equals(tier.getTierName())) {
                tier.setAvailableSeats(result.tierAvailableSeats());
                break;
            }
        }
        event.setStatus(result.eventStatus());
        return Optional.of(booking);
    }
}