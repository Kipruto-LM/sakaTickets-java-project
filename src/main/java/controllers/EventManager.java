package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    public void addEvent(Event event) {
        events.add(Objects.requireNonNull(event, "event must not be null"));
    }
}