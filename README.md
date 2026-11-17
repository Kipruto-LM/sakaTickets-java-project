# SakaTickets (dev)

A Java Swing class-project demo for browsing events, buying tickets, and managing hosted events.
This branch keeps the application local and in-memory where possible; it does not use a database.

## Run

Requirements: JDK 25 and Maven 3.9 or newer.

```bash
mvn clean compile
```

To launch it, open the project containing `pom.xml` in your Java IDE and run `src/main/java/Main.java`.

The built-in demo host login is **Talel / admin123**. Hosts can also create an account from the
login screen. Host accounts are saved in `~/.sakatickets/hosts-demo.properties`; events and bookings
are kept in memory and reset when the app closes. Existing `hosts.properties` files from the older
hashed-password version are left untouched and are not imported.

> **Demo-only security:** to keep password handling simple for an OOP class project, this branch
> stores and compares passwords as plain text. Do not use real credentials or use this design for
> an application exposed to other people.

## Learn the code

See [HOW_IT_WORKS.md](HOW_IT_WORKS.md) for a beginner-friendly map of the screens, data flow, and
OOP concepts used in this branch.

## Project layout

- `src/main/java/Main.java` starts the application.
- `src/main/java/models/` defines the data objects: `Event`, `TicketTier`, `Booking`, and `User`.
- `src/main/java/controllers/` manages account, event, and booking operations.
- `src/main/java/views/` contains the Swing screens, screen navigation, and shared styling.
- `src/main/resources/images/` contains event images shown by the interface.
- `pom.xml` declares the Java version and Swing UI libraries.
