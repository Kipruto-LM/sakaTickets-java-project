// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import database.DatabaseException;       // Our own error type for database problems
import java.sql.Connection;              // The open link to PostgreSQL
import java.sql.PreparedStatement;       // A SQL command with "?" blanks we fill in safely
import java.sql.ResultSet;               // The rows a SELECT query gives back
import java.sql.SQLException;            // Error thrown by Java's database classes
import java.sql.Statement;               // Runs a plain SQL command
import java.time.LocalDate;              // A calendar date
import java.util.ArrayList;              // A resizable list
import java.util.Collections;            // Helpers for lists, e.g. making a read-only view
import java.util.LinkedHashMap;          // A map (key -> value) that keeps insertion order
import java.util.List;                   // The general list type
import java.util.Map;                    // The general map type
import java.util.Objects;                // Null-safe helpers such as Objects.equals()
import java.util.UUID;                   // Random unique IDs
import models.Event;                     // The event model
import models.TicketTier;                // The ticket tier model

/**
 * Controller that owns the events. Events, their ticket tiers and their custom details are
 * stored in PostgreSQL (tables "events", "ticket_tiers" and "event_details").
 *
 * How it works: the events are read from the database into a list when the app starts (and again
 * whenever reload() is called). Every change - adding, editing, archiving, selling seats - is
 * written straight to the database, so it is never lost when the app closes.
 *
 * OOP ideas shown here:
 * - MVC: this is a "Controller". Screens ask it for events instead of touching the database.
 * - Encapsulation: the SQL is all private. Other classes just call addEvent(), updateEvent() and so on.
 * - Dependency injection: the database Connection is handed in through the constructor.
 * - Objects working together: rows from the database are turned into Event and TicketTier objects.
 */
public class EventManager {
    private final Connection connection;          // The shared database connection
    private List<Event> events = new ArrayList<>();   // The events as last read from the database
    private boolean newDatabase;                  // true if the sample events were just created

    // Constructor: reads the events. If the database has none yet, it saves the sample events first.
    public EventManager(Connection connection) {
        this.connection = connection;
        reload();
        if (events.isEmpty()) {
            for (Event sample : createSampleEvents()) {
                addEvent(sample);
            }
            newDatabase = true;
        }
    }

    // True on the very first run, right after the sample events were saved (MainFrame then adds sample sales).
    public boolean isNewDatabase() {
        return newDatabase;
    }

    // ---- Reading ----

    // Reads every event, tier and custom detail from the database again, so the app shows the
    // latest data (including changes made by teammates using the same database).
    public void reload() {
        // A map from event ID to Event lets us attach each tier and detail to the right event.
        Map<String, Event> byId = new LinkedHashMap<>();
        try (Statement query = connection.createStatement()) {
            // 1. The events themselves, soonest first.
            try (ResultSet row = query.executeQuery("SELECT * FROM events ORDER BY event_date, event_id")) {
                while (row.next()) {                                    // One loop per row
                    Event event = new Event();
                    event.setEventId(row.getString("event_id"));
                    event.setHostId(row.getString("host_id"));
                    event.setTitle(row.getString("title"));
                    event.setDate(row.getObject("event_date", LocalDate.class).toString());   // DATE -> "2026-10-15"
                    event.setVenue(row.getString("venue"));
                    event.setStatus(row.getString("status"));
                    event.setCategory(row.getString("category"));
                    event.setBannerImagePath(row.getString("banner_image_path"));
                    event.setSalesStartDate(row.getObject("sales_start", LocalDate.class));   // May be null
                    event.setSalesEndDate(row.getObject("sales_end", LocalDate.class));
                    event.setArchived(row.getBoolean("archived"));
                    byId.put(event.getEventId(), event);
                }
            }
            // 2. The tiers, in the order the host created them.
            Map<String, List<TicketTier>> tiersById = new LinkedHashMap<>();
            try (ResultSet row = query.executeQuery("SELECT * FROM ticket_tiers ORDER BY event_id, position")) {
                while (row.next()) {
                    TicketTier tier = new TicketTier(row.getString("tier_name"), row.getDouble("price"),
                            row.getInt("initial_capacity"), row.getInt("available_seats"));
                    // computeIfAbsent: get this event's list, making an empty one the first time.
                    tiersById.computeIfAbsent(row.getString("event_id"), id -> new ArrayList<>()).add(tier);
                }
            }
            // 3. The custom details, in order.
            Map<String, Map<String, String>> detailsById = new LinkedHashMap<>();
            try (ResultSet row = query.executeQuery("SELECT * FROM event_details ORDER BY event_id, position")) {
                while (row.next()) {
                    detailsById.computeIfAbsent(row.getString("event_id"), id -> new LinkedHashMap<>())
                            .put(row.getString("name"), row.getString("value"));
                }
            }
            // Attach the tiers and details to their events.
            for (Event event : byId.values()) {
                event.setTicketTiers(tiersById.getOrDefault(event.getEventId(), new ArrayList<>()));
                event.setCustomDetails(detailsById.getOrDefault(event.getEventId(), new LinkedHashMap<>()));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load the events", exception);
        }
        events = new ArrayList<>(byId.values());
    }

    // Read-only view: callers can't add or remove events through this list.
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    // Finds one event by its ID. Returns null if no event has that ID.
    public Event getEventById(String eventId) {
        for (Event event : events) {
            if (Objects.equals(event.getEventId(), eventId)) {
                return event;
            }
        }
        return null;
    }

    // All events owned by one host, in a new list.
    public List<Event> getEventsByHostId(String hostId) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (Objects.equals(event.getHostId(), hostId)) {   // Keep it if this host owns it
                result.add(event);
            }
        }
        return result;
    }

    // Only events that are not archived, for the guest catalogue.
    public List<Event> getPublicEvents() {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (!event.isArchived()) {                         // Skip hidden (archived) events
                result.add(event);
            }
        }
        return result;
    }

    // ---- Changing ----

    // Saves a brand-new event (with its tiers and details) and adds it to the list.
    public void addEvent(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        writeEvent(event, true);
        events.add(event);
    }

    // Saves the changes to an existing event and swaps it into the list.
    public void updateEvent(Event updatedEvent) {
        Objects.requireNonNull(updatedEvent, "event must not be null");
        writeEvent(updatedEvent, false);
        for (int index = 0; index < events.size(); index++) {
            if (Objects.equals(events.get(index).getEventId(), updatedEvent.getEventId())) {
                events.set(index, updatedEvent);
                return;
            }
        }
        events.add(updatedEvent);                    // Wasn't in our list yet (e.g. added by a teammate)
    }

    // Archives (hides) or restores an event.
    public void setArchived(String eventId, boolean archived) {
        Event event = getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        event.setArchived(archived);                 // Change the object...
        updateEvent(event);                          // ...and save it
    }

    // Builds a brand-new Event (and new TicketTier objects) from an existing one, and saves it.
    // New objects are made instead of sharing the old ones, so changing the copy never affects the original.
    public Event duplicateEvent(String eventId, String hostId) {
        Event source = getEventById(eventId);
        if (source == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        // Fresh tiers start with all seats available.
        List<TicketTier> copiedTiers = new ArrayList<>();
        for (TicketTier tier : source.getTicketTiers()) {
            copiedTiers.add(new TicketTier(tier.getTierName(), tier.getPrice(), tier.getInitialCapacity()));
        }
        // The copy gets a new random ID like "EVT-3F9A21BC" and "(Copy)" added to its title.
        Event copy = new Event("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                source.getTitle() + " (Copy)", source.getDate(), source.getVenue(), copiedTiers, "ON_SALE", hostId);
        copy.setSalesStartDate(source.getSalesStartDate());
        copy.setSalesEndDate(source.getSalesEndDate());
        copy.setCategory(source.getCategory());
        copy.setBannerImagePath(source.getBannerImagePath());
        copy.setCustomDetails(source.getCustomDetails());
        addEvent(copy);
        return copy;
    }

    // Sells seats in one tier. Returns true if it worked, false if sales are closed or seats ran out.
    // The seat count is changed in the database with ONE command that only succeeds if enough
    // seats are left, so two guests buying the last seat at the same moment can't both get it.
    public boolean purchaseTickets(Event event, String tierName, int quantity) {
        if (!event.canPurchaseOn(LocalDate.now()) || quantity <= 0) {
            return false;                                       // The event's own rule says no
        }
        String sql = "UPDATE ticket_tiers SET available_seats = available_seats - ? "
                + "WHERE event_id = ? AND tier_name = ? AND available_seats >= ?";
        try (PreparedStatement update = connection.prepareStatement(sql)) {
            update.setInt(1, quantity);
            update.setString(2, event.getEventId());
            update.setString(3, tierName);
            update.setInt(4, quantity);
            if (update.executeUpdate() == 0) {                  // 0 rows changed = not enough seats left
                return false;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not reserve the seats", exception);
        }
        // Keep our copy in step: take the seats from the Event object too (it may mark itself SOLD_OUT).
        event.purchase(tierName, quantity);
        if ("SOLD_OUT".equals(event.getStatus())) {
            updateEvent(event);                                 // Save the new status
        }
        return true;
    }

    // Stores the Event object's current seat counts exactly as they are. Only used once, after the
    // sample sales are created on a brand-new database (normal sales go through purchaseTickets()).
    public void saveSeatCounts(Event event) {
        String sql = "UPDATE ticket_tiers SET available_seats = ? WHERE event_id = ? AND tier_name = ?";
        try (PreparedStatement update = connection.prepareStatement(sql)) {
            for (TicketTier tier : event.getTicketTiers()) {
                update.setInt(1, tier.getAvailableSeats());
                update.setString(2, event.getEventId());
                update.setString(3, tier.getTierName());
                update.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save the seat counts", exception);
        }
    }

    // ---- Database writing (private) ----

    // Writes one event, its tiers and its details. isNew = INSERT the event row, otherwise UPDATE it.
    // It runs as a "transaction": either every step is saved, or (if any step fails) none of them are.
    private void writeEvent(Event event, boolean isNew) {
        try {
            connection.setAutoCommit(false);                    // Start the transaction
            String eventSql = isNew
                    ? "INSERT INTO events (host_id, title, event_date, venue, status, category, "
                        + "banner_image_path, sales_start, sales_end, archived, event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    : "UPDATE events SET host_id = ?, title = ?, event_date = ?, venue = ?, status = ?, category = ?, "
                        + "banner_image_path = ?, sales_start = ?, sales_end = ?, archived = ? WHERE event_id = ?";
            // Both versions use the same 11 blanks in the same order, so one block fills either.
            try (PreparedStatement save = connection.prepareStatement(eventSql)) {
                save.setString(1, event.getHostId());
                save.setString(2, event.getTitle());
                save.setObject(3, LocalDate.parse(event.getDate()));     // "2026-10-15" -> DATE
                save.setString(4, event.getVenue());
                save.setString(5, event.getStatus());
                save.setString(6, event.getCategory());
                save.setString(7, event.getBannerImagePath());
                save.setObject(8, event.getSalesStartDate());            // null is saved as NULL
                save.setObject(9, event.getSalesEndDate());
                save.setBoolean(10, event.isArchived());
                save.setString(11, event.getEventId());
                save.executeUpdate();
            }
            // Tiers. First remove any tier the host deleted (one not in the event's list any more).
            List<String> tierNames = new ArrayList<>();
            for (TicketTier tier : event.getTicketTiers()) {
                tierNames.add(tier.getTierName());
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM ticket_tiers WHERE event_id = ? AND NOT (tier_name = ANY (?))")) {
                delete.setString(1, event.getEventId());
                delete.setArray(2, connection.createArrayOf("text", tierNames.toArray()));   // A list of names for SQL
                delete.executeUpdate();
            }
            // Then add each tier, or update it if it's already there ("ON CONFLICT ... DO UPDATE").
            // For an existing tier, the seats already sold in the DATABASE are kept: if the capacity
            // goes up by 10, the available seats go up by 10. This way, editing an event can never undo
            // tickets that someone else sold in the meantime.
            String tierSql = "INSERT INTO ticket_tiers (event_id, position, tier_name, price, initial_capacity, "
                    + "available_seats) VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (event_id, tier_name) DO UPDATE SET position = EXCLUDED.position, "
                    + "price = EXCLUDED.price, "
                    + "available_seats = ticket_tiers.available_seats + (EXCLUDED.initial_capacity - ticket_tiers.initial_capacity), "
                    + "initial_capacity = EXCLUDED.initial_capacity";
            try (PreparedStatement insert = connection.prepareStatement(tierSql)) {
                int position = 0;
                for (TicketTier tier : event.getTicketTiers()) {
                    insert.setString(1, event.getEventId());
                    insert.setInt(2, position++);                        // Remember the order
                    insert.setString(3, tier.getTierName());
                    insert.setDouble(4, tier.getPrice());
                    insert.setInt(5, tier.getInitialCapacity());
                    insert.setInt(6, tier.getAvailableSeats());          // Only used for a brand-new tier
                    insert.executeUpdate();
                }
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM event_details WHERE event_id = ?")) {
                delete.setString(1, event.getEventId());
                delete.executeUpdate();
            }
            String detailSql = "INSERT INTO event_details (event_id, position, name, value) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insert = connection.prepareStatement(detailSql)) {
                int position = 0;
                for (Map.Entry<String, String> detail : event.getCustomDetails().entrySet()) {
                    insert.setString(1, event.getEventId());
                    insert.setInt(2, position++);
                    insert.setString(3, detail.getKey());
                    insert.setString(4, detail.getValue());
                    insert.executeUpdate();
                }
            }
            connection.commit();                                // Everything worked: save it all
        } catch (SQLException exception) {
            try {
                connection.rollback();                          // Something failed: undo every step
            } catch (SQLException ignored) {
                // Nothing more we can do; the original error below explains the problem.
            }
            throw new DatabaseException("Could not save the event \"" + event.getTitle() + "\"", exception);
        } finally {
            try {
                connection.setAutoCommit(true);                 // Back to normal: each command saves at once
            } catch (SQLException ignored) {
                // The connection is broken; the next database call will report it.
            }
        }
    }

    // ---- Sample data (only used the first time, when the database is empty) ----

    // Makes the seven sample events, all owned by the built-in host account ("admin-001").
    private List<Event> createSampleEvents() {
        List<Event> samples = new ArrayList<>();
        // sampleEvent(id, title, date, venue, total seats, Regular ticket price, category, picture)
        samples.add(sampleEvent("EVT-001", "Nairobi Tech Meetup", "2026-10-15", "Nairobi National Museum",
                120, 1500.00, "Tech", "/images/event-tech.jpg"));
        samples.add(sampleEvent("EVT-002", "Amapiano Sundays", "2026-10-18", "KICC Rooftop",
                200, 2500.00, "Nightlife", "/images/event-nightlife.jpg"));
        samples.add(sampleEvent("EVT-003", "Acoustic Night", "2026-10-24", "The Alchemist",
                80, 1200.00, "Music", "/images/event-music.jpg"));
        samples.add(sampleEvent("EVT-004", "Swahili Food Festival", "2026-11-02", "Uhuru Gardens",
                300, 1800.00, "Food", "/images/event-food.jpg"));
        samples.add(sampleEvent("EVT-005", "Nairobi Jazz Evening", "2026-11-08", "K1 Klub House",
                100, 2200.00, "Jazz", "/images/event-jazz.jpg"));
        samples.add(sampleEvent("EVT-006", "Coastal Creators Expo", "2026-11-14", "Sarit Expo Centre",
                250, 1000.00, "Arts & Culture", "/images/event-expo.jpg"));
        samples.add(sampleEvent("EVT-007", "Rift Valley Trail Run", "2026-11-22", "Hell's Gate National Park",
                180, 750.00, "Outdoors", "/images/event-trail.jpg"));
        return samples;
    }

    // Builds a sample event with three ticket tiers, so the analytics pages have tiers to compare:
    // Early Bird (20% of seats, cheaper), Regular (65%) and VIP (15%, premium price).
    private Event sampleEvent(String eventId, String title, String date, String venue, int capacity,
            double regularPrice, String category, String imagePath) {
        // "(int)" turns the rounded decimal into a whole number of seats.
        int earlyBirdSeats = (int) Math.round(capacity * 0.20);
        int vipSeats = (int) Math.round(capacity * 0.15);
        // Three tier objects. Regular gets whatever seats are left, so the total always equals capacity.
        List<TicketTier> tiers = List.of(
                new TicketTier("Early Bird", roundPrice(regularPrice * 0.7), earlyBirdSeats),   // 30% cheaper
                new TicketTier("Regular", regularPrice, capacity - earlyBirdSeats - vipSeats),
                new TicketTier("VIP", roundPrice(regularPrice * 2.5), vipSeats));               // 2.5x the price
        Event event = new Event(eventId, title, date, venue, tiers, "ON_SALE", "admin-001");
        event.setCategory(category);
        event.setBannerImagePath(imagePath);
        return event;
    }

    // Rounds a price to the nearest 50 shillings so sample prices look natural.
    // e.g. 1050.0 / 50 = 21 -> 21 * 50 = 1050;  1083 / 50 = 21.66 -> 22 * 50 = 1100.
    private static double roundPrice(double price) {
        return Math.round(price / 50.0) * 50.0;
    }
}
