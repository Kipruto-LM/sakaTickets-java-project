package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import models.Event;

/**
 * Controller/repository for the event catalog. It creates and updates Event
 * objects, applies catalog rules, and supplies data to the Swing views.
 */
public class EventManager {
    // The master list of all events in the system
    private final List<Event> events;

    /**
     * Initializes the manager and seeds it with default events spanning various categories.
     * All default events are assigned to the primary demo host ID ('admin-001').
     */
    public EventManager() {
        events = new ArrayList<>();
        events.add(new Event("EVT-001", "Nairobi Tech Meetup", "2026-10-15", "Nairobi National Museum", 120, 1500.00));
        events.add(new Event("EVT-002", "Amapiano Sundays", "2026-10-18", "KICC Rooftop", 200, 2500.00));
        events.add(new Event("EVT-003", "Acoustic Night", "2026-10-24", "The Alchemist", 80, 1200.00));
        
        // Seed categories and default image paths
        assignArtwork(events.get(0), "Tech", "/images/event-tech.jpg");
        assignArtwork(events.get(1), "Nightlife", "/images/event-nightlife.jpg");
        assignArtwork(events.get(2), "Music", "/images/event-music.jpg");
        
        // Assign ownership to the default admin account created in AccountManager
        events.forEach(event -> event.setHostId("admin-001"));
    }

    /** Helper method to quickly configure the visual styling of seeded events. */
    private void assignArtwork(Event event, String category, String imagePath) {
        event.setCategory(category);
        event.setBannerImagePath(imagePath);
    }

    /** Returns an unmodifiable snapshot of the catalog to prevent UI code from breaking the list. */
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** Locates a specific event by its ID. Returns null if not found. */
    public Event getEventById(String eventId) {
        for (Event event : events) {
            if (Objects.equals(event.getEventId(), eventId)) {
                return event;
            }
        }
        return null;
    }

    /** 
     * Filters the catalog to return only the events created by a specific host.
     * Used exclusively by the AdminDashboardPanel.
     */
    public List<Event> getEventsByHostId(String hostId) {
        return events.stream()
                .filter(event -> Objects.equals(event.getHostId(), hostId))
                .collect(Collectors.toUnmodifiableList());
    }

    /** 
     * Filters out archived events so they do not appear on the guest-facing EventCatalogPanel. 
     */
    public List<Event> getPublicEvents() {
        return events.stream().filter(event -> !event.isArchived()).collect(Collectors.toUnmodifiableList());
    }

    /** 
     * Replaces an existing event with an updated version after an admin edits it.
     */
    public void updateEvent(Event updatedEvent) {
        Objects.requireNonNull(updatedEvent, "event must not be null");
        for (int index = 0; index < events.size(); index++) {
            if (Objects.equals(events.get(index).getEventId(), updatedEvent.getEventId())) {
                events.set(index, updatedEvent);
                return;
            }
        }
        throw new IllegalArgumentException("Event not found: " + updatedEvent.getEventId());
    }

    /** 
     * Creates a fresh copy of an existing event, resetting inventory and appending "(Copy)" 
     * to the title. Useful for recurring weekly events.
     */
    public Event duplicateEvent(String eventId, String hostId) {
        Event source = getEventById(eventId);
        if (source == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        
        // Deep copy the ticket tiers so the new event has full, fresh inventory
        List<models.TicketTier> copiedTiers = source.getTicketTiers().stream()
                .map(tier -> new models.TicketTier(tier.getTierName(), tier.getPrice(), tier.getInitialCapacity()))
                .collect(Collectors.toCollection(ArrayList::new));
                
        Event copy = new Event("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                source.getTitle() + " (Copy)", source.getDate(), source.getVenue(), copiedTiers, "ON_SALE", hostId);
                
        // Copy metadata
        copy.setCategory(source.getCategory());
        copy.setBannerImagePath(source.getBannerImagePath());
        copy.setAccentColorHex(source.getAccentColorHex());
        copy.setCustomDetails(source.getCustomDetails());
        
        addEvent(copy);
        return copy;
    }

    /** Soft-deletes an event by hiding it from the public catalog while keeping history intact. */
    public void setArchived(String eventId, boolean archived) {
        Event event = getEventById(eventId);
        if (event != null) {
            event.setArchived(archived);
        }
    }

    /** Adds a newly created event to the master catalog. */
    public void addEvent(Event event) {
        events.add(Objects.requireNonNull(event, "event must not be null"));
    }
}