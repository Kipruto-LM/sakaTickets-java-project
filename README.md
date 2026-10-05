# SakaTickets — System Documentation & Setup Guide

SakaTickets is a modern, feature-rich Java desktop event ticketing application built with **Java Swing**, styled using **FlatLaf**[cite: 16], and designed around a strict Object-Oriented **MVC (Model-View-Controller)** architecture. It supports dual user workflows: public guests who can browse events and seamlessly buy tickets without accounts, and administrative event hosts who manage live inventory, ticket tiers, and revenue analytics.

---

## 1. System Architecture & Core Modules

The project is cleanly separated into decoupled packages to ensure maintainability and adherence to software engineering best practices:

*   **`models` (Data Layer)**: Contains core entities representing business objects.
    *   `Event.java`: Manages schedule details, host ownership, lifecycle status (`ON_SALE`, `SOLD_OUT`, `PAUSED`, `CANCELLED`), and a collection of `TicketTier` objects[cite: 6].
    *   `TicketTier.java`: Handles multi-tier pricing and capacity management (e.g., VIP vs. Regular)[cite: 8].
    *   `Booking.java`: Represents a guest's digital receipt, tracking quantities, total charges, and a unique 8-character UUID reference[cite: 5].
    *   `User.java`: Secure host account profile leveraging PBKDF2-HMAC-SHA256 password hashing with a random salt.
    *   `Ticket.java`: Retained legacy model for backward compatibility[cite: 7].
*   **`controllers` (Business Logic Layer)**: Manages state, persistence, and computation.
    *   `EventManager.java`: Owns the event catalog, filters public unarchived events, and handles cloning/updating[cite: 3].
    *   `OrderManager.java`: Records guest bookings and calculates live event revenue and sell-through percentages[cite: 4].
    *   `AccountManager.java`: Handles host authentication, credential validation, and atomic local file persistence (`hosts.properties`)[cite: 2].
*   **`views` (Presentation Layer)**: Java Swing UI components managed via a centralized `CardLayout` inside `MainFrame.java`[cite: 15].
    *   `WelcomePanel.java`: First-run role chooser directing users to either guest browsing or host login[cite: 19].
    *   `EventCatalogPanel.java`: Public grid view featuring responsive image cropping and direct ticket purchasing options[cite: 13].
    *   `CheckoutPanel.java`: Guest-facing purchase form with live subtotal calculation and tier dropdowns[cite: 12].
    *   `TicketSummaryPanel.java`: Digital receipt confirmation screen displaying the unique transaction UUID[cite: 17].
    *   `AdminLoginPanel.java`: Secure tabbed interface for host sign-in and registration[cite: 11].
    *   `AdminDashboardPanel.java`: Comprehensive workspace for hosts to track real-time analytics, modify ticket tiers, export CSV attendee lists, and configure custom styling[cite: 10].
    *   `ThemeManager.java` & `ViewStyles.java`: Global engine managing Light/Dark mode transitions, user preference persistence, and CSS-style button hover states[cite: 16, 18].

---

## 2. Key Technical Features

*   **Guest Checkout Workflow:** Removes friction for buyers by eliminating mandatory account registration[cite: 5]. Transactions instantly generate a readable 8-character confirmation code[cite: 4].
*   **Secure Credential Storage:** Host passwords are never stored in plaintext. They are encrypted using Java's cryptographic libraries, and local data writes use atomic file replacement (`hosts.properties`) to protect against corruption during crashes[cite: 2].
*   **Adaptive 1080p Graphics Engine (`EventVisuals`):** Utilizes custom `Graphics2D` rendering and bicubic aspect-ratio scaling to dynamically fit event imagery across monitors without vertical clipping[cite: 14].
*   **Dynamic Theme Engine:** Instantly toggles between Dark and Light modes, saving user preferences locally via Java Preferences while dynamically remapping custom component surfaces and borders[cite: 16].

---

## 3. Local Setup & Installation Guide

### Prerequisites
*   **Java Development Kit (JDK):** Version 17 or higher installed on your machine.
*   **Git:** For cloning the source code repository.

---

### Method A: Setting up in Visual Studio Code (VS Code)

1. **Clone the Repository**
   Open your terminal or command prompt and clone the project:
   ```bash
   git clone [https://github.com/yourusername/sakatickets.git](https://github.com/yourusername/sakatickets.git)
   cd sakatickets