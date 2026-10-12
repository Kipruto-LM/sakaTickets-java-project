package controllers;

import models.Booking;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller that records ticket purchases ({@link Booking} objects) and works out sales figures.
 *
 * OOP ideas shown here:
 * - MVC: the checkout screen calls createBooking(), and the admin dashboard calls the
 *   calculate methods. Neither screen does this work itself.
 * - Method overloading: there are two createBooking() methods. The shorter one fills in an
 *   empty tier name and calls the longer one.
 * - Objects creating objects: createBooking() is the one place where new Booking objects are made.
 */
public class OrderManager {
    private List<Booking> allBookings;

    public OrderManager() {
        this.allBookings = new ArrayList<>();
        // In a complete app, you would load existing bookings from a JSON file here
    }

    // Creates a new booking, generates a UUID, and saves it to the list
    public Booking createBooking(String eventId, String customerName, String customerEmail, int quantity, double totalPrice) {
        return createBooking(eventId, customerName, customerEmail, "", quantity, totalPrice);
    }

    // Full version: builds the Booking object, stores it, and returns it to the caller.
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
    public List<Booking> getAllBookings() {
        return allBookings;
    }

    // Retrieve bookings for a specific event to check sales
    public List<Booking> getBookingsByEvent(String eventId) {
        List<Booking> eventBookings = new ArrayList<>();
        for (Booking b : allBookings) {
            if (b.getEventId().equals(eventId)) {
                eventBookings.add(b);
            }
        }
        return eventBookings;
    }

    // Total money taken for one event: adds up getTotalPrice() from each of its bookings.
    public double calculateEventRevenue(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToDouble(Booking::getTotalPrice).sum();
    }

    public int calculateTotalTicketsSold(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToInt(Booking::getQuantity).sum();
    }
}