package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import models.Event;
import models.TicketTier;

/**
 * Controller that owns the list of all events.
 *
 * OOP ideas shown here:
 * - MVC: this is a "Controller". Screens ask it for events instead of keeping their own lists,
 *   so there is a single source of truth.
 * - Encapsulation of a collection: getEvents() returns a read-only view, so other classes can
 *   look at the list but must use addEvent()/updateEvent() to change it.
 * - Objects working together: the manager creates {@link Event} objects and calls their
 *   methods (setCategory, setArchived, ...) instead of editing their data directly.
 */
public class EventManager {
    private final List<Event> events;

    // Constructor: fills the catalogue with sample events when the app starts.
    public EventManager() {
        events = new ArrayList<>();
        events.add(sampleEvent("EVT-001", "Nairobi Tech Meetup", "2026-10-15",
                "Nairobi National Museum", 120, 1500.00));
        events.add(sampleEvent("EVT-002", "Amapiano Sundays", "2026-10-18",
                "KICC Rooftop", 200, 2500.00));
        events.add(sampleEvent("EVT-003", "Acoustic Night", "2026-10-24",
                "The Alchemist", 80, 1200.00));
        events.add(sampleEvent("EVT-004", "Swahili Food Festival", "2026-11-02",
            "Uhuru Gardens", 300, 1800.00));
        events.add(sampleEvent("EVT-005", "Nairobi Jazz Evening", "2026-11-08",
            "K1 Klub House", 100, 2200.00));
        events.add(sampleEvent("EVT-006", "Coastal Creators Expo", "2026-11-14",
            "Sarit Expo Centre", 250, 1000.00));
        events.add(sampleEvent("EVT-007", "Rift Valley Trail Run", "2026-11-22",
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

    // Builds a sample event with three ticket tiers, so the analytics pages have tiers to compare:
    // Early Bird (20% of seats, cheaper), Regular (65%) and VIP (15%, premium price).
    private Event sampleEvent(String eventId, String title, String date, String venue, int capacity,
            double regularPrice) {
        int earlyBirdSeats = (int) Math.round(capacity * 0.20);
        int vipSeats = (int) Math.round(capacity * 0.15);
        List<TicketTier> tiers = List.of(
                new TicketTier("Early Bird", roundPrice(regularPrice * 0.7), earlyBirdSeats),
                new TicketTier("Regular", regularPrice, capacity - earlyBirdSeats - vipSeats),
                new TicketTier("VIP", roundPrice(regularPrice * 2.5), vipSeats));
        return new Event(eventId, title, date, venue, tiers, "ON_SALE", "");
    }

    // Rounds a price to the nearest 50 shillings so sample prices look natural.
    private static double roundPrice(double price) {
        return Math.round(price / 50.0) * 50.0;
    }

    // Private helper: only used inside this class while setting up the sample events.
    private void assignArtwork(Event event, String category, String imagePath) {
        event.setCategory(category);
        event.setBannerImagePath(imagePath);
    }

    // Read-only view: callers can't add or remove events through this list.
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

    public List<Event> getEventsByHostId(String hostId) {
        return events.stream()
                .filter(event -> Objects.equals(event.getHostId(), hostId))
                .collect(Collectors.toUnmodifiableList());
    }

    // Only events that are not archived, for the guest catalogue.
    public List<Event> getPublicEvents() {
        return events.stream().filter(event -> !event.isArchived()).collect(Collectors.toUnmodifiableList());
    }

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

    // Builds a brand-new Event (and new TicketTier objects) from an existing one.
    // New objects are made instead of sharing the old ones, so changing the copy never affects the original.
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