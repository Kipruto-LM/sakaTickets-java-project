package models;

public class Ticket {
    private String ticketId;
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
