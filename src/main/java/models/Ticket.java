// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

/**
 * An older model of a single ticket, kept so earlier code still compiles.
 * New purchases use {@link Booking} instead.
 *
 * OOP idea, association: a field can hold another object, not just text or numbers.
 * A Ticket is linked to the {@link User} who owns it and the {@link Event} it is for.
 */
public class Ticket {
    private String ticketId;      // The ticket's own ID
    // Association: these fields point to other objects instead of storing plain IDs.
    private User user;            // The person who owns this ticket
    private Event event;          // The event this ticket gets you into
    private String seatNumber;    // e.g. "A12"

    // Empty constructor: makes a blank ticket to fill in with the setters.
    public Ticket() {
    }

    // Full constructor: copies each parameter into the matching field ("this." = this object's field).
    public Ticket(String ticketId, User user, Event event, String seatNumber) {
        this.ticketId = ticketId;
        this.user = user;
        this.event = event;
        this.seatNumber = seatNumber;
    }

    // Getters and setters (encapsulation): the public way to read and change the private fields.

    // Returns the ticket ID.
    public String getTicketId() {
        return ticketId;
    }

    // Changes the ticket ID.
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    // Returns the User object that owns this ticket.
    public User getUser() {
        return user;
    }

    // Links this ticket to a different User object.
    public void setUser(User user) {
        this.user = user;
    }

    // Returns the Event object this ticket is for.
    public Event getEvent() {
        return event;
    }

    // Links this ticket to a different Event object.
    public void setEvent(Event event) {
        this.event = event;
    }

    // Returns the seat number.
    public String getSeatNumber() {
        return seatNumber;
    }

    // Changes the seat number.
    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}
