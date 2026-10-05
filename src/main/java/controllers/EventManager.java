package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import models.Event;

public class EventManager {
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
        events.forEach(event -> event.setHostId("admin-001"));
    }

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