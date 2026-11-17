# How SakaTickets Works (dev)

This guide follows the simplified `dev` branch. SakaTickets is a desktop program: Swing draws the
screens, Java objects hold the data, and controller classes coordinate the work. There is no
database in this branch.

## Project map

```text
Main.java
  └── MainFrame (the window and screen navigation)
       ├── views/       Swing panels: welcome, catalog, checkout, host dashboard
       ├── controllers/ account, event, and booking operations
       └── models/      Event, TicketTier, Booking, and User objects
```

The folders follow a simple version of **MVC**:

- **Model** (`models/`) describes the information, such as an event or a booking.
- **View** (`views/`) displays information and collects user input.
- **Controller** (`controllers/`) applies rules and changes the models.

For example, `CheckoutPanel` is a view. It asks `EventManager` to find an event and purchase a tier;
the event's ticket count changes, and `OrderManager` creates a `Booking` receipt.

## OOP ideas in this project

### Classes and objects

A class is a description; an object is one instance made from it. `Event` is a class, while the
seeded "Nairobi Tech Meetup" is one `Event` object. `EventManager` keeps a list of these objects.

### Encapsulation

`Event` and `User` keep their fields `private`. Other classes use methods such as `getTitle()` or
`checkPassword()` rather than directly reading a field. `Event.purchase()` keeps the ticket-sale
rules next to the event data they affect.

### Constructors and factory methods

Constructors initialize new objects. `Event` has more than one constructor, so the app can create
an event from either multiple ticket tiers or a simple seat-count and price pair. This is
**constructor overloading**.

`User.defaultAdmin()` is a **static factory method**: call it on the class to get the built-in demo
host object. The `static final` demo account values belong to the `User` class, not to an individual
account.

### Composition ("has-a")

An `Event` has a list of `TicketTier` objects. `MainFrame` also has the managers and screen panels
that make up the running application. These objects work together by holding references to one
another; this is composition.

### Inheritance

Swing supplies reusable UI classes. For example, `MainFrame` extends `JFrame`, and panels such as
`EventCatalogPanel` extend `JPanel`. They inherit window or panel behavior and add SakaTickets
content.

### Interfaces and callbacks

Views receive callbacks such as `Runnable` and `Consumer<User>`. A view can run a callback when a
button is clicked without needing to know all the details of the screen that will appear next.
`MainFrame` supplies those callbacks when it creates the panels.

## Follow the main flows

### Starting the application

1. `Main.main()` applies the saved appearance preference.
2. Swing schedules UI creation on its Event Dispatch Thread.
3. `MainFrame` creates one `EventManager`, `AccountManager`, and `OrderManager`.
4. It creates the panels and adds them to a `CardLayout`. Showing another screen swaps the visible
   card instead of opening another window.

### Buying tickets

1. A guest browses events in `EventCatalogPanel` and chooses one.
2. `MainFrame` opens `CheckoutPanel` for that event.
3. The checkout asks the event to sell the requested quantity. `TicketTier.purchase()` prevents
   selling more seats than remain; `Event` updates its sold-out status when appropriate.
4. `OrderManager` creates a `Booking`, which the receipt panel displays.

### Hosting an event

1. `AdminLoginPanel` collects the host's information and calls `AccountManager`.
2. `AccountManager` checks the input, finds or creates a `User`, and saves host profiles locally.
3. After sign-in, `MainFrame` creates the host dashboard and passes it the shared managers.
4. The dashboard uses `EventManager` to create and update events.

## Important files

| File or folder | Responsibility |
|---|---|
| `Main.java` | Starts Swing and opens the main window |
| `views/MainFrame.java` | Owns the screen navigation and shared managers |
| `models/Event.java` | Event details, tier composition, and purchase rules |
| `models/TicketTier.java` | Price and remaining ticket inventory |
| `models/Booking.java` | Receipt data for a completed purchase |
| `models/User.java` | Host profile and simple demo password check |
| `controllers/EventManager.java` | Seed data and event catalog operations |
| `controllers/OrderManager.java` | In-memory bookings and basic sales totals |
| `controllers/AccountManager.java` | Host registration, sign-in, and local account file |
| `views/` | Screens, presentation, and shared UI helpers |

## Storage and password warning

Events and bookings are in memory, so they reset when the app closes. Host accounts are kept in
`~/.sakatickets/hosts-demo.properties`.

For this class-project branch, a password is just a `String` field on `User` and is written as plain
text to the local properties file. This deliberately skips password hashing as a teaching
simplification. Use only the demo credentials; never use this approach for real accounts or real
passwords.
