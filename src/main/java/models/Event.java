package models;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Event {
    private String eventId;
    private String title;
    private String date;
    private String venue;
    private List<TicketTier> ticketTiers = new ArrayList<>();
    private String status = "ON_SALE";
    private String hostId;
    private LocalDate salesStartDate;
    private LocalDate salesEndDate;
    private String category = "General";
    private String bannerImagePath = "";
    private String accentColorHex = "#0D6EFD";
    private Map<String, String> customDetails = new LinkedHashMap<>();
    private boolean archived;

    public Event() {
    }

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

    public Event(String eventId, String title, String date, String venue, int availableSeats, double ticketPrice) {
        this(eventId, title, date, venue,
                List.of(new TicketTier("Regular", ticketPrice, availableSeats)), "ON_SALE", "");
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public List<TicketTier> getTicketTiers() {
        return ticketTiers;
    }

    public void setTicketTiers(List<TicketTier> ticketTiers) {
        this.ticketTiers = ticketTiers == null ? new ArrayList<>() : new ArrayList<>(ticketTiers);
    }

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

    public boolean canPurchaseOn(LocalDate date) {
        return "ON_SALE".equals(status)
                && (salesStartDate == null || !date.isBefore(salesStartDate))
                && (salesEndDate == null || !date.isAfter(salesEndDate));
    }

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

    public int getAvailableSeats() {
        return ticketTiers.stream().mapToInt(TicketTier::getAvailableSeats).sum();
    }

    public double getTicketPrice() {
        return ticketTiers.isEmpty() ? 0.0 : ticketTiers.get(0).getPrice();
    }

    public boolean purchaseTickets(int amount) {
        return !ticketTiers.isEmpty() && purchase(ticketTiers.get(0).getTierName(), amount);
    }
}
