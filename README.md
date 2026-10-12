# SakaTickets

A Java desktop app for buying and selling event tickets in Nairobi, built with **Java Swing** and
**PostgreSQL** as a first-year Object-Oriented Programming group project.

## What it does

- **Guests** browse upcoming events, open an event's page, pick a ticket tier (Early Bird, Regular, VIP),
  and buy tickets — no account needed. They get a receipt with a booking code.
- **Hosts** log in to a dashboard where they create, edit, archive and delete events, set up ticket tiers,
  see sales charts, view attendees and export them to CSV.
- Everything is **saved in PostgreSQL**, so it's still there next time you open the app.

## Get started

👉 **[SETUP.md](SETUP.md)** — step-by-step install guide for Windows + VS Code
(Git, JDK 25, PostgreSQL, and running the app).

**Default host login:** username `Talel`, password `admin123`

## Learn how it works

👉 **[HOW_IT_WORKS.md](HOW_IT_WORKS.md)** — a beginner-friendly tour of the code: the OOP ideas it uses
(classes, constructors, inheritance, polymorphism…), how the pieces fit together, and how the app talks
to the database.

## Project layout

```
src/main/java/
├── Main.java       starts the app
├── models/         the data: Event, TicketTier, Booking, User
├── controllers/    the logic: EventManager, OrderManager, AccountManager
├── database/       the PostgreSQL connection and tables
└── views/          the screens (Swing panels and charts)
```

## Built with

| Tool | Used for |
|---|---|
| Java 25 + Swing | The app and its screens |
| FlatLaf | The dark look |
| JCalendar | The date picker |
| PostgreSQL + JDBC | Saving data |
| Maven (`pom.xml`) | Downloading the libraries above |

> To keep this class project simple, host passwords are stored as plain text.
> A real app would store a scrambled "hash" instead.
