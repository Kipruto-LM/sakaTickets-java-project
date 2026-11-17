package models;

/** 
 * Represents a finalized transaction receipt. 
 * Stores the immutable record of a guest purchasing a specific quantity 
 * of a specific ticket tier for an event.
 *
 * Core concepts:
 * - Encapsulation: receipt fields are private and accessed through methods.
 * - Modeling: one Booking object groups related customer, event, and price data.
 * - Object references by ID: eventId links this receipt to its Event record.
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
    /** Returns the unique booking reference displayed on the receipt. */
    public String getBookingId() { return bookingId; }
    /** Changes the booking reference, mainly for data restoration. */
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    /** Returns the ID of the event that was booked. */
    public String getEventId() { return eventId; }
    /** Sets the ID of the event associated with this booking. */
    public void setEventId(String eventId) { this.eventId = eventId; }
    /** Returns the guest name saved on the receipt. */
    public String getCustomerName() { return customerName; }
    /** Sets the guest name saved on the receipt. */
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    /** Returns the guest email saved on the receipt. */
    public String getCustomerEmail() { return customerEmail; }
    /** Sets the guest email saved on the receipt. */
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    /** Returns the purchased ticket tier's display name. */
    public String getTierName() { return tierName; }
    /** Sets the purchased ticket tier's display name. */
    public void setTierName(String tierName) { this.tierName = tierName; }
    /** Returns how many tickets the guest purchased. */
    public int getQuantity() { return quantity; }
    /** Sets the number of tickets recorded by this booking. */
    public void setQuantity(int quantity) { this.quantity = quantity; }
    /** Returns the total charge for this booking. */
    public double getTotalPrice() { return totalPrice; }
    /** Sets the total charge recorded by this booking. */
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}