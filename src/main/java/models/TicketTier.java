package models;

/** A named price/capacity bucket whose available seats decrease as guests purchase tickets. */
public class TicketTier {
    // Initial capacity remains fixed so sales can be calculated after purchases.
    private String tierName;
    private double price;
    private int initialCapacity;
    private int availableSeats;

    /** Creates an empty tier for bean-style tools. */
    public TicketTier() {
    }

    /** Creates a tier whose full capacity is initially available. */
    public TicketTier(String tierName, double price, int initialCapacity) {
        this(tierName, price, initialCapacity, initialCapacity);
    }

    /** Restores a tier from saved inventory values. */
    public TicketTier(String tierName, double price, int initialCapacity, int availableSeats) {
        this.tierName = tierName;
        this.price = price;
        this.initialCapacity = initialCapacity;
        this.availableSeats = availableSeats;
    }

    // Standard accessors support the admin editor and customer checkout controls.
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getInitialCapacity() { return initialCapacity; }
    public void setInitialCapacity(int initialCapacity) { this.initialCapacity = initialCapacity; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    /** Returns false for invalid or unavailable quantities; successful purchases deduct inventory. */
    public boolean purchase(int qty) {
        if (qty <= 0 || qty > availableSeats) {
            return false;
        }
        availableSeats -= qty;
        return true;
    }
}