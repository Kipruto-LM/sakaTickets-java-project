// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

// LocalDateTime stores a date and a time together, e.g. 2026-10-12 19:35.
import java.time.LocalDateTime;

/**
 * A Booking is one guest's purchase, much like a receipt.
 * Example: "Wanjiru bought 2 Regular tickets to Acoustic Night for KES 2,400".
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
    // "private" means only code inside this class can touch them directly.
    private String bookingId;     // Short receipt code shown to the guest, e.g. "7FE84F78"
    private String eventId;       // Links this booking to a specific event
    private String customerName;  // Captured during guest checkout
    private String customerEmail; // Acts as the guest's identifier for their receipt
    private String tierName;      // Which ticket type was bought, e.g. "VIP"
    private int quantity;         // How many tickets were bought in this booking
    private double totalPrice;    // Price per ticket x quantity, in Kenyan shillings
    // When the guest bought the tickets. Used for the sales-over-time charts.
    // It starts as "right now", so a new booking is stamped with the moment it was made.
    private LocalDateTime purchasedAt = LocalDateTime.now();

    // Empty (default) constructor: creates a blank Booking that can be filled in later with setters.
    public Booking() {
    }

    // Overloaded constructor without a tier name. "this(...)" passes the work on to the
    // full constructor below (with "" as the tier name), so the setup code is written only once.
    public Booking(String bookingId, String eventId, String customerName, String customerEmail, int quantity, double totalPrice) {
        this(bookingId, eventId, customerName, customerEmail, "", quantity, totalPrice);
    }

    // Full constructor. "this.field = parameter" copies each value into this object's own field.
    // "this" means "the object being built", which tells the field and the parameter apart
    // when they have the same name.
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
    // A getter returns a field's value; a setter replaces it.

    // Returns the receipt code.
    public String getBookingId() {
        return bookingId;
    }

    // Changes the receipt code.
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    // Returns the ID of the event this booking is for.
    public String getEventId() {
        return eventId;
    }

    // Changes which event this booking points to.
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // Returns the guest's name.
    public String getCustomerName() {
        return customerName;
    }

    // Changes the guest's name.
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    // Returns the guest's email address.
    public String getCustomerEmail() {
        return customerEmail;
    }

    // Changes the guest's email address.
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    // Short one-line getter and setter for the ticket tier name.
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    // Returns how many tickets were bought.
    public int getQuantity() {
        return quantity;
    }

    // Changes how many tickets were bought.
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Returns the total amount paid.
    public double getTotalPrice() {
        return totalPrice;
    }

    // Changes the total amount paid.
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    // Getter and setter for the purchase time. The sample-sales code uses the setter
    // to give old bookings a time in the past.
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
}
