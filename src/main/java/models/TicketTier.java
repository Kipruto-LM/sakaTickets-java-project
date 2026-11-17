package models;

/** 
 * Represents a specific pricing tier within an event (e.g., "VIP", "General Admission").
 * This class tracks both the starting capacity and the remaining available seats,
 * allowing the system to calculate how many tickets have been sold over time.
 *
 * Core concepts:
 * - Encapsulation: the tier's price and inventory are private fields.
 * - Composition: Event contains one or more TicketTier objects.
 * - Constructor overloading: constructors create new tiers or restore saved inventory.
 * - Data mapping: Database stores the tier fields in the ticket_tiers table.
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

    // Accessors let the UI and database layer read or update private tier data.
    /** Returns the tier label, such as Regular or VIP. */
    public String getTierName() { return tierName; }
    /** Sets the tier label. */
    public void setTierName(String tierName) { this.tierName = tierName; }
    /** Returns the price of one ticket in this tier. */
    public double getPrice() { return price; }
    /** Sets the price of one ticket in this tier. */
    public void setPrice(double price) { this.price = price; }
    /** Returns the original number of seats offered in this tier. */
    public int getInitialCapacity() { return initialCapacity; }
    /** Sets the original capacity used to describe this tier. */
    public void setInitialCapacity(int initialCapacity) { this.initialCapacity = initialCapacity; }
    /** Returns the seats that are still available to purchase. */
    public int getAvailableSeats() { return availableSeats; }
    /** Updates the remaining inventory returned by SQLite after a purchase. */
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

}