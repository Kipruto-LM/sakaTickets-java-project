package models;

/** 
 * Represents a finalized transaction receipt. 
 * Stores the immutable record of a guest purchasing a specific quantity 
 * of a specific ticket tier for an event.
 */
public class Booking {
    // Unique receipt identifier (UUID)
    private String bookingId;
    
    // Relational links
    private String eventId;       
    private String customerName;  
    private String customerEmail; 
    private String tierName;
    
    // Purchase metrics
    private int quantity;
    private double totalPrice;

    /** No-argument constructor required for serialization tools. */
    public Booking() {
    }

    /** 
     * Creates a comprehensive receipt capturing exactly what the guest bought.
     */
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

    // Standard getters and setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}