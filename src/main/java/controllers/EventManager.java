package controllers;

import database.Database;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import models.Event;

/**
 * Controller/repository for the event catalog. It loads and saves Event objects
 * through SQLite, then supplies them to the Swing views.
 *
 * Core concepts:
 * - Encapsulation: the private event list is changed only through manager methods.
 * - Abstraction: views use these operations without knowing the SQL details.
 * - Composition: the manager owns and coordinates a collection of Event objects.
 */
public class EventManager {
    // The master list of all events in the system
    private final List<Event> events;

    /** Loads saved events, or inserts sample events the first time the app runs. */
    public EventManager() throws SQLException {
        Database.initialize();
        events = new ArrayList<>();
        events.addAll(Database.loadEvents());

        if (events.isEmpty()) {
            seedSampleEvents();
        }
    }

    /** Creates starter data only when the database has no events yet. */
    private void seedSampleEvents() throws SQLException {
        events.add(new Event("EVT-001", "Nairobi Tech Meetup", "2026-10-15", "Nairobi National Museum", 120, 1500.00));
        events.add(new Event("EVT-002", "Amapiano Sundays", "2026-10-18", "KICC Rooftop", 200, 2500.00));
        events.add(new Event("EVT-003", "Acoustic Night", "2026-10-24", "The Alchemist", 80, 1200.00));
        
        // Categories also tell the catalog which bundled image to display.
        assignCategory(events.get(0), "Tech");
        assignCategory(events.get(1), "Nightlife");
        assignCategory(events.get(2), "Music");
        
        // Assign ownership to the default admin account created in AccountManager
        events.forEach(event -> event.setHostId("admin-001"));
        Database.saveEvents(events);
    }

    /** Sets the display category for one of the sample events. */
    /** Assigns a sample event's category, which also determines its catalog artwork. */
    private void assignCategory(Event event, String category) {
        event.setCategory(category);
    }

    /** Returns a read-only view so screens cannot replace the catalog list directly. */
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** Locates a specific event by its ID. Returns null if not found. */
    /**
     * Searches the in-memory catalog by its unique identifier.
     *
     * @param eventId identifier to find
     * @return the matching event, or null when the catalog has no such event
     */
    public Event getEventById(String eventId) {
        for (Event event : events) {
            if (Objects.equals(event.getEventId(), eventId)) {
                return event;
            }
        }
        return null;
    }

    /** Finds the events owned by one host for their dashboard. */
    /**
     * Creates a read-only list containing only events owned by the given host.
     *
     * @param hostId owner identifier to match
     * @return the host's events in catalog order
     */
    public List<Event> getEventsByHostId(String hostId) {
        List<Event> hostEvents = new ArrayList<>();
        for (Event event : events) {
            if (Objects.equals(event.getHostId(), hostId)) {
                hostEvents.add(event);
            }
        }
        return Collections.unmodifiableList(hostEvents);
    }

    /** Replaces an existing event in memory and SQLite after its owner edits it. */
    /**
     * Persists an edited event before replacing the matching in-memory object.
     *
     * @param updatedEvent changed event data
     * @throws IllegalArgumentException if the event ID is not in this catalog
     * @throws SQLException if SQLite cannot save the update
     */
    public void updateEvent(Event updatedEvent) throws SQLException {
        Objects.requireNonNull(updatedEvent, "event must not be null");
        for (int index = 0; index < events.size(); index++) {
            if (Objects.equals(events.get(index).getEventId(), updatedEvent.getEventId())) {
                Database.saveEvent(updatedEvent);
                events.set(index, updatedEvent);
                return;
            }
        }
        throw new IllegalArgumentException("Event not found: " + updatedEvent.getEventId());
    }

    /** Adds a new event to SQLite and then makes it available to the screens. */
    /**
     * Persists a new event before adding it to the shared in-memory catalog.
     *
     * @param event event to store
     * @throws SQLException if SQLite cannot save the event
     */
    public void addEvent(Event event) throws SQLException {
        Event checkedEvent = Objects.requireNonNull(event, "event must not be null");
        Database.saveEvent(checkedEvent);
        events.add(checkedEvent);
    }
}