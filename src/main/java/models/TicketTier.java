// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

/**
 * One price level for an event, such as "Regular" at 1,000 or "VIP" at 5,000.
 * An Event holds a list of these (see Event.ticketTiers).
 *
 * OOP ideas shown here:
 * - Single responsibility: this class only looks after one tier's price and seats.
 * - Encapsulation protecting data: availableSeats is private and only goes down through
 *   purchase(), which refuses bad amounts. That way the seat count can never go negative.
 */
public class TicketTier {
    private String tierName;        // Name shown to guests, e.g. "Early Bird"
    private double price;           // Price of ONE ticket in this tier, in KES
    private int initialCapacity;    // How many seats the tier started with (never changes when selling)
    private int availableSeats;     // How many seats are still unsold (goes down with each sale)

    // Empty constructor: a blank tier to be filled in with setters.
    public TicketTier() {
    }

    // Constructor chaining: a brand-new tier starts with every seat available,
    // so this passes initialCapacity for both values to the 4-argument constructor.
    public TicketTier(String tierName, double price, int initialCapacity) {
        this(tierName, price, initialCapacity, initialCapacity);
    }

    // Full constructor: used when copying a tier that has already sold some seats.
    public TicketTier(String tierName, double price, int initialCapacity, int availableSeats) {
        this.tierName = tierName;
        this.price = price;
        this.initialCapacity = initialCapacity;
        this.availableSeats = availableSeats;
    }

    // One-line getters and setters for each field (encapsulation).
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getInitialCapacity() { return initialCapacity; }
    public void setInitialCapacity(int initialCapacity) { this.initialCapacity = initialCapacity; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    // The object checks its own rules before changing its state: it returns false
    // instead of selling more seats than it has.
    // Returns true if the seats were taken, false if the sale was refused.
    public boolean purchase(int qty) {
        // Refuse zero/negative amounts, and amounts bigger than the seats left.
        if (qty <= 0 || qty > availableSeats) {
            return false;
        }
        availableSeats -= qty;   // Same as: availableSeats = availableSeats - qty;
        return true;
    }
}
