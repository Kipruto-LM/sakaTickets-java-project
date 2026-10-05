package controllers;

import models.Booking;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Creates guest booking receipts and calculates event-level ticket/revenue analytics. */
public class OrderManager {
    // In-memory booking repository for this MVP.
    private List<Booking> allBookings;

    public OrderManager() {
        this.allBookings = new ArrayList<>();
        // In a complete app, you would load existing bookings from a JSON file here
    }

    // Creates a new booking, generates a UUID, and saves it to the list
    /** Legacy overload retained for callers that do not specify a ticket tier. */
    public Booking createBooking(String eventId, String customerName, String customerEmail, int quantity, double totalPrice) {
        return createBooking(eventId, customerName, customerEmail, "", quantity, totalPrice);
    }

    /** Generates an eight-character receipt ID, stores the booking, and returns it. */
    public Booking createBooking(String eventId, String customerName, String customerEmail, String tierName,
            int quantity, double totalPrice) {
        // Generate a short, readable 8-character alphanumeric ticket ID
        String uniqueBookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Booking newBooking = new Booking(uniqueBookingId, eventId, customerName, customerEmail, tierName,
                quantity, totalPrice);
        allBookings.add(newBooking);
        
        // Print to terminal for testing purposes
        System.out.println("Success! Ticket " + uniqueBookingId + " generated for " + customerName);
        
        // In a complete app, you would save the updated allBookings list to a JSON file here
        return newBooking;
    }

    // Retrieve all bookings (for the Admin dashboard)
    /** Returns the current in-memory booking collection for reporting screens. */
    public List<Booking> getAllBookings() {
        return allBookings;
    }

    // Retrieve bookings for a specific event to check sales
    /** Selects bookings for one event so attendee lists and analytics share one source. */
    public List<Booking> getBookingsByEvent(String eventId) {
        List<Booking> eventBookings = new ArrayList<>();
        for (Booking b : allBookings) {
            if (b.getEventId().equals(eventId)) {
                eventBookings.add(b);
            }
        }
        return eventBookings;
    }

    // Analytics are calculated from saved booking records so the dashboard stays current.
    public double calculateEventRevenue(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToDouble(Booking::getTotalPrice).sum();
    }

    public int calculateTotalTicketsSold(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToInt(Booking::getQuantity).sum();
    }
}