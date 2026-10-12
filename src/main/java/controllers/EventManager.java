// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import java.sql.Connection;              // The open link to PostgreSQL
import java.sql.PreparedStatement;       // A SQL command with "?" blanks we fill in
import java.sql.ResultSet;               // The rows a SELECT query gives back
import java.sql.SQLException;            // The error Java's database classes throw
import java.time.LocalDate;              // A calendar date
import java.util.ArrayList;              // A resizable list
import java.util.LinkedHashMap;          // A map (key -> value) that keeps insertion order
import java.util.List;                   // The general list type
import java.util.Map;                    // The general map type
import java.util.UUID;                   // Random unique IDs
import models.Event;                     // The event model
import models.TicketTier;                // The ticket tier model
import models.User;                      // For the built-in admin's ID

/**
 * Controller that owns the events. Events, their ticket tiers and their custom details are
 * stored in PostgreSQL (tables "events", "ticket_tiers" and "event_details").
 *
 * How it works: the events are read from the database into a list when the app starts (and again
 * whenever reload() is called). Every change - adding, editing, archiving, deleting, selling
 * seats - is written straight to the database, so nothing is lost when the app closes.
 *
 * OOP ideas shown here:
 * - MVC: this is a "Controller". Screens ask it for events instead of touching the database.
 * - Encapsulation: the SQL is all inside this class. Other classes just call addEvent(), updateEvent()...
 * - Dependency injection: the database Connection is handed in through the constructor.
 * - Objects working together: database rows are turned into Event and TicketTier objects.
 */
public class EventManager {
    private final Connection connection;               // The shared database connection
    private List<Event> events = new ArrayList<>();    // The events as last read from the database
    private boolean newDatabase = false;               // true if the sample events were just created

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

    // Reads every event (with its tiers and details) from the database again, so the app shows the latest data.
    public void reload() {
        List<Event> loaded = new ArrayList<>();
        try {
            PreparedStatement query = connection.prepareStatement("SELECT * FROM events ORDER BY event_date");
            ResultSet row = query.executeQuery();
            while (row.next()) {                                     // One loop per event row
                Event event = new Event();
                event.setEventId(row.getString("event_id"));
                event.setHostId(row.getString("host_id"));
                event.setTitle(row.getString("title"));
                event.setDate(row.getString("event_date"));          // e.g. "2026-10-15"
                event.setVenue(row.getString("venue"));
                event.setStatus(row.getString("status"));
                event.setCategory(row.getString("category"));
                event.setBannerImagePath(row.getString("banner_image_path"));
                event.setArchived(row.getBoolean("archived"));
                event.setTicketTiers(loadTiers(event.getEventId()));
                event.setCustomDetails(loadDetails(event.getEventId()));
                loaded.add(event);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load the events: " + exception.getMessage());
        }
        events = loaded;
    }

    // Reads one event's ticket tiers, in the order the host listed them.
    private List<TicketTier> loadTiers(String eventId) throws SQLException {
        List<TicketTier> tiers = new ArrayList<>();
        PreparedStatement query = connection.prepareStatement(
                "SELECT * FROM ticket_tiers WHERE event_id = ? ORDER BY position");
        query.setString(1, eventId);                                 // Fill in blank number 1
        ResultSet row = query.executeQuery();
        while (row.next()) {
            tiers.add(new TicketTier(row.getString("tier_name"), row.getDouble("price"),
                    row.getInt("initial_capacity"), row.getInt("available_seats")));
        }
        return tiers;
    }

    // Reads one event's custom details, in order.
    private Map<String, String> loadDetails(String eventId) throws SQLException {
        Map<String, String> details = new LinkedHashMap<>();
        PreparedStatement query = connection.prepareStatement(
                "SELECT * FROM event_details WHERE event_id = ? ORDER BY position");
        query.setString(1, eventId);
        ResultSet row = query.executeQuery();
        while (row.next()) {
            details.put(row.getString("name"), row.getString("value"));
        }
        return details;
    }

    // All events (archived ones included).
    public List<Event> getEvents() {
        return events;
    }

    // Finds one event by its ID. Returns null if no event has that ID.
    public Event getEventById(String eventId) {
        for (Event event : events) {
            if (event.getEventId().equals(eventId)) {
                return event;
            }
        }
        return null;
    }

    // All events owned by one host, in a new list.
    public List<Event> getEventsByHostId(String hostId) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (event.getHostId().equals(hostId)) {          // Keep it if this host owns it
                result.add(event);
            }
        }
        return result;
    }

    // Only events that are not archived, for the guest catalogue.
    public List<Event> getPublicEvents() {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (!event.isArchived()) {                       // Skip hidden (archived) events
                result.add(event);
            }
        }
        return result;
    }

    // ---- Changing ----

    // Saves a brand-new event (with its tiers and details) and adds it to the list.
    public void addEvent(Event event) {
        try {
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO events (event_id, host_id, title, event_date, venue, status, category, "
                    + "banner_image_path, archived) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            insert.setString(1, event.getEventId());
            insert.setString(2, event.getHostId());
            insert.setString(3, event.getTitle());
            insert.setObject(4, LocalDate.parse(event.getDate()));   // "2026-10-15" -> a DATE
            insert.setString(5, event.getVenue());
            insert.setString(6, event.getStatus());
            insert.setString(7, event.getCategory());
            insert.setString(8, event.getBannerImagePath());
            insert.setBoolean(9, event.isArchived());
            insert.executeUpdate();
            saveTiersAndDetails(event);
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save the event: " + exception.getMessage());
        }
        events.add(event);
    }

    // Saves the changes to an existing event (the list already holds this same Event object).
    public void updateEvent(Event event) {
        try {
            PreparedStatement update = connection.prepareStatement(
                    "UPDATE events SET title = ?, event_date = ?, venue = ?, status = ?, category = ?, "
                    + "banner_image_path = ?, archived = ? WHERE event_id = ?");
            update.setString(1, event.getTitle());
            update.setObject(2, LocalDate.parse(event.getDate()));
            update.setString(3, event.getVenue());
            update.setString(4, event.getStatus());
            update.setString(5, event.getCategory());
            update.setString(6, event.getBannerImagePath());
            update.setBoolean(7, event.isArchived());
            update.setString(8, event.getEventId());
            update.executeUpdate();
            saveTiersAndDetails(event);
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save the event: " + exception.getMessage());
        }
    }

    // Replaces an event's tiers and details in the database: delete the old rows, insert the current ones.
    private void saveTiersAndDetails(Event event) throws SQLException {
        PreparedStatement deleteTiers = connection.prepareStatement("DELETE FROM ticket_tiers WHERE event_id = ?");
        deleteTiers.setString(1, event.getEventId());
        deleteTiers.executeUpdate();
        int position = 0;                                    // Remembers the order of the tiers
        for (TicketTier tier : event.getTicketTiers()) {
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO ticket_tiers (event_id, position, tier_name, price, initial_capacity, "
                    + "available_seats) VALUES (?, ?, ?, ?, ?, ?)");
            insert.setString(1, event.getEventId());
            insert.setInt(2, position);
            insert.setString(3, tier.getTierName());
            insert.setDouble(4, tier.getPrice());
            insert.setInt(5, tier.getInitialCapacity());
            insert.setInt(6, tier.getAvailableSeats());
            insert.executeUpdate();
            position++;
        }

        PreparedStatement deleteDetails = connection.prepareStatement("DELETE FROM event_details WHERE event_id = ?");
        deleteDetails.setString(1, event.getEventId());
        deleteDetails.executeUpdate();
        position = 0;
        for (String name : event.getCustomDetails().keySet()) {      // Each detail's name...
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO event_details (event_id, position, name, value) VALUES (?, ?, ?, ?)");
            insert.setString(1, event.getEventId());
            insert.setInt(2, position);
            insert.setString(3, name);
            insert.setString(4, event.getCustomDetails().get(name));  // ...and its value
            insert.executeUpdate();
            position++;
        }
    }

    // Sells seats: the Event object checks its rules and takes the seats, then the change is saved.
    // Returns true if it worked, false if sales are closed or there aren't enough seats.
    public boolean purchaseTickets(Event event, String tierName, int quantity) {
        boolean bought = event.purchase(tierName, quantity);
        if (bought) {
            updateEvent(event);                              // Save the new seat count (and SOLD_OUT, if so)
        }
        return bought;
    }

    // Archives (hides) or restores an event.
    public void setArchived(Event event, boolean archived) {
        event.setArchived(archived);                         // Change the object...
        updateEvent(event);                                  // ...and save it
    }

    // Deletes an event for good. Its tiers, details and bookings are deleted with it automatically,
    // because those tables were made with "ON DELETE CASCADE" (see Database.java).
    public void deleteEvent(Event event) {
        try {
            PreparedStatement delete = connection.prepareStatement("DELETE FROM events WHERE event_id = ?");
            delete.setString(1, event.getEventId());
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not delete the event: " + exception.getMessage());
        }
        events.remove(event);                                // Remove it from our list too
    }

    // Deletes every event owned by one host (used when a host account is deleted).
    public void deleteEventsByHost(String hostId) {
        for (Event event : getEventsByHostId(hostId)) {      // getEventsByHostId gives a separate list, so this is safe
            deleteEvent(event);
        }
    }

    // Makes a copy of an event with fresh, unsold tiers, and saves it.
    public Event duplicateEvent(Event source) {
        List<TicketTier> copiedTiers = new ArrayList<>();
        for (TicketTier tier : source.getTicketTiers()) {
            copiedTiers.add(new TicketTier(tier.getTierName(), tier.getPrice(), tier.getInitialCapacity()));
        }
        // The copy gets a new random ID like "EVT-3F9A21BC" and "(Copy)" added to its title.
        String newId = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Event copy = new Event(newId, source.getTitle() + " (Copy)", source.getDate(), source.getVenue(),
                copiedTiers, "ON_SALE", source.getHostId());
        copy.setCategory(source.getCategory());
        copy.setBannerImagePath(source.getBannerImagePath());
        copy.setCustomDetails(new LinkedHashMap<>(source.getCustomDetails()));   // A copy of the details
        addEvent(copy);
        return copy;
    }

    // ---- Sample data (only used the first time, when the database is empty) ----

    // Makes the seven sample events, all owned by the built-in host account.
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

    // Builds a sample event with three ticket tiers: Early Bird (20% of seats, 30% cheaper),
    // Regular (the rest) and VIP (15% of seats, 2.5x the price).
    private Event sampleEvent(String eventId, String title, String date, String venue, int capacity,
            double regularPrice, String category, String imagePath) {
        int earlyBirdSeats = capacity * 20 / 100;
        int vipSeats = capacity * 15 / 100;
        List<TicketTier> tiers = new ArrayList<>();
        tiers.add(new TicketTier("Early Bird", regularPrice * 0.7, earlyBirdSeats));
        tiers.add(new TicketTier("Regular", regularPrice, capacity - earlyBirdSeats - vipSeats));
        tiers.add(new TicketTier("VIP", regularPrice * 2.5, vipSeats));
        Event event = new Event(eventId, title, date, venue, tiers, "ON_SALE", User.DEFAULT_ADMIN_ID);
        event.setCategory(category);
        event.setBannerImagePath(imagePath);
        return event;
    }
}
