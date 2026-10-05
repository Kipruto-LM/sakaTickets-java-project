package models;

/** Legacy ticket record linking an issued ticket to a user, event, and seat. */
public class Ticket {
    // Legacy ticket relationship fields retained for older project flows.
    private String ticketId;
    private User user;
    private Event event;
    private String seatNumber;

    /** Creates an empty ticket for bean-style tools. */
    public Ticket() {
    }

    /** Creates a ticket with its associated user, event, and seat number. */
    public Ticket(String ticketId, User user, Event event, String seatNumber) {
        this.ticketId = ticketId;
        this.user = user;
        this.event = event;
        this.seatNumber = seatNumber;
    }

    // Standard bean accessors used by older ticket screens.
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
