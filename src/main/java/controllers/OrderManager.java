package controllers;

import models.Booking;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller for guest purchases. It creates Booking model objects, keeps them
 * in memory, and calculates simple event sales totals for the host dashboard.
 */
public class OrderManager {
    // In-memory list acting as our database for the MVP
    private List<Booking> allBookings;

    public OrderManager() {
        this.allBookings = new ArrayList<>();
        // Note: For persistent storage, load from a bookings.json file here
    }

    /** 
     * Generates a short, user-friendly 8-character receipt UUID, creates the booking,
     * and saves it to the in-memory database.
     * 
     * @return The finalized Booking object, ready to be displayed on the TicketSummaryPanel.
     */
    public Booking createBooking(String eventId, String customerName, String customerEmail, String tierName,
            int quantity, double totalPrice) {
                
        // Generate an 8-character alphanumeric string (e.g., "A1B2C3D4")
        String uniqueBookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Booking newBooking = new Booking(uniqueBookingId, eventId, customerName, customerEmail, tierName,
                quantity, totalPrice);
        
        allBookings.add(newBooking);
        System.out.println("Success! Ticket " + uniqueBookingId + " generated for " + customerName);
        
        return newBooking;
    }

    /** Retrieves the entire transaction history (useful for master admin reporting). */
    public List<Booking> getAllBookings() {
        return allBookings;
    }

    /** 
     * Filters the master booking list to return only transactions for a specific event. 
     */
    public List<Booking> getBookingsByEvent(String eventId) {
        List<Booking> eventBookings = new ArrayList<>();
        for (Booking b : allBookings) {
            if (b.getEventId().equals(eventId)) {
                eventBookings.add(b);
            }
        }
        return eventBookings;
    }

    /** 
     * Calculates total gross revenue for an event by summing the prices of all related bookings. 
     */
    public double calculateEventRevenue(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToDouble(Booking::getTotalPrice).sum();
    }

    /** 
     * Calculates the total physical ticket quantity sold for an event. 
     */
    public int calculateTotalTicketsSold(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToInt(Booking::getQuantity).sum();
    }
}