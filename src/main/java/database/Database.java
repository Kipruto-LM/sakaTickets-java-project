// This file belongs to the "database" package: the classes that talk to PostgreSQL.
package database;

import java.io.IOException;            // Error thrown when reading a file fails
import java.io.InputStream;            // Reads bytes from a file
import java.nio.file.Files;            // Helpers for files
import java.nio.file.Path;             // A file location
import java.sql.Connection;            // An open link to the database
import java.sql.DriverManager;         // Opens connections using the JDBC driver from pom.xml
import java.sql.SQLException;          // Error thrown by Java's database classes
import java.sql.Statement;             // Runs a plain SQL command
import java.util.Properties;           // Reads "key=value" files such as db.properties

/**
 * Opens the connection to PostgreSQL and creates the tables the app needs.
 *
 * The login details come from a file called db.properties in the project folder
 * (copy db.properties.example and fill it in). That file is in .gitignore, so each team member
 * keeps their own password out of git.
 *
 * The tables:
 *   hosts          - host accounts (password stored as salt + hash, never plain text)
 *   events         - one row per event
 *   ticket_tiers   - the tiers of each event (Early Bird, Regular, VIP, ...)
 *   event_details  - the host's custom details for each event ("Dress code" -> "Smart casual")
 *   bookings       - one row per guest purchase
 *
 * OOP idea: a utility class. It only has static methods, so you call Database.connect()
 * without making a Database object. The private constructor stops anyone making one.
 */
public final class Database {
    private static final Path SETTINGS_FILE = Path.of("db.properties");   // Looked for in the project folder

    // Private constructor: this class is never turned into an object.
    private Database() {
    }

    // Reads db.properties, opens a connection, makes sure the tables exist, and returns the connection.
    // The same connection is then shared by the three controllers (passed into their constructors).
    public static Connection connect() {
        if (!Files.exists(SETTINGS_FILE)) {
            throw new DatabaseException("Missing db.properties. Copy db.properties.example to db.properties "
                    + "and fill in your PostgreSQL details.", null);
        }
        Properties settings = new Properties();
        try (InputStream input = Files.newInputStream(SETTINGS_FILE)) {      // Closed automatically
            settings.load(input);
        } catch (IOException exception) {
            throw new DatabaseException("Could not read db.properties", exception);
        }
        try {
            Connection connection = DriverManager.getConnection(
                    settings.getProperty("url"), settings.getProperty("user"), settings.getProperty("password"));
            createTables(connection);
            return connection;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not connect to the database at " + settings.getProperty("url")
                    + "\n" + exception.getMessage(), exception);
        }
    }

    // Creates each table if it isn't there yet ("IF NOT EXISTS"), so this is safe to run every start.
    private static void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Host accounts. TEXT = any length of text. PRIMARY KEY = unique ID for each row.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hosts (
                        user_id       TEXT PRIMARY KEY,
                        name          TEXT NOT NULL,
                        email         TEXT NOT NULL,
                        username      TEXT NOT NULL,
                        password_salt TEXT NOT NULL,
                        password_hash TEXT NOT NULL
                    )""");
            // Usernames must be unique, ignoring capitals ("Talel" and "talel" are the same).
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS hosts_username_key ON hosts (LOWER(username))");

            // Events. DATE columns hold a calendar date; sales dates may be empty (NULL).
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS events (
                        event_id          TEXT PRIMARY KEY,
                        host_id           TEXT NOT NULL,
                        title             TEXT NOT NULL,
                        event_date        DATE NOT NULL,
                        venue             TEXT NOT NULL,
                        status            TEXT NOT NULL,
                        category          TEXT NOT NULL,
                        banner_image_path TEXT NOT NULL DEFAULT '',
                        sales_start       DATE,
                        sales_end         DATE,
                        archived          BOOLEAN NOT NULL DEFAULT FALSE
                    )""");

            // Ticket tiers. REFERENCES links each tier to its event (a "foreign key");
            // ON DELETE CASCADE removes the tiers if the event is ever deleted.
            // The CHECK rule means seats can never go below zero, even if two people buy at once.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_tiers (
                        event_id         TEXT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                        position         INTEGER NOT NULL,
                        tier_name        TEXT NOT NULL,
                        price            NUMERIC(12, 2) NOT NULL,
                        initial_capacity INTEGER NOT NULL,
                        available_seats  INTEGER NOT NULL CHECK (available_seats >= 0),
                        PRIMARY KEY (event_id, tier_name)
                    )""");

            // Custom details ("Dress code" -> "Smart casual"), kept in the order the host added them.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS event_details (
                        event_id TEXT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                        position INTEGER NOT NULL,
                        name     TEXT NOT NULL,
                        value    TEXT NOT NULL,
                        PRIMARY KEY (event_id, name)
                    )""");

            // Bookings. TIMESTAMP holds a date and a time.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        booking_id     TEXT PRIMARY KEY,
                        event_id       TEXT NOT NULL REFERENCES events(event_id),
                        customer_name  TEXT NOT NULL,
                        customer_email TEXT NOT NULL,
                        tier_name      TEXT NOT NULL,
                        quantity       INTEGER NOT NULL,
                        total_price    NUMERIC(12, 2) NOT NULL,
                        purchased_at   TIMESTAMP NOT NULL
                    )""");
        }
    }
}
