package models;

/**
 * One price level for an event, such as "Regular" at 1,000 or "VIP" at 5,000.
 *
 * OOP ideas shown here:
 * - Single responsibility: this class only looks after one tier's price and seats.
 * - Encapsulation protecting data: availableSeats is private and only goes down through
 *   purchase(), which refuses bad amounts. That way the seat count can never go negative.
 */
public class TicketTier {
    private String tierName;
    private double price;
    private int initialCapacity;
    private int availableSeats;

    public TicketTier() {
    }

    // Constructor chaining: a brand-new tier starts with every seat available,
    // so this passes initialCapacity for both values to the 4-argument constructor.
    public TicketTier(String tierName, double price, int initialCapacity) {
        this(tierName, price, initialCapacity, initialCapacity);
    }

    public TicketTier(String tierName, double price, int initialCapacity, int availableSeats) {
        this.tierName = tierName;
        this.price = price;
        this.initialCapacity = initialCapacity;
        this.availableSeats = availableSeats;
    }

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
    public boolean purchase(int qty) {
        if (qty <= 0 || qty > availableSeats) {
            return false;
        }
        availableSeats -= qty;
        return true;
    }
}