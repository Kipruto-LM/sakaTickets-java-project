package models;

public class Event {
    private String eventId;
    private String title;
    private String date;
    private String venue;
    private int availableSeats;
    private double ticketPrice;

    public Event() {
    }

    public Event(String eventId, String title, String date, String venue, int availableSeats, double ticketPrice) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.venue = venue;
        this.availableSeats = availableSeats;
        this.ticketPrice = ticketPrice;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    // Core logic to handle a ticket purchase
    public boolean purchaseTickets(int amount) {
        if (amount > 0 && amount <= availableSeats) {
            availableSeats -= amount;
            return true; // Purchase successful
        }
        return false; // Not enough tickets available
    }
}
