// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import models.Booking;               // One purchase (the receipt)
import models.Event;                 // The event model
import models.TicketTier;            // A ticket price level
import java.time.LocalDate;          // A date without a time
import java.time.LocalDateTime;      // A date with a time
import java.util.ArrayList;          // A resizable list
import java.util.Comparator;         // Describes how to sort things
import java.util.LinkedHashMap;      // A map that keeps insertion order
import java.util.List;               // The general list type
import java.util.Locale;             // Language rules for lower-casing
import java.util.Map;                // The general map type
import java.util.Random;             // Random numbers (for the sample sales)
import java.util.UUID;               // Random unique IDs (for receipt codes)

/**
 * Controller that records ticket purchases ({@link Booking} objects) and works out sales figures.
 *
 * OOP ideas shown here:
 * - MVC: the checkout screen calls createBooking(), and the admin dashboard calls the
 *   calculate methods. Neither screen does this work itself.
 * - Objects creating objects: createBooking() is the one place where new Booking objects are made.
 * - Abstraction: the analytics methods at the bottom turn the raw list of bookings into
 *   ready-to-chart numbers, so the screens don't need to know how they are worked out.
 */
public class OrderManager {
    private List<Booking> allBookings;   // Every booking ever made, for all events

    // Constructor: starts with an empty list of bookings.
    public OrderManager() {
        this.allBookings = new ArrayList<>();
        // In a complete app, you would load existing bookings from a JSON file here
    }

    // Creates a new booking: builds the Booking object with a short random receipt code,
    // stores it, and returns it to the caller.
    public Booking createBooking(String eventId, String customerName, String customerEmail, String tierName,
            int quantity, double totalPrice) {
        // Generate a short, readable 8-character alphanumeric ticket ID
        // (first 8 characters of a random UUID, in capitals).
        String uniqueBookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Make the Booking object and add it to the list.
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
        // Made-up guest names used for the sample bookings.
        String[] guests = {"Wanjiru Kamau", "Otieno Ochieng", "Achieng Atieno", "Kipchoge Rotich",
            "Njeri Mwangi", "Mutua Musyoka", "Akinyi Odhiambo", "Kiprop Kibet", "Wambui Njoroge",
            "Omondi Owino", "Chebet Langat", "Kamau Githinji", "Nyambura Wairimu", "Barasa Wekesa",
            "Moraa Nyaboke", "Mwende Mutiso", "Juma Hassan", "Amina Abdi", "Kariuki Maina", "Zawadi Mbeki"};
        // Share of each event's seats already sold, in the same order as the sample events.
        // e.g. 0.72 means 72% of the first event's seats are sold.
        double[] sellThrough = {0.72, 0.58, 0.91, 0.44, 0.66, 0.37, 0.53};
        Random random = new Random(2026);        // The "seed" 2026 makes the random numbers repeatable
        LocalDateTime now = LocalDateTime.now(); // Purchase times are counted back from right now
        // Outer loop: one pass per sample event (stops early if there are fewer events or percentages).
        for (int index = 0; index < events.size() && index < sellThrough.length; index++) {
            Event event = events.get(index);
            // Inner loop: one pass per ticket tier of that event.
            for (TicketTier tier : event.getTicketTiers()) {
                // Early Bird sells faster and earlier, VIP sells a little slower.
                // This "switch expression" picks a number based on the tier's name.
                double factor = switch (tier.getTierName()) {
                    case "Early Bird" -> 1.45;
                    case "VIP" -> 0.75;
                    default -> 1.0;
                };
                // How many seats to sell in this tier (Math.min caps it at 100% of the seats).
                int target = (int) Math.round(tier.getAvailableSeats() * Math.min(1.0, sellThrough[index] * factor));
                int sold = 0;
                // Keep making bookings until the target is reached.
                while (sold < target) {
                    // Each booking buys 1-4 tickets (nextInt(4) gives 0-3), but never more than are still needed.
                    int quantity = Math.min(1 + random.nextInt(4), target - sold);
                    // Let the Event take the seats. If it refuses, stop selling this tier.
                    if (!event.purchase(tier.getTierName(), quantity)) {
                        break;
                    }
                    // Pick a random guest and build their email, e.g. "wanjiru.kamau@example.com".
                    String guest = guests[random.nextInt(guests.length)];
                    String email = guest.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com";
                    String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    // Record the booking, then move its purchase time into the past.
                    Booking booking = new Booking(bookingId, event.getEventId(), guest, email, tier.getTierName(),
                            quantity, tier.getPrice() * quantity);
                    booking.setPurchasedAt(now.minusMinutes(sampleMinutesAgo(tier.getTierName(), random)));
                    allBookings.add(booking);
                    sold += quantity;
                }
            }
        }
        // Keep the list in time order, like real purchases arriving one after another.
        allBookings.sort(Comparator.comparing(Booking::getPurchasedAt));
    }

    // Picks how long ago a sample sale happened. Early Bird sales are 2-5 weeks old; other
    // tiers lean towards recent days, so sales speed up as the event gets closer.
    private static long sampleMinutesAgo(String tierName, Random random) {
        // nextDouble() gives a random number from 0 up to 1.
        double days = "Early Bird".equals(tierName)
                ? 14 + random.nextDouble() * 21               // Early Bird: between 14 and 35 days ago
                : 27 * Math.pow(random.nextDouble(), 1.7);    // Others: 0-27 days ago, more often recent
        return (long) (days * 24 * 60);                       // Convert days to minutes
    }

    // Retrieve bookings for a specific event to check sales
    public List<Booking> getBookingsByEvent(String eventId) {
        List<Booking> eventBookings = new ArrayList<>();   // Start an empty result list
        for (Booking b : allBookings) {                    // Look at every booking
            if (b.getEventId().equals(eventId)) {          // Keep it if it's for this event
                eventBookings.add(b);
            }
        }
        return eventBookings;
    }

    // Total money taken for one event: adds up getTotalPrice() from each of its bookings.
    public double calculateEventRevenue(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToDouble(Booking::getTotalPrice).sum();
    }

    // Total tickets sold for one event: adds up the quantity of each of its bookings.
    // "Booking::getQuantity" is a method reference: "call getQuantity() on each booking".
    public int calculateTotalTicketsSold(String eventId) {
        return getBookingsByEvent(eventId).stream().mapToInt(Booking::getQuantity).sum();
    }

    // ---- Analytics: turn bookings into numbers the charts can draw ----

    // Tickets sold on each of the last `days` days, oldest first. The last entry is today.
    // e.g. with days = 30, counts[0] is 29 days ago and counts[29] is today.
    public int[] getDailyTicketSales(String eventId, int days) {
        int[] counts = new int[days];                                  // One counter per day, all starting at 0
        LocalDate firstDay = LocalDate.now().minusDays(days - 1L);     // The oldest day in the range
        for (Booking booking : getBookingsByEvent(eventId)) {
            LocalDate day = booking.getPurchasedAt().toLocalDate();    // Just the date part
            // How many days after firstDay this booking happened = which counter to add to.
            int offset = (int) java.time.temporal.ChronoUnit.DAYS.between(firstDay, day);
            if (offset >= 0 && offset < days) {                        // Ignore bookings outside the range
                counts[offset] += booking.getQuantity();
            }
        }
        return counts;
    }

    // Revenue per tier name, in the event's tier order.
    // e.g. {"Early Bird" -> 25200.0, "Regular" -> 84000.0, "VIP" -> 37500.0}
    public Map<String, Double> getRevenueByTier(Event event) {
        Map<String, Double> revenue = new LinkedHashMap<>();
        // Start every tier at 0, so tiers with no sales still show up.
        event.getTicketTiers().forEach(tier -> revenue.put(tier.getTierName(), 0.0));
        for (Booking booking : getBookingsByEvent(event.getEventId())) {
            // merge(): add this booking's total to whatever that tier already has.
            revenue.merge(tierLabel(booking), booking.getTotalPrice(), Double::sum);
        }
        return revenue;
    }

    // How many bookings bought 1, 2, 3 or 4+ tickets at once.
    // counts[0] = 1 ticket, counts[1] = 2 tickets, counts[2] = 3 tickets, counts[3] = 4 or more.
    public int[] getOrderSizeCounts(String eventId) {
        int[] counts = new int[4];
        for (Booking booking : getBookingsByEvent(eventId)) {
            // Clamp the quantity to 1-4, then subtract 1 to get the array position (0-3).
            counts[Math.min(4, Math.max(1, booking.getQuantity())) - 1]++;
        }
        return counts;
    }

    // The most recent bookings first, up to `limit` of them.
    public List<Booking> getRecentBookings(String eventId, int limit) {
        return getBookingsByEvent(eventId).stream()
                .sorted(Comparator.comparing(Booking::getPurchasedAt).reversed())   // Newest first
                .limit(limit)                                                         // Keep only the first `limit`
                .toList();
    }

    // The tier name to use for a booking, or "General" if it has none (older bookings).
    private static String tierLabel(Booking booking) {
        return booking.getTierName() == null || booking.getTierName().isBlank() ? "General" : booking.getTierName();
    }
}
