package controllers;

import models.Booking;
import models.Event;
import models.TicketTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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

    // Fills the sample events with past sales so the catalogue and dashboard look realistic.
    // Each sale goes through Event.purchase() (which takes the seats) and is recorded as a
    // Booking, the same way a real checkout works. A fixed Random seed gives the same
    // sales every time the app starts.
    public void seedSampleSales(List<Event> events) {
        String[] guests = {"Wanjiru Kamau", "Otieno Ochieng", "Achieng Atieno", "Kipchoge Rotich",
            "Njeri Mwangi", "Mutua Musyoka", "Akinyi Odhiambo", "Kiprop Kibet", "Wambui Njoroge",
            "Omondi Owino", "Chebet Langat", "Kamau Githinji", "Nyambura Wairimu", "Barasa Wekesa",
            "Moraa Nyaboke", "Mwende Mutiso", "Juma Hassan", "Amina Abdi", "Kariuki Maina", "Zawadi Mbeki"};
        // Share of each event's seats already sold, in the same order as the sample events.
        double[] sellThrough = {0.72, 0.58, 0.91, 0.44, 0.66, 0.37, 0.53};
        Random random = new Random(2026);
        for (int index = 0; index < events.size() && index < sellThrough.length; index++) {
            Event event = events.get(index);
            int target = (int) Math.round(event.getAvailableSeats() * sellThrough[index]);
            int sold = 0;
            while (sold < target && !event.getTicketTiers().isEmpty()) {
                TicketTier tier = event.getTicketTiers().get(0);
                int quantity = Math.min(1 + random.nextInt(4), target - sold);
                if (!event.purchase(tier.getTierName(), quantity)) {
                    break;
                }
                String guest = guests[random.nextInt(guests.length)];
                String email = guest.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com";
                String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                allBookings.add(new Booking(bookingId, event.getEventId(), guest, email, tier.getTierName(),
                        quantity, tier.getPrice() * quantity));
                sold += quantity;
            }
        }
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