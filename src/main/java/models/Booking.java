package models;

/**
 * A Booking is one guest's purchase, much like a receipt.
 *
 * OOP ideas shown here:
 * - Class vs object: this class is the blueprint. Every purchase creates a new Booking object.
 * - Encapsulation: all fields are private. Other classes read and change them only
 *   through the public getters and setters below.
 * - Constructor overloading: there are several constructors with different parameters,
 *   so a Booking can be built in more than one way.
 */
public class Booking {
    // Fields (attributes): the data each Booking object holds.
    private String bookingId;
    private String eventId;       // Links this booking to a specific event
    private String customerName;  // Captured during guest checkout
    private String customerEmail; // Acts as the guest's identifier for their receipt
    private String tierName;
    private int quantity;
    private double totalPrice;

    // Empty (default) constructor: creates a blank Booking that can be filled in later with setters.
    public Booking() {
    }

    // Overloaded constructor without a tier name. "this(...)" passes the work on to the
    // full constructor below, so the setup code is written only once.
    public Booking(String bookingId, String eventId, String customerName, String customerEmail, int quantity, double totalPrice) {
        this(bookingId, eventId, customerName, customerEmail, "", quantity, totalPrice);
    }

    // Full constructor. "this.field = parameter" copies each value into this object's own field.
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

    // Getters and setters: the public "doors" into the private fields (encapsulation).
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