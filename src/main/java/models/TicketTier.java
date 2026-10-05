package models;

/** 
 * Represents a specific pricing tier within an event (e.g., "VIP", "General Admission").
 * This class tracks both the starting capacity and the remaining available seats,
 * allowing the system to calculate how many tickets have been sold over time.
 */
public class TicketTier {
    private String tierName;
    private double price;
    
    // We keep the initial capacity fixed so we can calculate (initial - available) to find total sales.
    private int initialCapacity;
    private int availableSeats;

    /** 
     * Default no-argument constructor required for standard Java object serialization 
     * (e.g., saving to JSON). 
     */
    public TicketTier() {
    }

    /** 
     * Creates a new ticket tier where the available seats automatically match the starting capacity.
     * 
     * @param tierName        The display name of the tier.
     * @param price           The cost of a single ticket.
     * @param initialCapacity The maximum number of tickets allowed to be sold.
     */
    public TicketTier(String tierName, double price, int initialCapacity) {
        this(tierName, price, initialCapacity, initialCapacity);
    }

    /** 
     * Internal constructor used when restoring a previously saved tier from a database or file,
     * where the available seats might already be lower than the initial capacity.
     */
    public TicketTier(String tierName, double price, int initialCapacity, int availableSeats) {
        this.tierName = tierName;
        this.price = price;
        this.initialCapacity = initialCapacity;
        this.availableSeats = availableSeats;
    }

    // Standard getters and setters for the UI and serialization tools
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getInitialCapacity() { return initialCapacity; }
    public void setInitialCapacity(int initialCapacity) { this.initialCapacity = initialCapacity; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    /** 
     * Safely deducts inventory when a guest buys a ticket.
     * 
     * @param qty The number of tickets requested.
     * @return true if the purchase was successful, false if there isn't enough inventory.
     */
    public boolean purchase(int qty) {
        if (qty <= 0 || qty > availableSeats) {
            return false; // Prevent overselling or negative quantities
        }
        availableSeats -= qty;
        return true;
    }
}