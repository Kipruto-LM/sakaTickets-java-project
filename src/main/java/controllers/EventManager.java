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