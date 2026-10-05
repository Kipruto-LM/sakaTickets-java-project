# SakaTickets

SakaTickets is a Java Swing desktop app for discovering events, booking guest tickets, and managing hosted events. It uses Maven, FlatLaf, and JCalendar.

## Requirements

- JDK 25
- Apache Maven 3.9 or newer
- VS Code with the Extension Pack for Java, or Apache NetBeans with Maven support

Check the installed tools:

```bash
java -version
mvn -version
```

## Get the Project

Clone the GitHub repository and enter its directory:

```bash
git clone https://github.com/Kipruto-LM/sakaTickets-java-project.git
cd sakaTickets-java-project
```

If using SSH keys with GitHub, you can use:

```bash
git clone git@github.com:Kipruto-LM/sakaTickets-java-project.git
```

## Build

From the project root, download dependencies and compile the app:

```bash
mvn clean compile
```

Run the Maven test lifecycle with:

```bash
mvn test
```

Build a distributable Maven package with:

```bash
mvn package
```

## Run in VS Code

1. Open the cloned project folder with **File > Open Folder**.
2. Install the **Extension Pack for Java** if it is not already installed.
3. Wait for the Java and Maven projects to finish importing.
4. Open `src/main/java/Main.java` and choose **Run** above `main` (or press **F5** to debug).
5. If VS Code asks to configure the Java runtime, select JDK 25. Do not add Maven source folders manually; `pom.xml` manages the project.

## Run in Apache NetBeans

1. Choose **File > Open Project** and select the cloned project directory containing `pom.xml`.
2. Allow NetBeans to load Maven dependencies.
3. Open `src/main/java/Main.java` and choose **Run File**, or right-click the project and select **Run**.
4. If prompted for the main class, enter `Main`. Configure the project to use JDK 25 in **Project Properties > Libraries** if needed.

## Host Login

The first run seeds the demo host account:

- Username: `Talel`
- Password: `admin123`

Hosts can also create accounts from **Host an event > Create Host Account**. Account records are saved locally in `~/.sakatickets/hosts.properties` with salted PBKDF2 password hashes. This file is per computer and is not part of the Git repository.

## Appearance and Event Data

The app starts in the saved appearance mode. Hosts can switch between light and dark modes and choose a custom interface accent in **Settings**. These preferences are saved locally using Java Preferences. Event photos are bundled under `src/main/resources/images` so they are available offline; image panels adapt their height to the available width to reduce cropping on wide displays.

Hosts can also customize each event with its own category, banner image, accent color, custom detail fields, ticket tiers, and sales window from **Create / Edit Event**.

For this class-project MVP, host accounts persist locally, while events and bookings are held in memory and reset when the application closes.

## Project Layout

- `src/main/java/Main.java` starts the Swing application and applies saved appearance preferences.
- `src/main/java/models/` contains event, ticket-tier, booking, user, and ticket data models.
- `src/main/java/controllers/` manages local host accounts, event catalog operations, and booking analytics.
- `src/main/java/views/` contains the CardLayout frame, buyer/host screens, event editor, checkout, and shared theme/image helpers.
- `src/main/resources/images/` contains the bundled event photography used by the screens.
- `pom.xml` defines the Java version and FlatLaf/JCalendar dependencies.
