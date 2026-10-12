package models;

/**
 * An older model of a single ticket, kept so earlier code still compiles.
 * New purchases use {@link Booking} instead.
 *
 * OOP idea, association: a field can hold another object, not just text or numbers.
 * A Ticket is linked to the {@link User} who owns it and the {@link Event} it is for.
 */
public class Ticket {
    private String ticketId;
    // Association: these fields point to other objects instead of storing plain IDs.
    private User user;
    private Event event;
    private String seatNumber;

    public Ticket() {
    }

    public Ticket(String ticketId, User user, Event event, String seatNumber) {
        this.ticketId = ticketId;
        this.user = user;
        this.event = event;
        this.seatNumber = seatNumber;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}
