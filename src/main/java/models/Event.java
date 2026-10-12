// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

import java.util.ArrayList;      // A resizable list
import java.util.LinkedHashMap;  // A map (key -> value) that remembers the order items were added
import java.util.List;           // The general "list" type
import java.util.Map;            // The general "map" type

/**
 * An Event is something guests can buy tickets for, such as a concert or an expo.
 *
 * OOP ideas shown here:
 * - Composition ("has-a"): an Event HAS a list of {@link TicketTier} objects. It does not
 *   track seats itself. Each tier looks after its own seats.
 * - Encapsulation: fields are private, and some setters clean the value first
 *   (for example, setCategory uses "General" when the input is empty).
 * - Behaviour inside the object: purchase() and canPurchase() keep the sales rules
 *   with the data they apply to, so no other class has to repeat them.
 */
public class Event {
    // Fields: the state of one event object.
    private String eventId;          // Unique ID such as "EVT-001"
    private String title;            // Name shown to guests
    private String date;             // Event date as text in "yyyy-mm-dd" form
    private String venue;            // Where it happens
    // Composition: the event is made up of ticket tier objects (Regular, VIP, ...).
    private List<TicketTier> ticketTiers = new ArrayList<>();
    private String status = "ON_SALE";        // ON_SALE, PAUSED, SOLD_OUT or CANCELLED
    private String hostId;                    // ID of the host (User) who owns this event
    private String category = "General";      // e.g. "Music", "Tech"
    private String bannerImagePath = "";      // Picture file shown on the event's cards
    // Extra facts the host adds, as name -> value pairs, e.g. "Dress code" -> "Smart casual".
    private Map<String, String> customDetails = new LinkedHashMap<>();
    private boolean archived;                 // true = hidden from guests (booleans start as false)

    // Default constructor: an empty event that is filled in through setters.
    public Event() {
    }

    // Main constructor: builds a complete event in one step.
    public Event(String eventId, String title, String date, String venue, List<TicketTier> ticketTiers,
            String status, String hostId) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.venue = venue;
        this.ticketTiers = ticketTiers;
        this.status = status;
        this.hostId = hostId;
    }

    // Getters and setters (encapsulation): the only way other classes reach the fields.
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public List<TicketTier> getTicketTiers() { return ticketTiers; }
    public void setTicketTiers(List<TicketTier> ticketTiers) { this.ticketTiers = ticketTiers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public String getBannerImagePath() { return bannerImagePath; }
    public void setBannerImagePath(String bannerImagePath) { this.bannerImagePath = bannerImagePath; }
    public Map<String, String> getCustomDetails() { return customDetails; }
    public void setCustomDetails(Map<String, String> customDetails) { this.customDetails = customDetails; }
    // Boolean getters are usually called "isSomething" instead of "getSomething".
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public String getCategory() { return category; }

    // A setter with a check: an empty category becomes "General".
    public void setCategory(String category) {
        if (category == null || category.isBlank()) {
            this.category = "General";
        } else {
            this.category = category.trim();
        }
    }

    // Business rule kept inside the object: tickets can only be bought while the event is ON_SALE.
    public boolean canPurchase() {
        return status.equals("ON_SALE");
    }

    // Delegation: the event finds the right tier and lets that TicketTier object take the seats.
    // When every tier is empty, the event marks itself SOLD_OUT.
    // Returns true if the purchase worked, false if it was refused.
    public boolean purchase(String tierName, int quantity) {
        if (!canPurchase()) {
            return false;                            // Sales are closed
        }
        for (TicketTier tier : ticketTiers) {
            if (tier.getTierName().equals(tierName)) {   // Found the right tier
                boolean bought = tier.purchase(quantity);    // The tier checks it has enough seats
                if (bought && getAvailableSeats() == 0) {
                    status = "SOLD_OUT";                     // That was the very last seat
                }
                return bought;
            }
        }
        return false;                                // No tier with that name
    }

    // Derived value: worked out from the tiers each time it is asked for, rather than stored.
    // Adds up the available seats of every tier.
    public int getAvailableSeats() {
        int total = 0;
        for (TicketTier tier : ticketTiers) {
            total += tier.getAvailableSeats();
        }
        return total;
    }

    // Total seats the event started with (all tiers added up).
    public int getCapacity() {
        int total = 0;
        for (TicketTier tier : ticketTiers) {
            total += tier.getInitialCapacity();
        }
        return total;
    }

    // The cheapest price of a tier that still has seats (or of any tier, if all are sold out).
    public double getLowestPrice() {
        double lowest = -1;                          // -1 means "nothing found yet"
        for (TicketTier tier : ticketTiers) {
            if (tier.getAvailableSeats() > 0 && (lowest < 0 || tier.getPrice() < lowest)) {
                lowest = tier.getPrice();
            }
        }
        if (lowest < 0) {                            // Every tier is sold out: use the cheapest anyway
            for (TicketTier tier : ticketTiers) {
                if (lowest < 0 || tier.getPrice() < lowest) {
                    lowest = tier.getPrice();
                }
            }
        }
        return Math.max(lowest, 0);                  // 0 if the event has no tiers at all
    }
}
