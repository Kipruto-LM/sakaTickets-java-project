package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import models.Booking;
import models.Event;
import models.TicketTier;

/**
 * Small SQLite helper for the project. Each method opens its own connection,
 * runs a few SQL statements, and closes the connection when finished.
 *
 * Core concepts:
 * - Abstraction: controllers call Java methods rather than building SQL themselves.
 * - Encapsulation: the connection URL and SQL operations stay inside this class.
 * - Transactions: related writes commit together or roll back together.
 * - Resource management: try-with-resources closes connections and statements.
 * - Object mapping: query rows are reconstructed as Event and TicketTier objects.
 */
public final class Database {
    private static final String URL = "jdbc:sqlite:sakatickets.db";

    /** Prevents instances because every database operation is static. */
    private Database() {
    }

    /** Creates the tables once when the application starts. */
    public static void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            // Each table represents one kind of data; foreign keys connect tiers and bookings to events.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS events (
                        event_id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        event_date TEXT NOT NULL,
                        venue TEXT NOT NULL,
                        status TEXT NOT NULL,
                        host_id TEXT NOT NULL,
                        category TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_tiers (
                        event_id TEXT NOT NULL,
                        tier_name TEXT NOT NULL,
                        price REAL NOT NULL,
                        initial_capacity INTEGER NOT NULL,
                        available_seats INTEGER NOT NULL,
                        PRIMARY KEY (event_id, tier_name),
                        FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        booking_id TEXT PRIMARY KEY,
                        event_id TEXT NOT NULL,
                        customer_name TEXT NOT NULL,
                        customer_email TEXT NOT NULL,
                        tier_name TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        total_price REAL NOT NULL,
                        FOREIGN KEY (event_id) REFERENCES events(event_id)
                    )
                    """);
        }
    }

    /** Reads events and their ticket tiers from the database into Java objects. */
    public static List<Event> loadEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = """
                SELECT event_id, title, event_date, venue, status, host_id, category
                FROM events ORDER BY rowid
                """;

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            // Rebuild each event together with its child ticket tiers.
            while (results.next()) {
                String eventId = results.getString("event_id");
                List<TicketTier> tiers = loadTicketTiers(connection, eventId);
                Event event = new Event(eventId, results.getString("title"),
                        results.getString("event_date"), results.getString("venue"), tiers,
                        results.getString("status"), results.getString("host_id"));
                event.setCategory(results.getString("category"));
                events.add(event);
            }
        }
        return events;
    }

    /** Saves one event and its tiers as a single transaction. */
    public static void saveEvent(Event event) throws SQLException {
        saveEvents(List.of(event));
    }

    /** Saves a group of events together, used for the first-run sample data. */
    public static void saveEvents(List<Event> events) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                // Save all event rows and their tiers as one all-or-nothing operation.
                for (Event event : events) {
                    saveEvent(connection, event);
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    /** Inserts or updates one event row, then replaces its tier rows within the caller's transaction. */
    private static void saveEvent(Connection connection, Event event) throws SQLException {
        // Bound parameters separate user data from SQL syntax; the conflict clause updates an existing event.
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO events (event_id, title, event_date, venue, status, host_id, category)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(event_id) DO UPDATE SET
                    title = excluded.title,
                    event_date = excluded.event_date,
                    venue = excluded.venue,
                    status = excluded.status,
                    host_id = excluded.host_id,
                    category = excluded.category
                """)) {
            statement.setString(1, event.getEventId());
            statement.setString(2, event.getTitle());
            statement.setString(3, event.getDate());
            statement.setString(4, event.getVenue());
            statement.setString(5, event.getStatus());
            statement.setString(6, event.getHostId());
            statement.setString(7, event.getCategory());
            statement.executeUpdate();
        }

        try (PreparedStatement deleteTiers = connection.prepareStatement(
                "DELETE FROM ticket_tiers WHERE event_id = ?")) {
            deleteTiers.setString(1, event.getEventId());
            deleteTiers.executeUpdate();
        }

        try (PreparedStatement insertTier = connection.prepareStatement("""
                INSERT INTO ticket_tiers
                    (event_id, tier_name, price, initial_capacity, available_seats)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (TicketTier tier : event.getTicketTiers()) {
                insertTier.setString(1, event.getEventId());
                insertTier.setString(2, tier.getTierName());
                insertTier.setDouble(3, tier.getPrice());
                insertTier.setInt(4, tier.getInitialCapacity());
                insertTier.setInt(5, tier.getAvailableSeats());
                insertTier.addBatch();
            }
            insertTier.executeBatch();
        }
    }

    /**
     * Saves a booking and reduces ticket availability atomically. Returns the
     * updated inventory values so the in-memory Event can stay in sync.
     */
    public static PurchaseResult saveBooking(Booking booking, String tierName, int quantity)
            throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ticket quantity must be positive.");
        }
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                // Check current database state inside the transaction before changing inventory.
                String status = findEventStatus(connection, booking.getEventId());
                if (!"ON_SALE".equals(status)) {
                    connection.rollback();
                    return new PurchaseResult(false, 0, status);
                }

                // The available-seat condition prevents inventory from becoming negative.
                int rowsUpdated;
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE ticket_tiers
                        SET available_seats = available_seats - ?
                        WHERE event_id = ? AND tier_name = ? AND available_seats >= ?
                        """)) {
                    update.setInt(1, quantity);
                    update.setString(2, booking.getEventId());
                    update.setString(3, tierName);
                    update.setInt(4, quantity);
                    rowsUpdated = update.executeUpdate();
                }
                if (rowsUpdated == 0) {
                    connection.rollback();
                    return new PurchaseResult(false, 0, status);
                }

                // Only mark the whole event sold out when no tier has seats remaining.
                int remaining = getAvailableSeats(connection, booking.getEventId());
                String updatedStatus = remaining == 0 ? "SOLD_OUT" : status;
                if (remaining == 0) {
                    try (PreparedStatement updateStatus = connection.prepareStatement(
                            "UPDATE events SET status = ? WHERE event_id = ?")) {
                        updateStatus.setString(1, updatedStatus);
                        updateStatus.setString(2, booking.getEventId());
                        updateStatus.executeUpdate();
                    }
                }

                int tierAvailable = getTierAvailableSeats(connection, booking.getEventId(), tierName);
                insertBooking(connection, booking);
                connection.commit();
                return new PurchaseResult(true, tierAvailable, updatedStatus);
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    /** Loads every ticket tier belonging to one event using the caller's connection. */
    private static List<TicketTier> loadTicketTiers(Connection connection, String eventId)
            throws SQLException {
        List<TicketTier> tiers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT tier_name, price, initial_capacity, available_seats
                FROM ticket_tiers WHERE event_id = ?
                """)) {
            statement.setString(1, eventId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    tiers.add(new TicketTier(results.getString("tier_name"),
                            results.getDouble("price"), results.getInt("initial_capacity"),
                            results.getInt("available_seats")));
                }
            }
        }
        return tiers;
    }

    /** Reads the current sales status so checkout uses database state, not stale UI state. */
    private static String findEventStatus(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status FROM events WHERE event_id = ?")) {
            statement.setString(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("status") : null;
            }
        }
    }

    /** Adds the remaining inventory across all ticket tiers for an event. */
    private static int getAvailableSeats(Connection connection, String eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(available_seats), 0) FROM ticket_tiers WHERE event_id = ?")) {
            statement.setString(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    /** Reads the remaining inventory for one tier after the purchase update. */
    private static int getTierAvailableSeats(Connection connection, String eventId, String tierName)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT available_seats FROM ticket_tiers
                WHERE event_id = ? AND tier_name = ?
                """)) {
            statement.setString(1, eventId);
            statement.setString(2, tierName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Ticket tier not found after saving booking: " + tierName);
                }
                return result.getInt("available_seats");
            }
        }
    }

    /** Inserts the receipt row using the same connection and transaction as the inventory change. */
    private static void insertBooking(Connection connection, Booking booking) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO bookings
                    (booking_id, event_id, customer_name, customer_email, tier_name, quantity, total_price)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, booking.getBookingId());
            statement.setString(2, booking.getEventId());
            statement.setString(3, booking.getCustomerName());
            statement.setString(4, booking.getCustomerEmail());
            statement.setString(5, booking.getTierName());
            statement.setInt(6, booking.getQuantity());
            statement.setDouble(7, booking.getTotalPrice());
            statement.executeUpdate();
        }
    }

    /** Opens the SQLite file and enables foreign-key checks for this connection. */
    private static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    /** Attempts to undo a failed transaction while preserving the original SQL exception. */
    private static void rollback(Connection connection, SQLException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    /**
     * Immutable result returned to the controller after a purchase attempt.
     * A rejected purchase carries no inventory change; a successful one carries updated values.
     */
    public record PurchaseResult(boolean purchased, int tierAvailableSeats, String eventStatus) {
    }
}
