package models;

public class TicketTier {
    private String tierName;
    private double price;
    private int initialCapacity;
    private int availableSeats;

    public TicketTier() {
    }

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

    public boolean purchase(int qty) {
        if (qty <= 0 || qty > availableSeats) {
            return false;
        }
        availableSeats -= qty;
        return true;
    }
}