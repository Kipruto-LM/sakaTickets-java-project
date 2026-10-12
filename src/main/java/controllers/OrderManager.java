// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import java.sql.Connection;          // The open link to PostgreSQL
import java.sql.PreparedStatement;   // A SQL command with "?" blanks we fill in
import java.sql.ResultSet;           // The rows a SELECT query gives back
import java.sql.SQLException;        // The error Java's database classes throw
import java.time.LocalDate;          // A date without a time
import java.time.LocalDateTime;      // A date with a time
import java.time.temporal.ChronoUnit; // Counts the days between two dates
import java.util.ArrayList;          // A resizable list
import java.util.List;               // The general list type
import java.util.Random;             // Random numbers (for the sample sales)
import java.util.UUID;               // Random unique IDs (for receipt codes)
import models.Booking;               // One purchase (the receipt)
import models.Event;                 // The event model
import models.TicketTier;            // A ticket price level

/**
 * Controller that records ticket purchases ({@link Booking} objects) and works out sales figures.
 * Bookings are stored in the "bookings" table in PostgreSQL. They are read into a list when the
 * app starts (and again on reload()), and each new booking is saved straight away.
 *
 * OOP ideas shown here:
 * - MVC: the checkout screen calls createBooking(), and the dashboard calls the
 *   calculate methods. Neither screen does this work itself.
 * - Objects creating objects: createBooking() is where new Booking objects are made.
 * - Abstraction: the methods at the bottom turn the list of bookings into ready-to-chart numbers.
 */
public class OrderManager {
    private final Connection connection;                    // The shared database connection
    private List<Booking> allBookings = new ArrayList<>();  // Every booking, oldest first

    // Constructor: keeps the connection (dependency injection) and reads the bookings.
    public OrderManager(Connection connection) {
        this.connection = connection;
        reload();
    }

    // Reads every booking from the database again, oldest first.
    public void reload() {
        List<Booking> loaded = new ArrayList<>();
        try {
            PreparedStatement query = connection.prepareStatement("SELECT * FROM bookings ORDER BY purchased_at");
            ResultSet row = query.executeQuery();
            while (row.next()) {                                 // One loop per row
                Booking booking = new Booking(row.getString("booking_id"), row.getString("event_id"),
                        row.getString("customer_name"), row.getString("customer_email"),
                        row.getString("tier_name"), row.getInt("quantity"), row.getDouble("total_price"));
                booking.setPurchasedAt(row.getTimestamp("purchased_at").toLocalDateTime());
                loaded.add(booking);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load the bookings: " + exception.getMessage());
        }
        allBookings = loaded;
    }

    // Creates a new booking with a short random receipt code (e.g. "7FE84F78"), saves it, and returns it.
    public Booking createBooking(String eventId, String customerName, String customerEmail, String tierName,
            int quantity, double totalPrice) {
        String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = new Booking(bookingId, eventId, customerName, customerEmail, tierName,
                quantity, totalPrice);
        saveBooking(booking);                     // Save it to the database first...
        allBookings.add(booking);                 // ...then add it to our list
        return booking;
    }

    // Inserts one booking row.
    private void saveBooking(Booking booking) {
        try {
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO bookings (booking_id, event_id, customer_name, customer_email, tier_name, "
                    + "quantity, total_price, purchased_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            insert.setString(1, booking.getBookingId());
            insert.setString(2, booking.getEventId());
            insert.setString(3, booking.getCustomerName());
            insert.setString(4, booking.getCustomerEmail());
            insert.setString(5, booking.getTierName());
            insert.setInt(6, booking.getQuantity());
            insert.setDouble(7, booking.getTotalPrice());
            insert.setObject(8, booking.getPurchasedAt());      // LocalDateTime -> TIMESTAMP
            insert.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save the booking: " + exception.getMessage());
        }
    }

    // Fills the sample events with past sales so the catalogue and dashboard look realistic.
    // Only used once, on a brand-new database. MainFrame saves the events afterwards so their
    // reduced seat counts are stored too.
    public void seedSampleSales(List<Event> events) {
        // Made-up guest names used for the sample bookings.
        String[] guests = {"Wanjiru Kamau", "Otieno Ochieng", "Achieng Atieno", "Kipchoge Rotich",
            "Njeri Mwangi", "Mutua Musyoka", "Akinyi Odhiambo", "Kiprop Kibet", "Wambui Njoroge",
            "Omondi Owino", "Chebet Langat", "Kamau Githinji", "Nyambura Wairimu", "Barasa Wekesa"};
        Random random = new Random();
        for (Event event : events) {
            for (TicketTier tier : event.getTicketTiers()) {
                // Sell about 30% to 90% of each tier's seats.
                int target = tier.getAvailableSeats() * (30 + random.nextInt(61)) / 100;
                int sold = 0;
                while (sold < target) {
                    int quantity = Math.min(1 + random.nextInt(4), target - sold);   // 1 to 4 tickets
                    if (!event.purchase(tier.getTierName(), quantity)) {
                        break;                                       // The event refused: stop this tier
                    }
                    String guest = guests[random.nextInt(guests.length)];
                    String email = guest.toLowerCase().replace(' ', '.') + "@example.com";
                    String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    Booking booking = new Booking(bookingId, event.getEventId(), guest, email,
                            tier.getTierName(), quantity, tier.getPrice() * quantity);
                    // Give the sale a random time in the last 30 days.
                    booking.setPurchasedAt(LocalDateTime.now().minusMinutes(random.nextInt(30 * 24 * 60)));
                    saveBooking(booking);
                    sold += quantity;
                }
            }
        }
        reload();                                    // Read them back in time order
    }

    // ---- Sales figures ----

    // All bookings for one event.
    public List<Booking> getBookingsByEvent(String eventId) {
        List<Booking> eventBookings = new ArrayList<>();
        for (Booking booking : allBookings) {
            if (booking.getEventId().equals(eventId)) {           // Keep it if it's for this event
                eventBookings.add(booking);
            }
        }
        return eventBookings;
    }

    // Total money taken for one event.
    public double calculateEventRevenue(String eventId) {
        double total = 0;
        for (Booking booking : getBookingsByEvent(eventId)) {
            total += booking.getTotalPrice();
        }
        return total;
    }

    // Total tickets sold for one event.
    public int calculateTotalTicketsSold(String eventId) {
        int total = 0;
        for (Booking booking : getBookingsByEvent(eventId)) {
            total += booking.getQuantity();
        }
        return total;
    }

    // Tickets sold on each of the last `days` days, oldest first. The last entry is today.
    // e.g. with days = 30, counts[0] is 29 days ago and counts[29] is today.
    public int[] getDailyTicketSales(String eventId, int days) {
        int[] counts = new int[days];                                  // One counter per day, all starting at 0
        LocalDate firstDay = LocalDate.now().minusDays(days - 1);      // The oldest day in the range
        for (Booking booking : getBookingsByEvent(eventId)) {
            LocalDate day = booking.getPurchasedAt().toLocalDate();    // Just the date part
            int index = (int) ChronoUnit.DAYS.between(firstDay, day);  // Which counter this day belongs to
            if (index >= 0 && index < days) {                          // Ignore bookings outside the range
                counts[index] += booking.getQuantity();
            }
        }
        return counts;
    }

    // The newest bookings for one event, up to `limit` of them. The list is oldest-first,
    // so we walk it backwards from the end.
    public List<Booking> getRecentBookings(String eventId, int limit) {
        List<Booking> eventBookings = getBookingsByEvent(eventId);
        List<Booking> result = new ArrayList<>();
        for (int i = eventBookings.size() - 1; i >= 0 && result.size() < limit; i--) {
            result.add(eventBookings.get(i));
        }
        return result;
    }
}
