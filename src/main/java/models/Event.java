// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

import java.util.ArrayList;      // A resizable list
import java.time.LocalDate;      // A calendar date without a time, e.g. 2026-10-15
import java.util.LinkedHashMap;  // A map (key -> value) that remembers the order items were added
import java.util.List;           // The general "list" type
import java.util.Map;            // The general "map" type

/**
 * An Event is something guests can buy tickets for, such as a concert or an expo.
 *
 * OOP ideas shown here:
 * - Composition ("has-a"): an Event HAS a list of {@link TicketTier} objects. It does not
 *   track seats itself. Each tier looks after its own seats.
 * - Encapsulation: fields are private, and some setters check or clean the value first
 *   (for example, setCategory falls back to "General" when the input is empty).
 * - Behaviour inside the object: purchase() and canPurchaseOn() keep the sales rules
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
    private LocalDate salesStartDate;         // First day tickets can be bought (null = no limit)
    private LocalDate salesEndDate;           // Last day tickets can be bought (null = no limit)
    private String category = "General";      // e.g. "Music", "Tech"
    private String bannerImagePath = "";      // Picture file shown on the event's cards
    private String accentColorHex = "#0D6EFD"; // The host's chosen highlight colour
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
        setTicketTiers(ticketTiers);   // Uses the setter so the list gets copied safely
        this.status = status;
        this.hostId = hostId;
    }

    // Getters and setters (encapsulation): the only way other classes reach the fields.

    // Returns the event's ID.
    public String getEventId() {
        return eventId;
    }

    // Changes the event's ID.
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // Returns the event's title.
    public String getTitle() {
        return title;
    }

    // Changes the event's title.
    public void setTitle(String title) {
        this.title = title;
    }

    // Returns the event date (as text).
    public String getDate() {
        return date;
    }

    // Changes the event date.
    public void setDate(String date) {
        this.date = date;
    }

    // Returns the venue.
    public String getVenue() {
        return venue;
    }

    // Changes the venue.
    public void setVenue(String venue) {
        this.venue = venue;
    }

    // Returns the list of ticket tiers.
    public List<TicketTier> getTicketTiers() {
        return ticketTiers;
    }

    // Copies the list it is given, so later changes to the caller's list don't affect this event.
    // "condition ? a : b" is the ternary operator: if the condition is true use a, otherwise b.
    // So: if no list was given (null), start with an empty one; otherwise copy the given list.
    public void setTicketTiers(List<TicketTier> ticketTiers) {
        this.ticketTiers = ticketTiers == null ? new ArrayList<>() : new ArrayList<>(ticketTiers);
    }

    // Short one-line getters and setters.
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public LocalDate getSalesStartDate() { return salesStartDate; }
    public void setSalesStartDate(LocalDate salesStartDate) { this.salesStartDate = salesStartDate; }
    public LocalDate getSalesEndDate() { return salesEndDate; }
    public void setSalesEndDate(LocalDate salesEndDate) { this.salesEndDate = salesEndDate; }
    public String getCategory() { return category; }
    // A setter with a check: empty or missing input becomes "General"; otherwise extra spaces are trimmed.
    public void setCategory(String category) { this.category = category == null || category.isBlank() ? "General" : category.trim(); }
    public String getBannerImagePath() { return bannerImagePath; }
    // Missing input becomes "" (no picture); otherwise extra spaces are trimmed.
    public void setBannerImagePath(String bannerImagePath) { this.bannerImagePath = bannerImagePath == null ? "" : bannerImagePath.trim(); }
    public String getAccentColorHex() { return accentColorHex; }
    public void setAccentColorHex(String accentColorHex) { this.accentColorHex = accentColorHex; }
    // Returns a copy, so callers can read the details but can't change this event's map directly.
    public Map<String, String> getCustomDetails() { return new LinkedHashMap<>(customDetails); }
    // Stores a copy of the given map (or an empty map if none was given).
    public void setCustomDetails(Map<String, String> customDetails) {
        this.customDetails = customDetails == null ? new LinkedHashMap<>() : new LinkedHashMap<>(customDetails);
    }
    // Boolean getters are usually called "isSomething" instead of "getSomething".
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    // Business rule kept inside the object: tickets can be bought only while the event is
    // ON_SALE and the date falls inside the sales window.
    // All three conditions must be true (&& means "and").
    public boolean canPurchaseOn(LocalDate date) {
        return "ON_SALE".equals(status)                                          // 1. Event is on sale
                && (salesStartDate == null || !date.isBefore(salesStartDate))    // 2. Sales have started
                && (salesEndDate == null || !date.isAfter(salesEndDate));        // 3. Sales haven't ended
    }

    // Delegation: the event finds the right tier and lets that TicketTier object take the seats.
    // When every tier is empty, the event marks itself SOLD_OUT.
    // Returns true if the purchase worked, false if it was refused.
    public boolean purchase(String tierName, int quantity) {
        // Stop straight away if sales are closed today or no tier was chosen.
        if (!canPurchaseOn(LocalDate.now()) || tierName == null) {
            return false;
        }
        // Look through each tier for the one with the matching name.
        for (TicketTier tier : ticketTiers) {
            // tier.purchase(quantity) only runs if the name matched (Java stops at the first false in &&).
            if (tierName.equals(tier.getTierName()) && tier.purchase(quantity)) {
                // If that sale used up the very last seat of the whole event, mark it sold out.
                if (getAvailableSeats() == 0) {
                    status = "SOLD_OUT";
                }
                return true;
            }
        }
        return false;   // No matching tier, or that tier didn't have enough seats
    }

    // Derived value: worked out from the tiers each time it is asked for, rather than stored.
    // Adds up the available seats of every tier.
    public int getAvailableSeats() {
        return ticketTiers.stream().mapToInt(TicketTier::getAvailableSeats).sum();
    }
}
