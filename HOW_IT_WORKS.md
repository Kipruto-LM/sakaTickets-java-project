# How SakaTickets Works (dev)

This guide follows the simplified `dev` branch. Swing draws the screens, Java objects hold the
data, controller classes coordinate the work, and SQLite keeps events and bookings after the
application closes.

## Project map

```text
Main.java
  └── MainFrame (the window and screen navigation)
       ├── views/       Swing panels: welcome, catalog, checkout, simple host dashboard
       ├── controllers/ account, event, and booking operations
       └── models/      Event, TicketTier, Booking, and User objects
            database/   SQLite tables and SQL operations
```

The folders follow a simple version of **MVC**:

- **Model** (`models/`) describes the information, such as an event or a booking.
- **View** (`views/`) displays information and collects user input.
- **Controller** (`controllers/`) applies rules and changes the models.

For example, `CheckoutPanel` is a view. It asks `EventManager` to find an event, then sends the
purchase details to `OrderManager`. `OrderManager` asks SQLite to save the `Booking` and update
ticket inventory together.

## OOP ideas in this project

### Classes and objects

A class is a description; an object is one instance made from it. `Event` is a class, while the
seeded "Nairobi Tech Meetup" is one `Event` object. `EventManager` keeps a list of these objects.

### Encapsulation

`Event` and `User` keep their fields `private`. Other classes use methods such as `getTitle()` or
`checkPassword()` rather than directly reading a field. `Event.canPurchase()` describes whether the
event status permits sales; SQLite checks that rule again when saving a purchase.

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

1. `Main.main()` sets the FlatLaf dark look and feel.
2. Swing schedules UI creation on its Event Dispatch Thread.
3. `EventManager` creates the SQLite tables if needed, loads saved events, and inserts the sample
   events only when the database is empty.
4. `MainFrame` creates one `EventManager`, `AccountManager`, and `OrderManager`.
5. It creates the panels and adds them to a `CardLayout`. Showing another screen swaps the visible
   card instead of opening another window.

### Buying tickets

1. A guest browses events in `EventCatalogPanel` and chooses one.
2. `MainFrame` opens `CheckoutPanel` for that event.
3. `OrderManager` asks SQLite to reduce available seats and insert the booking in one transaction.
   The database rejects a purchase if sales are closed or not enough seats remain.
4. SQLite updates the event status to `SOLD_OUT` when all tiers run out and returns the new
   inventory so the in-memory Event object can match it.
5. The receipt panel displays the saved `Booking`.

### Hosting an event

1. `AdminLoginPanel` collects the host's information and calls `AccountManager`.
2. `AccountManager` checks the input, finds or creates a `User`, and saves host profiles locally.
3. After sign-in, `MainFrame` creates the host dashboard and passes it the shared `EventManager`.
4. The dashboard shows the host's events in a list. Selecting one fills the form for editing.
5. Saving creates a new event or updates the selected event through `EventManager`. New events get
   one `Regular` ticket tier to keep setup simple; editing keeps existing tiers unchanged.

The host dashboard intentionally contains only an event list, a small event form, and a logout
button. Analytics, attendee exports, custom event fields, archive/duplicate actions, and appearance
settings are not part of this simplified branch.

## Important files

| File or folder | Responsibility |
|---|---|
| `Main.java` | Starts Swing and opens the main window |
| `views/MainFrame.java` | Owns the screen navigation and shared managers |
| `models/Event.java` | Event details, tier composition, and purchase rules |
| `models/TicketTier.java` | Price and remaining ticket inventory |
| `models/Booking.java` | Receipt data for a completed purchase |
| `models/User.java` | Host profile and simple demo password check |
| `controllers/EventManager.java` | Loads, creates, and updates events through SQLite |
| `controllers/OrderManager.java` | Saves bookings and updates ticket inventory |
| `controllers/AccountManager.java` | Host registration, sign-in, and local account file |
| `database/Database.java` | SQLite connection, table creation, and SQL statements |
| `views/` | Screens, presentation, and shared UI helpers |

## Storage and password warning

Events, ticket tiers, and bookings are stored in the `sakatickets.db` SQLite file in the working
directory. The first run creates tables and sample events. Host accounts are still kept in
`~/.sakatickets/hosts-demo.properties`, not in SQLite.

For this class-project branch, a password is just a `String` field on `User`, displayed as it is
typed, and written as plain text to the local properties file. Registration asks for the password
once, with no confirmation field. This deliberately skips password masking and hashing as teaching
simplifications. Use only the demo credentials; never use this approach for real accounts or real
passwords.
