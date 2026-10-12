# How SakaTickets Works — A Beginner's Guide to the Code

This guide explains **what SakaTickets does, how the code is organised, and which Object-Oriented
Programming (OOP) ideas it uses** — with real examples taken straight from our project.

It's written for first-year students meeting OOP for the first time. You don't need to read it in one go:
the basics come first, and the harder ideas get shorter, simpler explanations near the end.

> 💡 **Tip:** keep the project open in VS Code while you read. Every example names the file it comes
> from, so you can open it and see the real thing. Snippets marked `// ...` have been shortened.

---

## Contents

1. [What the app does](#1-what-the-app-does)
2. [How the project is organised](#2-how-the-project-is-organised)
3. [The core OOP ideas (with examples)](#3-the-core-oop-ideas-with-examples)
   - [3.1 Classes and objects](#31-classes-and-objects)
   - [3.2 Fields](#32-fields)
   - [3.3 Constructors](#33-constructors)
   - [3.4 Methods](#34-methods)
   - [3.5 Encapsulation](#35-encapsulation)
   - [3.6 `static` — belonging to the class, not the object](#36-static--belonging-to-the-class-not-the-object)
   - [3.7 Composition — "has-a"](#37-composition--has-a)
   - [3.8 Inheritance — "is-a"](#38-inheritance--is-a)
   - [3.9 Method overriding](#39-method-overriding)
   - [3.10 Polymorphism](#310-polymorphism)
   - [3.11 Abstract classes](#311-abstract-classes)
   - [3.12 Interfaces](#312-interfaces)
4. [Other Java ideas you'll see (short versions)](#4-other-java-ideas-youll-see-short-versions)
5. [MVC — how the pieces fit together](#5-mvc--how-the-pieces-fit-together)
6. [Following the code: four journeys](#6-following-the-code-four-journeys)
7. [The app and the database, side by side](#7-the-app-and-the-database-side-by-side)
8. [Glossary](#8-glossary)
9. [Every file in one line](#9-every-file-in-one-line)

---

## 1. What the app does

SakaTickets is a desktop app for **buying and selling event tickets** in Nairobi. It has two kinds of user:

| Guest (buyer) | Host (event organiser) |
|---|---|
| Browses upcoming events | Logs in with a username and password |
| Opens an event's details page | Sees sales totals and charts |
| Picks a ticket tier (Early Bird, Regular, VIP) | Creates, edits, archives and deletes events |
| Enters name + email and buys | Sets up ticket tiers with prices and seat numbers |
| Gets a receipt with a booking code | Sees who booked, and exports a CSV list |

Everything is **saved in a PostgreSQL database**, so it's still there when you close and reopen the app.

---

## 2. How the project is organised

All the Java code lives in `src/main/java`, split into four **packages** (folders):

```
src/main/java/
│
├── Main.java            ← The starting point. Java runs this first.
│
├── models/              ← The DATA: what things ARE
│   ├── Event.java           an event (title, date, venue, tiers…)
│   ├── TicketTier.java      one price level (e.g. "VIP – KES 3,750 – 18 seats")
│   ├── Booking.java         one purchase (the receipt)
│   └── User.java            a host account
│
├── controllers/         ← The LOGIC: what you can DO with the data
│   ├── EventManager.java    add / edit / delete events, sell seats
│   ├── OrderManager.java    record bookings, work out sales totals
│   └── AccountManager.java  sign up, log in, delete hosts
│
├── database/            ← The LINK to PostgreSQL
│   └── Database.java        opens the connection, creates the tables
│
└── views/               ← The SCREENS: what you SEE
    ├── MainFrame.java       the window that holds every screen
    ├── WelcomePanel.java    the first screen
    ├── EventCatalogPanel.java, EventDetailsPanel.java, CheckoutPanel.java, TicketSummaryPanel.java
    ├── AdminLoginPanel.java, AdminDashboardPanel.java (+ OverviewPage, EventListPage,
    │   EventEditorPage, EventAnalyticsPanel, SettingsPage)
    └── ChartPanel.java, ColumnChart.java, HorizontalBarChart.java, ViewStyles.java, …
```

This split is called **MVC — Model, View, Controller**. Section 5 explains why it's useful.

---

## 3. The core OOP ideas (with examples)

### 3.1 Classes and objects

A **class** is a *blueprint*. An **object** is a *real thing built from the blueprint*.

Think of a class as the design for a ticket tier, and objects as the actual tiers of an actual event:

```java
// models/TicketTier.java — the blueprint
public class TicketTier {
    private String tierName;
    private double price;
    private int initialCapacity;
    private int availableSeats;
    // ...
}
```

```java
// controllers/EventManager.java — building three real objects from that one blueprint
tiers.add(new TicketTier("Early Bird", regularPrice * 0.7, earlyBirdSeats));
tiers.add(new TicketTier("Regular", regularPrice, capacity - earlyBirdSeats - vipSeats));
tiers.add(new TicketTier("VIP", regularPrice * 2.5, vipSeats));
```

The keyword **`new`** creates an object. Each of the three tiers is a separate object with its *own*
name, price and seats — changing the VIP seats doesn't touch the Regular seats.

> **In one line:** one class (`TicketTier`), many objects (every tier of every event).

---

### 3.2 Fields

**Fields** (also called *attributes* or *instance variables*) are the data each object holds.
They describe the object's **state**.

```java
// models/Booking.java
public class Booking {
    private String bookingId;     // e.g. "7FE84F78"
    private String eventId;       // which event was booked
    private String customerName;
    private String customerEmail;
    private String tierName;      // e.g. "VIP"
    private int quantity;         // how many tickets
    private double totalPrice;    // price × quantity
    private LocalDateTime purchasedAt = LocalDateTime.now();   // when it was bought
}
```

Every `Booking` object has its own copy of all eight fields. Wanjiru's booking and Otieno's booking
are two objects with different values in the same fields.

---

### 3.3 Constructors

A **constructor** is a special method that runs **once, when an object is created with `new`**.
Its job is to set up the object's fields. It has the **same name as the class** and **no return type**.

```java
// models/TicketTier.java
public TicketTier(String tierName, double price, int initialCapacity, int availableSeats) {
    this.tierName = tierName;
    this.price = price;
    this.initialCapacity = initialCapacity;
    this.availableSeats = availableSeats;
}
```

**What is `this`?** It means *"the object being built"*. In `this.price = price;` the left side is the
object's field, and the right side is the value passed in. `this` tells Java which is which, because
they have the same name.

#### Constructor overloading (several constructors in one class)

A class can have **more than one constructor**, as long as their parameter lists differ. `TicketTier` has three:

```java
public TicketTier() { }                                                    // 1. empty

public TicketTier(String tierName, double price, int initialCapacity) {   // 2. a brand-new tier
    this(tierName, price, initialCapacity, initialCapacity);
}

public TicketTier(String tierName, double price, int initialCapacity, int availableSeats) {  // 3. full
    // ... (shown above)
}
```

Constructor 2 is a **shortcut**: a brand-new tier has *all* its seats available, so it calls
constructor 3 with `initialCapacity` passed twice. **`this(...)`** (with brackets) means
*"run another constructor of this same class"*. This avoids writing the same setup code twice.

> **When is each used?**
> `new TicketTier("VIP", 3750, 18)` → constructor 2 (new tier, 18 of 18 free).
> `new TicketTier("VIP", 3750, 18, 5)` → constructor 3 (loaded from the database: 5 of 18 free).

---

### 3.4 Methods

A **method** is an action an object can do. It can take **parameters** (inputs) and give back a
**return value** (output). `void` means it gives nothing back.

```java
// models/TicketTier.java
public boolean purchase(int qty) {                 // takes a number, returns true/false
    if (qty <= 0 || qty > availableSeats) {
        return false;                              // refuse: bad amount or not enough seats
    }
    availableSeats -= qty;                         // take the seats
    return true;                                   // success
}
```

Calling it:

```java
boolean bought = tier.purchase(2);   // "tier, please sell 2 seats" → true or false
```

The dot (`tier.purchase`) means *"call the `purchase` method **on this particular object**"*.
Only that tier's seats change.

#### Getters and setters

Small methods whose only job is to read (*get*) or change (*set*) a field:

```java
// models/Event.java
public String getTitle() { return title; }
public void setTitle(String title) { this.title = title; }
```

---

### 3.5 Encapsulation

**Encapsulation** means *keeping an object's data private and only letting other classes use it
through the object's own methods.* It's like a vending machine: you can't reach inside and grab a
drink — you press buttons, and the machine decides what's allowed.

In Java, you do this with the keyword **`private`**:

```java
// models/TicketTier.java
private int availableSeats;     // no other class can touch this directly
```

So other classes **cannot** write `tier.availableSeats = -50;`. The only way to sell seats is to
call `tier.purchase(...)`, and `purchase` **checks the rules first** (no negative amounts, no
overselling). That makes it impossible for the rest of the program to put a tier into a broken state.

| Keyword | Who can use it |
|---|---|
| `public` | Any class |
| `private` | Only code inside the same class |
| *(nothing)* | Only classes in the same package (folder) |
| `protected` | Same package, plus subclasses |

A second example — `User` never lets anyone read the password field to compare it themselves.
Instead, it offers a method that does the check:

```java
// models/User.java
public boolean checkPassword(String typedPassword) {
    return password.equals(typedPassword);
}
```

---

### 3.6 `static` — belonging to the class, not the object

Normally every object has its own copy of each field. A **`static`** field or method belongs to the
**class itself** — there's only one, shared by everyone, and you use it through the class name.

```java
// models/User.java
public static final String DEFAULT_ADMIN_USERNAME = "Talel";   // one shared constant

public static User defaultAdmin() {                             // called on the class
    return new User(DEFAULT_ADMIN_ID, DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
            DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
}
```

```java
User admin = User.defaultAdmin();    // note: User.something, not someUser.something
```

- `final` means *"can't be changed after it's set"*, so `static final` = a shared constant.
- Constants are written in `CAPITAL_LETTERS` by convention.
- `main` in `Main.java` is `static` because Java has to run it **before any objects exist**.
- A method like `defaultAdmin()` that builds and returns an object is called a **factory method**.

---

### 3.7 Composition — "has-a"

**Composition** means an object is *built out of other objects*. We describe it with **"has-a"**.

- An `Event` **has a** list of `TicketTier` objects.
- The `MainFrame` window **has a** `CheckoutPanel`, a `EventCatalogPanel`, and so on.
- The `AdminDashboardPanel` **has an** `OverviewPage`, an `EventListPage`, an `EventEditorPage`…

```java
// models/Event.java
private List<TicketTier> ticketTiers = new ArrayList<>();   // an Event HAS tiers
```

The useful part: the `Event` doesn't do the tiers' job for them. When someone buys tickets, the
event **finds the right tier and asks it to sell** — this is called **delegation**:

```java
// models/Event.java
public boolean purchase(String tierName, int quantity) {
    if (!canPurchase()) {
        return false;                                  // sales are closed
    }
    for (TicketTier tier : ticketTiers) {
        if (tier.getTierName().equals(tierName)) {
            boolean bought = tier.purchase(quantity);  // ← delegation: the tier does the work
            if (bought && getAvailableSeats() == 0) {
                status = "SOLD_OUT";                   // that was the very last seat
            }
            return bought;
        }
    }
    return false;                                      // no tier with that name
}
```

---

### 3.8 Inheritance — "is-a"

**Inheritance** lets a new class **reuse everything from an existing class** and add to it.
We describe it with **"is-a"**, and write it with the keyword **`extends`**.

```java
// views/MainFrame.java
public class MainFrame extends JFrame { ... }
```

`JFrame` is Java's built-in window class. Because `MainFrame` **is a** `JFrame`, it automatically gets
all of `JFrame`'s methods without writing them:

```java
// views/MainFrame.java — none of these methods are written in MainFrame; they're inherited
setTitle("SakaTickets");
setSize(1200, 800);
setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
```

Every screen in our app works the same way — they all extend `JPanel` (Java's built-in "rectangle you
can put things in"):

```
            JPanel  (Java's built-in class — the "parent" / "superclass")
               │
   ┌───────────┼───────────────┬─────────────────┬──────────────┐
WelcomePanel  CheckoutPanel  AdminLoginPanel  OverviewPage   ChartPanel …
   (each one IS a JPanel, so each can be placed on the screen)
```

- The class being extended is the **superclass** (parent). The new class is the **subclass** (child).
- **`super(...)`** calls the parent's constructor. Example: `ColumnChart`'s constructor starts with
  `super(height);`, which runs `ChartPanel`'s constructor first.

---

### 3.9 Method overriding

A subclass can **replace** a method it inherited with its own version. This is **overriding**, and we
mark it with **`@Override`** (which asks Java to check we really are replacing an existing method).

**Example 1 — `toString()`.** Every Java class secretly extends a class called `Object`, which has a
`toString()` method that turns an object into text. The default version prints something unhelpful
like `models.TicketTier@1b6d3586`. So `TicketTier` overrides it:

```java
// models/TicketTier.java
@Override
public String toString() {
    return String.format("%s - KES %,.0f (%d of %d left)", tierName, price, availableSeats, initialCapacity);
}
```

Now, whenever Java needs to *show* a tier as text — in the checkout drop-down list, or the editor's
tier list — it calls our version, and you see **`VIP - KES 3,750 (12 of 18 left)`**.
We never call `toString()` ourselves; Swing does it for us.

**Example 2 — drawing.** `JPanel` has a `paintComponent` method that draws the panel. Our
`RoundedCardPanel` (inside `EventCatalogPanel.java`) overrides it to draw rounded corners:

```java
@Override
protected void paintComponent(java.awt.Graphics graphics) {
    // ... fill a rounded rectangle ...
    super.paintComponent(graphics);    // then also run the parent's normal drawing
}
```

---

### 3.10 Polymorphism

**Polymorphism** ("many forms") means **one piece of code can work with objects of different classes,
and each object responds in its own way.**

**Example 1 — the charts.** Both `ColumnChart` and `HorizontalBarChart` extend `ChartPanel`, and each
has its *own* version of `paintChart(...)`. Swing doesn't know or care which kind of chart it has — it
just tells each one to paint itself, and **Java automatically runs the right version**:

```
    Swing:  "chart, paint yourself"
                    │
       ┌────────────┴─────────────┐
  ColumnChart.paintChart()   HorizontalBarChart.paintChart()
   draws vertical columns     draws sideways bars
```

**Example 2 — the screens.** `MainFrame` keeps all the screens in one "deck" and can treat them all as
`JPanel`s, even though they're `WelcomePanel`, `CheckoutPanel`, `AdminDashboardPanel`…:

```java
// views/MainFrame.java
cardPanel.add(welcomePanel, WELCOME);
cardPanel.add(catalogPanel, CATALOG);
cardPanel.add(checkoutPanel, CHECKOUT);
```

**Example 3 — `toString()`** (from 3.9): the drop-down list calls `toString()` on whatever it holds;
for a `TicketTier` it gets our custom text.

> **In one line:** same method call, different object, different behaviour — chosen automatically while
> the program runs.

---

### 3.11 Abstract classes

An **abstract class** is a class that is **only meant to be a parent** — you can't create an object
from it directly. It can leave some methods **unfinished** (`abstract`), and every child **must** finish them.

```java
// views/ChartPanel.java
abstract class ChartPanel extends JPanel {

    // Shared by every chart: colours, size, smooth drawing...
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintChart(g, getWidth(), getHeight());     // ← calls the unfinished method
    }

    abstract void paintChart(Graphics2D g, int width, int height);   // no body: children must write it
}
```

```java
// views/ColumnChart.java
class ColumnChart extends ChartPanel {
    @Override
    void paintChart(Graphics2D g, int width, int height) {
        // ... draws the columns ...
    }
}
```

`new ChartPanel(...)` is **not allowed** (it's not a complete chart). `new ColumnChart(...)` **is**.
The shared code lives once in the parent; only the part that's different lives in each child.

---

### 3.12 Interfaces

An **interface** is a list of methods that a class **promises** to provide — a contract, with no code
of its own. A class signs the contract with **`implements`**.

```java
// views/ScrollableColumn.java
class ScrollableColumn extends JPanel implements Scrollable {
    @Override
    public boolean getScrollableTracksViewportWidth() { return true; }   // "match the window's width"
    // ... plus the other four methods that Scrollable requires ...
}
```

Because `ScrollableColumn` keeps the `Scrollable` promise, Java's scroll pane knows exactly how to
scroll it (only up and down, never sideways).

- A class can **extend only one** class, but **implement many** interfaces.
- You'll also see the built-in interface **`Runnable`** (one method: `run()`), which we use for button
  actions — see "callbacks" in section 4.

---

## 4. Other Java ideas you'll see (short versions)

| Idea | What it means | Where to see it |
|---|---|---|
| **Package & import** | Packages are folders that group classes. `import` lets you use a class from another package by its short name. | Top of every file, e.g. `import models.Event;` |
| **`ArrayList`** | A list that grows as you add things. | `List<Event> events = new ArrayList<>();` in `EventManager` |
| **`LinkedHashMap`** | A list of *name → value* pairs that remembers their order. | An event's custom details: `"Dress code" → "Smart casual"` |
| **`for` each loop** | "Do this once for every item in the list." | `for (TicketTier tier : ticketTiers) { ... }` |
| **`null`** | "No object here." Always check for it before using something that might be missing. | `login(...)` returns `null` when the password is wrong |
| **Exceptions: `throw`** | A way to say "something went wrong — stop and report it." | `throw new IllegalArgumentException("Enter a valid email address.");` in `AccountManager.register` |
| **Exceptions: `try` / `catch`** | "Try this; if it goes wrong, do this instead." | The login screen catches the exception above and shows its message on the form |
| **Callbacks** (`Runnable`, `Consumer`) | Handing a piece of code to another object, to run later — e.g. "when this button is clicked, do *this*". It lets screens ask `MainFrame` to change page without knowing about each other. | `new WelcomePanel(this::showCatalog, ...)` in `MainFrame` |
| **Lambda** `x -> ...` | A tiny unnamed method, usually for a button. | `save.addActionListener(action -> saveEvent());` = "when Save is clicked, run `saveEvent()`" |
| **Method reference** `this::name` | A shortcut for a lambda that just calls one method. | `this::showCatalog` = "run my `showCatalog` method" |
| **Ternary** `a ? b : c` | A one-line if/else: "if `a`, use `b`, otherwise `c`". | `left == 0 ? "Sold out" : "Select"` |
| **`String.format`** | Builds text with numbers in it. `%,.0f` = number with commas, no decimals. | `String.format("KES %,.0f", 1500.0)` → `"KES 1,500"` |
| **Nested class** | A small class written inside another class, only used there. | `RoundedCardPanel` inside `EventCatalogPanel` |

---

## 5. MVC — how the pieces fit together

**MVC** splits the program into three jobs so that each class has *one* clear responsibility:

```
   ┌──────────────┐   "Buy 2 VIP tickets"   ┌──────────────────┐   saves/loads   ┌──────────────┐
   │     VIEW     │ ──────────────────────► │    CONTROLLER    │ ──────────────► │   DATABASE   │
   │  (screens)   │                         │ (EventManager,   │                 │ (PostgreSQL) │
   │ CheckoutPanel│ ◄────────────────────── │  OrderManager…)  │ ◄────────────── │              │
   └──────────────┘   "Done — here's the    └──────────────────┘   rows of data  └──────────────┘
                         Booking object"            │  ▲
                                                    ▼  │  uses
                                             ┌──────────────────┐
                                             │      MODEL       │
                                             │ (Event, Booking, │
                                             │  TicketTier…)    │
                                             └──────────────────┘
```

| Part | Job | Never does… |
|---|---|---|
| **Model** (`models/`) | Holds the data and its rules (e.g. "can't oversell seats") | Draw anything or touch the database |
| **View** (`views/`) | Shows things and reacts to clicks | Contain business rules or SQL |
| **Controller** (`controllers/`) | Does the work: saving, loading, calculating | Draw anything |

**Why bother?** If we redesigned the checkout screen tomorrow, the rules for selling tickets wouldn't
change. If we switched from PostgreSQL to another database, only the controllers would change. Each part
can be understood (and fixed) on its own.

**Dependency injection** — a fancy name for a simple idea: objects are *given* what they need through
their constructor, instead of creating it themselves. `MainFrame` opens **one** database connection and
hands it to all three controllers, so they all share it:

```java
// views/MainFrame.java
Connection connection = Database.connect();
accountManager = new AccountManager(connection);
eventManager = new EventManager(connection);
orderManager = new OrderManager(connection);
```

---

## 6. Following the code: four journeys

### Journey 1 — The app starts

```
Main.main()                                 ← Java starts here (static, so no object needed yet)
  └─ new MainFrame()                         ← build the window
       ├─ Database.connect()                 ← read db.properties, open PostgreSQL, create tables if missing
       ├─ new AccountManager(connection)     ← makes sure the "Talel" account exists
       ├─ new EventManager(connection)       ← loads all events (adds 7 samples if the database is empty)
       ├─ new OrderManager(connection)       ← loads all bookings
       ├─ (first run only) add sample sales
       ├─ new WelcomePanel(...), new CheckoutPanel(...), …   ← build every screen
       └─ show the "Welcome" screen
```

If the database can't be reached, `Database.connect()` **throws** an exception, and `Main` **catches**
it and shows a pop-up explaining the problem.

### Journey 2 — A guest buys 2 VIP tickets

1. **View:** the guest fills in the form on `CheckoutPanel` and clicks **Confirm Purchase**.
   That button was set up with `confirmButton.addActionListener(event -> submitBooking());`
2. **View → Controller:** `submitBooking()` asks the controller to sell the seats:
   ```java
   eventManager.purchaseTickets(selectedEvent, selectedTier.getTierName(), quantity)
   ```
3. **Controller → Model:** `EventManager.purchaseTickets` asks the `Event` object, which asks the
   right `TicketTier` (delegation, section 3.7). The tier checks the rules and takes 2 seats.
4. **Controller → Database:** `EventManager` saves the event's new seat count (`UPDATE`).
5. **View → Controller:** `CheckoutPanel` asks `OrderManager.createBooking(...)`, which makes a new
   `Booking` object with a random code like `7FE84F78` and saves it (`INSERT INTO bookings`).
6. **Back to the View:** the `Booking` is handed to `MainFrame` (a callback), which shows the receipt
   on `TicketSummaryPanel`.

### Journey 3 — A host creates an event

1. On `EventEditorPage`, the host types a title, picks a date, adds tiers, and clicks **Save Event**.
2. `saveEvent()` checks the form (title, date, venue and at least one tier).
3. It builds the objects: a list of `TicketTier` objects and a `new Event(...)`.
4. It calls `eventManager.addEvent(event)`, which runs three kinds of `INSERT`: one row in `events`,
   one row per tier in `ticket_tiers`, one row per custom detail in `event_details`.
5. The dashboard refreshes, and the new event appears in the list — and in the guest catalogue.

### Journey 4 — A host logs in

1. `AdminLoginPanel` reads the username and password boxes and calls
   `accountManager.login(username, password)`.
2. `AccountManager` runs `SELECT * FROM hosts WHERE LOWER(username) = LOWER(?)`, turns the row into a
   `User` object, and asks it `user.checkPassword(password)`.
3. It returns the `User` if the password is right, or `null` if not.
4. If it got a `User`, the login screen hands it to `MainFrame`, which builds an
   `AdminDashboardPanel` for that host.

---

## 7. The app and the database, side by side

### Why a database at all?

Objects live in the computer's **memory**, which is wiped when the program closes. A **database** stores
data **on disk**, permanently. So the app *loads* data from the database when it starts, and *saves*
every change straight back.

### Classes ↔ tables, objects ↔ rows

A database stores data in **tables** — like spreadsheets. Our tables mirror our model classes:

| Java (in memory) | PostgreSQL (on disk) |
|---|---|
| the `Event` **class** | the `events` **table** |
| one `Event` **object** | one **row** in `events` |
| the `title` **field** | the `title` **column** |
| an `Event` **has** `TicketTier`s (a list) | each `ticket_tiers` row has an `event_id` pointing to its event |

Our five tables:

```
hosts            one row per host account
events           one row per event
 ├── ticket_tiers   one row per tier       (linked to an event by event_id)
 ├── event_details  one row per detail     (linked to an event by event_id)
 └── bookings       one row per purchase   (linked to an event by event_id)
```

A column that points to another table's row (like `event_id` in `ticket_tiers`) is a **foreign key**.
Ours are set to **`ON DELETE CASCADE`**: delete an event, and the database automatically deletes its
tiers, details and bookings too.

### How Java talks to the database

```
  Your Java code ──► JDBC (Java's database toolkit) ──► PostgreSQL driver ──► PostgreSQL
                     java.sql.Connection,               (a library listed      (the database
                     PreparedStatement, ResultSet        in pom.xml)            server)
```

1. **`db.properties`** holds the address, username and password.
2. **`Database.connect()`** reads that file and opens a **`Connection`** — an open line to PostgreSQL.
3. The controllers send **SQL** (the database's language) through that connection.

### SQL in four commands

| SQL | Meaning | Example from our code |
|---|---|---|
| `SELECT` | Read rows | `SELECT * FROM events ORDER BY event_date` |
| `INSERT` | Add a row | `INSERT INTO bookings (booking_id, event_id, …) VALUES (?, ?, …)` |
| `UPDATE` | Change a row | `UPDATE events SET title = ?, … WHERE event_id = ?` |
| `DELETE` | Remove a row | `DELETE FROM events WHERE event_id = ?` |

### Saving: object → row

```java
// controllers/EventManager.java (shortened)
PreparedStatement insert = connection.prepareStatement(
        "INSERT INTO events (event_id, host_id, title, event_date, venue, ...) VALUES (?, ?, ?, ?, ?, ...)");
insert.setString(1, event.getEventId());    // fill blank 1
insert.setString(2, event.getHostId());     // fill blank 2
insert.setString(3, event.getTitle());      // fill blank 3
// ...
insert.executeUpdate();                     // send it to PostgreSQL
```

Each **`?`** is a blank that we fill in with `setString`, `setInt` and so on. Using blanks (instead of
gluing the text into the SQL ourselves) keeps the user's typing as *data* — someone can't type SQL into
the "Event title" box and run it. This is called a **`PreparedStatement`**.

### Loading: row → object

```java
// controllers/EventManager.java (shortened)
ResultSet row = query.executeQuery();         // run the SELECT
while (row.next()) {                          // move to the next row; false when there are no more
    Event event = new Event();                // a new, empty object…
    event.setEventId(row.getString("event_id"));   // …filled from the row's columns
    event.setTitle(row.getString("title"));
    // ...
    loaded.add(event);
}
```

A **`ResultSet`** is the table of rows that comes back. We walk through it one row at a time and build
one Java object per row.

### The whole loop

```
 1. App starts      →  SELECT everything       →  build objects in memory
 2. User clicks     →  objects change          →  INSERT / UPDATE / DELETE straight away
 3. App closes      →  memory is wiped         →  but the database still has everything
 4. App starts again → SELECT everything again →  you're back where you left off
```

> **Note for a real-world app:** to keep this class project simple, passwords are stored as plain text.
> Real apps never do this — they store a scrambled "hash" of the password instead.

---

## 8. Glossary

| Word | Meaning |
|---|---|
| **Class** | A blueprint for objects. |
| **Object / instance** | A real thing made from a class with `new`. |
| **Field / attribute** | A piece of data an object holds. |
| **Method** | An action an object can do. |
| **Constructor** | The special method that sets up a new object. |
| **Parameter / argument** | An input to a method (the name in the definition / the value you pass in). |
| **Return value** | What a method gives back. `void` = nothing. |
| **Encapsulation** | Keeping data `private` and only changing it through methods. |
| **Getter / setter** | Methods that read / change one field. |
| **Overloading** | Several methods (or constructors) with the same name but different parameters. |
| **Inheritance** | A class reusing another class with `extends` ("is-a"). |
| **Superclass / subclass** | The parent class / the child class. |
| **Overriding** | A subclass replacing an inherited method (`@Override`). |
| **Polymorphism** | One call working on different object types, each in its own way. |
| **Abstract class** | A parent class that can't be created directly and may leave methods for children to finish. |
| **Interface** | A list of methods a class promises to have (`implements`). |
| **Composition** | An object built from other objects ("has-a"). |
| **Delegation** | An object passing a job to another object it has. |
| **`static`** | Belongs to the class, shared by all objects. |
| **`final`** | Can't be changed once set. |
| **Package** | A folder that groups related classes. |
| **MVC** | Model–View–Controller: splitting data, screens and logic. |
| **Callback** | Code handed to an object so it can run it later (e.g. on a click). |
| **Exception** | An error object that's `throw`n and can be `catch`ed. |
| **Database / table / row / column** | Permanent storage / a spreadsheet-like grid / one record / one property. |
| **SQL** | The language for talking to databases. |
| **JDBC** | Java's built-in toolkit for talking to databases. |
| **Primary key / foreign key** | A row's unique ID / a column pointing at another table's row. |

---

## 9. Every file in one line

| File | What it does |
|---|---|
| `Main.java` | Starts the app: sets the dark theme and fonts, opens the main window. |
| `models/Event.java` | An event and its rules (can it be bought? sell seats, total capacity, cheapest price). |
| `models/TicketTier.java` | One price level with its seats; refuses to oversell. |
| `models/Booking.java` | One purchase — the receipt. |
| `models/User.java` | A host account; checks passwords. |
| `controllers/EventManager.java` | Loads/saves events, tiers and details; sells seats; archives, duplicates, deletes. |
| `controllers/OrderManager.java` | Loads/saves bookings; works out revenue, tickets sold and daily sales. |
| `controllers/AccountManager.java` | Sign up, log in, list and delete host accounts. |
| `database/Database.java` | Opens the PostgreSQL connection and creates the tables. |
| `views/MainFrame.java` | The window; holds every screen and switches between them. |
| `views/WelcomePanel.java` | First screen: "Browse events" or "Start hosting". |
| `views/EventCatalogPanel.java` | Grid of event cards for guests. |
| `views/EventDetailsPanel.java` | One event's page for guests: info, demand chart, ticket choices. |
| `views/CheckoutPanel.java` | Guest details form and purchase. |
| `views/TicketSummaryPanel.java` | The receipt after buying. |
| `views/AdminLoginPanel.java` | Host sign-in and sign-up. |
| `views/AdminDashboardPanel.java` | The host's dashboard frame: sidebar, top bar, page switching. |
| `views/OverviewPage.java` | Dashboard home: totals and revenue chart. |
| `views/EventListPage.java` | Dashboard list of events with Edit / Insights / Delete etc. |
| `views/EventEditorPage.java` | Form to create or edit an event and its tiers. |
| `views/EventAnalyticsPanel.java` | One event's insights: totals, daily sales, seats by tier, latest bookings. |
| `views/SettingsPage.java` | Account details; delete accounts. |
| `views/ChartPanel.java` | Abstract parent of both charts. |
| `views/ColumnChart.java` / `HorizontalBarChart.java` | The two kinds of chart. |
| `views/ViewStyles.java` | Shared colours, fonts and button styles. |
| `views/EventVisuals.java` | Loads and draws event pictures. |
| `views/ScrollableColumn.java` | A panel that scrolls only up and down. |
