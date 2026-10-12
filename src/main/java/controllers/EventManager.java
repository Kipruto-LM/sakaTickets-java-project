// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import java.util.ArrayList;              // A resizable list
import java.util.Collections;            // Helpers for lists, e.g. making a read-only view
import java.util.List;                   // The general list type
import java.util.Objects;                // Null-safe helpers such as Objects.equals()
import java.util.stream.Collectors;      // Turns a stream back into a list
import models.Event;                     // The event model
import models.TicketTier;                // The ticket tier model

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
    private final List<Event> events;   // Every event in the app, archived ones included

    // Constructor: fills the catalogue with sample events when the app starts.
    public EventManager() {
        events = new ArrayList<>();
        // sampleEvent(id, title, date, venue, total seats, Regular ticket price)
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
        // Give each sample event a category and a picture (list positions start at 0).
        assignArtwork(events.get(0), "Tech", "/images/event-tech.jpg");
        assignArtwork(events.get(1), "Nightlife", "/images/event-nightlife.jpg");
        assignArtwork(events.get(2), "Music", "/images/event-music.jpg");
        assignArtwork(events.get(3), "Food", "/images/event-food.jpg");
        assignArtwork(events.get(4), "Jazz", "/images/event-jazz.jpg");
        assignArtwork(events.get(5), "Arts & Culture", "/images/event-expo.jpg");
        assignArtwork(events.get(6), "Outdoors", "/images/event-trail.jpg");
        // All sample events belong to the built-in host account ("admin-001").
        // "event -> ..." is a lambda: a tiny method run once for each event in the list.
        events.forEach(event -> event.setHostId("admin-001"));
    }

    // Builds a sample event with three ticket tiers, so the analytics pages have tiers to compare:
    // Early Bird (20% of seats, cheaper), Regular (65%) and VIP (15%, premium price).
    private Event sampleEvent(String eventId, String title, String date, String venue, int capacity,
            double regularPrice) {
        // "(int)" turns the rounded decimal into a whole number of seats.
        int earlyBirdSeats = (int) Math.round(capacity * 0.20);
        int vipSeats = (int) Math.round(capacity * 0.15);
        // Three tier objects. Regular gets whatever seats are left, so the total always equals capacity.
        List<TicketTier> tiers = List.of(
                new TicketTier("Early Bird", roundPrice(regularPrice * 0.7), earlyBirdSeats),   // 30% cheaper
                new TicketTier("Regular", regularPrice, capacity - earlyBirdSeats - vipSeats),
                new TicketTier("VIP", roundPrice(regularPrice * 2.5), vipSeats));               // 2.5x the price
        return new Event(eventId, title, date, venue, tiers, "ON_SALE", "");
    }

    // Rounds a price to the nearest 50 shillings so sample prices look natural.
    // e.g. 1050.0 / 50 = 21 -> 21 * 50 = 1050;  1083 / 50 = 21.66 -> 22 * 50 = 1100.
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

    // Finds one event by its ID. Returns null if no event has that ID.
    public Event getEventById(String eventId) {
        for (Event event : events) {                              // Check each event in turn
            if (Objects.equals(event.getEventId(), eventId)) {   // Objects.equals copes with null safely
                return event;                                     // Found it - stop searching
            }
        }
        return null;                                              // Not found
    }

    // All events owned by one host, as a read-only list.
    // A "stream" processes a list step by step: filter keeps only the matching events,
    // then collect gathers them into a new list.
    public List<Event> getEventsByHostId(String hostId) {
        return events.stream()
                .filter(event -> Objects.equals(event.getHostId(), hostId))
                .collect(Collectors.toUnmodifiableList());
    }

    // Only events that are not archived, for the guest catalogue.
    public List<Event> getPublicEvents() {
        return events.stream().filter(event -> !event.isArchived()).collect(Collectors.toUnmodifiableList());
    }

    // Replaces a stored event with an edited version that has the same ID.
    public void updateEvent(Event updatedEvent) {
        // Stop with a clear error if no event was given.
        Objects.requireNonNull(updatedEvent, "event must not be null");
        // Walk through the list by position so we can replace the right slot.
        for (int index = 0; index < events.size(); index++) {
            if (Objects.equals(events.get(index).getEventId(), updatedEvent.getEventId())) {
                events.set(index, updatedEvent);   // Swap in the new version
                return;
            }
        }
        // Reaching here means no event had that ID.
        throw new IllegalArgumentException("Event not found: " + updatedEvent.getEventId());
    }

    // Builds a brand-new Event (and new TicketTier objects) from an existing one.
    // New objects are made instead of sharing the old ones, so changing the copy never affects the original.
    public Event duplicateEvent(String eventId, String hostId) {
        Event source = getEventById(eventId);          // The event being copied
        if (source == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        // Make a fresh TicketTier for each old tier. New tiers start with all seats available.
        List<models.TicketTier> copiedTiers = source.getTicketTiers().stream()
                .map(tier -> new models.TicketTier(tier.getTierName(), tier.getPrice(), tier.getInitialCapacity()))
                .collect(Collectors.toCollection(ArrayList::new));
        // The copy gets a new random ID like "EVT-3F9A21BC" and "(Copy)" added to its title.
        Event copy = new Event("EVT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                source.getTitle() + " (Copy)", source.getDate(), source.getVenue(), copiedTiers, "ON_SALE", hostId);
        // Copy over the remaining settings.
        copy.setSalesStartDate(source.getSalesStartDate());
        copy.setSalesEndDate(source.getSalesEndDate());
        copy.setCategory(source.getCategory());
        copy.setBannerImagePath(source.getBannerImagePath());
        copy.setAccentColorHex(source.getAccentColorHex());
        copy.setCustomDetails(source.getCustomDetails());
        addEvent(copy);                                // Store it with the others
        return copy;
    }

    // Archives (hides) or restores an event. The Event object changes its own flag.
    public void setArchived(String eventId, boolean archived) {
        Event event = getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        event.setArchived(archived);
    }

    // Adds a new event to the list. requireNonNull stops a missing (null) event sneaking in.
    public void addEvent(Event event) {
        events.add(Objects.requireNonNull(event, "event must not be null"));
    }
}
