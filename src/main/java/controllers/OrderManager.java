package controllers;

import models.Booking;
import models.Event;
import models.TicketTier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * - Abstraction: the analytics methods at the bottom turn the raw list of bookings into
 *   ready-to-chart numbers, so the screens don't need to know how they are worked out.
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
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < events.size() && index < sellThrough.length; index++) {
            Event event = events.get(index);
            for (TicketTier tier : event.getTicketTiers()) {
                // Early Bird sells faster and earlier, VIP sells a little slower.
                double factor = switch (tier.getTierName()) {
                    case "Early Bird" -> 1.45;
                    case "VIP" -> 0.75;
                    default -> 1.0;
                };
                int target = (int) Math.round(tier.getAvailableSeats() * Math.min(1.0, sellThrough[index] * factor));
                int sold = 0;
                while (sold < target) {
                    int quantity = Math.min(1 + random.nextInt(4), target - sold);
                    if (!event.purchase(tier.getTierName(), quantity)) {
                        break;
                    }
                    String guest = guests[random.nextInt(guests.length)];
                    String email = guest.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com";
                    String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
        double days = "Early Bird".equals(tierName)
                ? 14 + random.nextDouble() * 21
                : 27 * Math.pow(random.nextDouble(), 1.7);
        return (long) (days * 24 * 60);
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

    // ---- Analytics: turn bookings into numbers the charts can draw ----

    // Tickets sold on each of the last `days` days, oldest first. The last entry is today.
    public int[] getDailyTicketSales(String eventId, int days) {
        int[] counts = new int[days];
        LocalDate firstDay = LocalDate.now().minusDays(days - 1L);
        for (Booking booking : getBookingsByEvent(eventId)) {
            LocalDate day = booking.getPurchasedAt().toLocalDate();
            int offset = (int) java.time.temporal.ChronoUnit.DAYS.between(firstDay, day);
            if (offset >= 0 && offset < days) {
                counts[offset] += booking.getQuantity();
            }
        }
        return counts;
    }

    // Tickets sold in the last `days` days (for "12 sold this week" style messages).
    public int getRecentTicketsSold(String eventId, int days) {
        int total = 0;
        for (int count : getDailyTicketSales(eventId, days)) {
            total += count;
        }
        return total;
    }

    // Revenue per tier name, in the event's tier order.
    public Map<String, Double> getRevenueByTier(Event event) {
        Map<String, Double> revenue = new LinkedHashMap<>();
        event.getTicketTiers().forEach(tier -> revenue.put(tier.getTierName(), 0.0));
        for (Booking booking : getBookingsByEvent(event.getEventId())) {
            revenue.merge(tierLabel(booking), booking.getTotalPrice(), Double::sum);
        }
        return revenue;
    }

    // How many bookings bought 1, 2, 3 or 4+ tickets at once.
    public int[] getOrderSizeCounts(String eventId) {
        int[] counts = new int[4];
        for (Booking booking : getBookingsByEvent(eventId)) {
            counts[Math.min(4, Math.max(1, booking.getQuantity())) - 1]++;
        }
        return counts;
    }

    // The most recent bookings first, up to `limit` of them.
    public List<Booking> getRecentBookings(String eventId, int limit) {
        return getBookingsByEvent(eventId).stream()
                .sorted(Comparator.comparing(Booking::getPurchasedAt).reversed())
                .limit(limit)
                .toList();
    }

    private static String tierLabel(Booking booking) {
        return booking.getTierName() == null || booking.getTierName().isBlank() ? "General" : booking.getTierName();
    }
}
