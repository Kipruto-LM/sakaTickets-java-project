// This file belongs to the "database" package: the class that talks to PostgreSQL.
package database;

import java.io.FileInputStream;        // Reads a file from disk
import java.sql.Connection;            // An open link to the database
import java.sql.DriverManager;         // Opens connections using the PostgreSQL driver from pom.xml
import java.sql.Statement;             // Runs a plain SQL command
import java.util.Properties;           // Reads "key=value" files such as db.properties

/**
 * Opens the connection to PostgreSQL and creates the tables the app needs.
 *
 * The login details come from a file called db.properties in the project folder
 * (copy db.properties.example and fill it in). That file is in .gitignore, so the password
 * is never committed.
 *
 * The tables:
 *   hosts          - host accounts
 *   events         - one row per event
 *   ticket_tiers   - the tiers of each event (Early Bird, Regular, VIP, ...)
 *   event_details  - the host's custom details for each event ("Dress code" -> "Smart casual")
 *   bookings       - one row per guest purchase
 *
 * OOP idea: a utility class. It only has a static method, so you call Database.connect()
 * without making a Database object.
 */
public class Database {

    // Reads db.properties, opens a connection, makes sure the tables exist, and returns the connection.
    // The same connection is then shared by the three controllers (passed into their constructors).
    public static Connection connect() {
        try {
            // 1. Read the url, user and password from db.properties.
            Properties settings = new Properties();
            settings.load(new FileInputStream("db.properties"));
            // 2. Open the connection.
            Connection connection = DriverManager.getConnection(
                    settings.getProperty("url"), settings.getProperty("user"), settings.getProperty("password"));
            // 3. Create any tables that don't exist yet.
            createTables(connection);
            return connection;
        } catch (Exception exception) {
            // Any problem (no db.properties, wrong password, PostgreSQL not running...) ends up here.
            throw new RuntimeException("Could not connect to the database.\n" + exception.getMessage());
        }
    }

    // Creates each table if it isn't there yet ("IF NOT EXISTS"), so this is safe to run every start.
    // "throws Exception" passes any error up to connect(), which handles it.
    private static void createTables(Connection connection) throws Exception {
        Statement statement = connection.createStatement();

        // Host accounts. TEXT = any length of text. PRIMARY KEY = unique ID for each row.
        statement.execute("""
                CREATE TABLE IF NOT EXISTS hosts (
                    user_id  TEXT PRIMARY KEY,
                    name     TEXT NOT NULL,
                    email    TEXT NOT NULL,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL
                )""");

        // Events. DATE holds a calendar date. BOOLEAN holds true/false.
        statement.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    event_id          TEXT PRIMARY KEY,
                    host_id           TEXT NOT NULL,
                    title             TEXT NOT NULL,
                    event_date        DATE NOT NULL,
                    venue             TEXT NOT NULL,
                    status            TEXT NOT NULL,
                    category          TEXT NOT NULL,
                    banner_image_path TEXT NOT NULL,
                    archived          BOOLEAN NOT NULL
                )""");

        // Ticket tiers. REFERENCES links each tier to its event (a "foreign key"), and
        // ON DELETE CASCADE removes the tiers automatically when their event is deleted.
        // "position" remembers the order the host listed the tiers in.
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ticket_tiers (
                    event_id         TEXT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                    position         INTEGER NOT NULL,
                    tier_name        TEXT NOT NULL,
                    price            NUMERIC(12, 2) NOT NULL,
                    initial_capacity INTEGER NOT NULL,
                    available_seats  INTEGER NOT NULL
                )""");

        // Custom details ("Dress code" -> "Smart casual"), also deleted with their event.
        statement.execute("""
                CREATE TABLE IF NOT EXISTS event_details (
                    event_id TEXT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                    position INTEGER NOT NULL,
                    name     TEXT NOT NULL,
                    value    TEXT NOT NULL
                )""");

        // Bookings, also deleted with their event. TIMESTAMP holds a date and a time.
        statement.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    booking_id     TEXT PRIMARY KEY,
                    event_id       TEXT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                    customer_name  TEXT NOT NULL,
                    customer_email TEXT NOT NULL,
                    tier_name      TEXT NOT NULL,
                    quantity       INTEGER NOT NULL,
                    total_price    NUMERIC(12, 2) NOT NULL,
                    purchased_at   TIMESTAMP NOT NULL
                )""");
    }
}
