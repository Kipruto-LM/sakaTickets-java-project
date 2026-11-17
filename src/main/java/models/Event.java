package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds event details, ticket tiers, and host ownership.
 *
 * OOP concepts: encapsulation is shown by the private fields and accessors;
 * composition by the TicketTier list; overloaded constructors create either a
 * full event or a simple one-tier event. The database maps it to two SQL tables.
 */
public class Event {
    private String eventId;
    private String title;
    private String date;
    private String venue;
    
    private List<TicketTier> ticketTiers = new ArrayList<>();
    private String status = "ON_SALE"; // ON_SALE, PAUSED, SOLD_OUT, CANCELLED
    private String hostId;

    // The catalog uses category for its label and artwork.
    private String category = "General";

    /** Creates an event with its ticket tiers and the ID of its host. */
    public Event(String eventId, String title, String date, String venue, List<TicketTier> ticketTiers,
            String status, String hostId) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.venue = venue;
        setTicketTiers(ticketTiers);
        this.status = status;
        this.hostId = hostId;
    }

    /** Convenience constructor for sample events that each have one Regular tier. */
    public Event(String eventId, String title, String date, String venue, int availableSeats, double ticketPrice) {
        this(eventId, title, date, venue,
                List.of(new TicketTier("Regular", ticketPrice, availableSeats)), "ON_SALE", "");
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public List<TicketTier> getTicketTiers() { return ticketTiers; }

    /** Copies the list so external changes cannot replace the event's tier collection. */
    public void setTicketTiers(List<TicketTier> ticketTiers) {
        this.ticketTiers = ticketTiers == null ? new ArrayList<>() : new ArrayList<>(ticketTiers);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category == null || category.isBlank() ? "General" : category.trim(); }

    /** Business rule: only events marked ON_SALE accept ticket purchases. */
    public boolean canPurchase() {
        return "ON_SALE".equals(status);
    }

    /** Sums the remaining seats in every ticket tier. */
    public int getAvailableSeats() {
        return ticketTiers.stream().mapToInt(TicketTier::getAvailableSeats).sum();
    }
}