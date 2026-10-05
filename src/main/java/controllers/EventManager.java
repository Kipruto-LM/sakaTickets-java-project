package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import models.Event;

/** Owns the in-memory event catalog and host-scoped event lifecycle operations. */
public class EventManager {
    // MVP event store; demo entries are assigned tailored artwork and the seeded host ID.
    private final List<Event> events;

    public EventManager() {
        events = new ArrayList<>();
        events.add(new Event("EVT-001", "Nairobi Tech Meetup", "2026-10-15",
                "Nairobi National Museum", 120, 1500.00));
        events.add(new Event("EVT-002", "Amapiano Sundays", "2026-10-18",
                "KICC Rooftop", 200, 2500.00));
        events.add(new Event("EVT-003", "Acoustic Night", "2026-10-24",
                "The Alchemist", 80, 1200.00));
        events.add(new Event("EVT-004", "Swahili Food Festival", "2026-11-02",
            "Uhuru Gardens", 300, 1800.00));
        events.add(new Event("EVT-005", "Nairobi Jazz Evening", "2026-11-08",
            "K1 Klub House", 100, 2200.00));
        events.add(new Event("EVT-006", "Coastal Creators Expo", "2026-11-14",
            "Sarit Expo Centre", 250, 1000.00));
        events.add(new Event("EVT-007", "Rift Valley Trail Run", "2026-11-22",
            "Hell's Gate National Park", 180, 750.00));
        assignArtwork(events.get(0), "Tech", "/images/event-tech.jpg");
        assignArtwork(events.get(1), "Nightlife", "/images/event-nightlife.jpg");
        assignArtwork(events.get(2), "Music", "/images/event-music.jpg");
        assignArtwork(events.get(3), "Food", "/images/event-food.jpg");
        assignArtwork(events.get(4), "Jazz", "/images/event-jazz.jpg");
        assignArtwork(events.get(5), "Arts & Culture", "/images/event-expo.jpg");
        assignArtwork(events.get(6), "Outdoors", "/images/event-trail.jpg");
        events.forEach(event -> event.setHostId("admin-001"));
    }

    /** Applies a category and bundled image path to one seeded event. */
    private void assignArtwork(Event event, String category, String imagePath) {
        event.setCategory(category);
        event.setBannerImagePath(imagePath);
    }

    // Query methods return snapshots where callers should not mutate the catalog directly.
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public Event getEventById(String eventId) {
        for (Event event : events) {
            if (Objects.equals(event.getEventId(), eventId)) {
                return event;
            }
        }
        return null;
    }

    /** Returns the events owned by one authenticated host. */
    public List<Event> getEventsByHostId(String hostId) {
        return events.stream()
                .filter(event -> Objects.equals(event.getHostId(), hostId))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Hides archived events from guest-facing pages while preserving them for hosts. */
    public List<Event> getPublicEvents() {
        return events.stream().filter(event -> !event.isArchived()).collect(Collectors.toUnmodifiableList());
    }

    /** Replaces an existing event by ID, rejecting updates for unknown events. */
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

    /** Copies an event's setup and styling into a fresh, unsold event owned by the host. */
    public Event duplicateEvent(String eventId, String hostId) {
        Event source = getEventById(eventId);
        if (source == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        List<models.TicketTier> copiedTiers = source.getTicketTiers().stream()
                .map(tier -> new models.TicketTier(tier.getTierName(), tier.getPrice(), tier.getInitialCapacity()))
                .collect(Collectors.toCollection(ArrayList::new));
        Event copy = new Event("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                source.getTitle() + " (Copy)", source.getDate(), source.getVenue(), copiedTiers, "ON_SALE", hostId);
        copy.setSalesStartDate(source.getSalesStartDate());
        copy.setSalesEndDate(source.getSalesEndDate());
        copy.setCategory(source.getCategory());
        copy.setBannerImagePath(source.getBannerImagePath());
        copy.setAccentColorHex(source.getAccentColorHex());
        copy.setCustomDetails(source.getCustomDetails());
        addEvent(copy);
        return copy;
    }

    /** Toggles catalog visibility without deleting sales history. */
    public void setArchived(String eventId, boolean archived) {
        Event event = getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        event.setArchived(archived);
    }

    public void addEvent(Event event) {
        events.add(Objects.requireNonNull(event, "event must not be null"));
    }
}