package models;

/** Mutable booking/receipt data for a guest's event, tier, quantity, and charge. */
public class Booking {
    // Receipt identity and event/customer relationship.
    private String bookingId;
    private String eventId;       // Links this booking to a specific event
    private String customerName;  // Captured during guest checkout
    private String customerEmail; // Acts as the guest's identifier for their receipt
    private String tierName;
    // Purchased inventory and charged total.
    private int quantity;
    private double totalPrice;

    /** No-argument constructor for bean-style tools and serialization. */
    public Booking() {
    }

    /** Compatibility constructor for bookings created before ticket tiers were introduced. */
    public Booking(String bookingId, String eventId, String customerName, String customerEmail, int quantity, double totalPrice) {
        this(bookingId, eventId, customerName, customerEmail, "", quantity, totalPrice);
    }

    /** Creates a receipt with the purchased ticket tier recorded explicitly. */
    public Booking(String bookingId, String eventId, String customerName, String customerEmail, String tierName,
            int quantity, double totalPrice) {
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.tierName = tierName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    // Standard bean accessors keep bookings easy to display and persist.
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}