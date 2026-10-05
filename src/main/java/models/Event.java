package models;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 
 * The primary aggregate root for the application. It holds the schedule, 
 * host ownership, UI presentation options (images/colors), and the list of ticket tiers.
 */
public class Event {
    // Core identity and physical details
    private String eventId;
    private String title;
    private String date;
    private String venue;
    
    // Inventory and lifecycle management
    private List<TicketTier> ticketTiers = new ArrayList<>();
    private String status = "ON_SALE"; // ON_SALE, PAUSED, SOLD_OUT, CANCELLED
    private String hostId;             // Links the event to the Admin who created it
    private LocalDate salesStartDate;
    private LocalDate salesEndDate;
    
    // UI presentation metadata
    private String category = "General";
    private String bannerImagePath = "";
    private String accentColorHex = "#0D6EFD";
    private Map<String, String> customDetails = new LinkedHashMap<>();
    private boolean archived;

    /** No-argument constructor required for serialization tools. */
    public Event() {
    }

    /** 
     * Fully constructs a modern event with multiple ticket tiers and host ownership. 
     */
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

    /** 
     * Legacy constructor utilized by older versions of the app before multiple tiers were introduced.
     * It wraps the single price and capacity into a default "Regular" tier automatically.
     */
    public Event(String eventId, String title, String date, String venue, int availableSeats, double ticketPrice) {
        this(eventId, title, date, venue,
                List.of(new TicketTier("Regular", ticketPrice, availableSeats)), "ON_SALE", "");
    }

    // Standard accessors
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    /** Returns the tier list. Modifying the returned list does not affect the event safely. */
    public List<TicketTier> getTicketTiers() { return ticketTiers; }
    
    /** Sets the inventory tiers, copying the list to prevent external modification bugs. */
    public void setTicketTiers(List<TicketTier> ticketTiers) {
        this.ticketTiers = ticketTiers == null ? new ArrayList<>() : new ArrayList<>(ticketTiers);
    }

    // Metadata accessors
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public LocalDate getSalesStartDate() { return salesStartDate; }
    public void setSalesStartDate(LocalDate salesStartDate) { this.salesStartDate = salesStartDate; }
    public LocalDate getSalesEndDate() { return salesEndDate; }
    public void setSalesEndDate(LocalDate salesEndDate) { this.salesEndDate = salesEndDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category == null || category.isBlank() ? "General" : category.trim(); }
    public String getBannerImagePath() { return bannerImagePath; }
    public void setBannerImagePath(String bannerImagePath) { this.bannerImagePath = bannerImagePath == null ? "" : bannerImagePath.trim(); }
    public String getAccentColorHex() { return accentColorHex; }
    public void setAccentColorHex(String accentColorHex) { this.accentColorHex = accentColorHex; }
    
    public Map<String, String> getCustomDetails() { return new LinkedHashMap<>(customDetails); }
    public void setCustomDetails(Map<String, String> customDetails) {
        this.customDetails = customDetails == null ? new LinkedHashMap<>() : new LinkedHashMap<>(customDetails);
    }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    /** 
     * Validates if the event is actively accepting purchases on a specific day.
     * Checks both the manual status flag and the optional start/end date windows.
     */
    public boolean canPurchaseOn(LocalDate date) {
        return "ON_SALE".equals(status)
                && (salesStartDate == null || !date.isBefore(salesStartDate))
                && (salesEndDate == null || !date.isAfter(salesEndDate));
    }

    /** 
     * Locates the correct ticket tier by name and attempts to deduct inventory.
     * Automatically flips the event status to SOLD_OUT if this purchase empties the last tier.
     */
    public boolean purchase(String tierName, int quantity) {
        if (!canPurchaseOn(LocalDate.now()) || tierName == null) {
            return false;
        }
        for (TicketTier tier : ticketTiers) {
            if (tierName.equals(tier.getTierName()) && tier.purchase(quantity)) {
                if (getAvailableSeats() == 0) {
                    status = "SOLD_OUT";
                }
                return true;
            }
        }
        return false;
    }

    /** Calculates the total remaining inventory across all ticket tiers dynamically. */
    public int getAvailableSeats() {
        return ticketTiers.stream().mapToInt(TicketTier::getAvailableSeats).sum();
    }
}