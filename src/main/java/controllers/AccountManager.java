// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import java.sql.Connection;                 // The open link to PostgreSQL
import java.sql.PreparedStatement;          // A SQL command with "?" blanks we fill in
import java.sql.ResultSet;                  // The rows a SELECT query gives back
import java.sql.SQLException;               // The error Java's database classes throw
import java.util.ArrayList;                 // A resizable list
import java.util.List;                      // The general list type
import java.util.UUID;                      // Random unique IDs
import models.User;                         // The host account model

/**
 * Controller for host accounts: signing up, signing in, listing and deleting.
 * Accounts are stored in the "hosts" table in PostgreSQL.
 *
 * About "try / catch": Java makes us handle the SQLException that database commands can throw.
 * Each method does that once, by turning it into a RuntimeException with a clear message.
 *
 * OOP ideas shown here:
 * - MVC: this is the "Controller". The screens ask this class instead of using the database.
 * - Abstraction: callers just call register(), login() and so on. The SQL is hidden in here.
 * - Dependency injection: the database Connection is handed in through the constructor.
 */
public class AccountManager {
    private final Connection connection;     // The shared database connection

    // Constructor: keeps the connection and makes sure the built-in admin account exists.
    public AccountManager(Connection connection) {
        this.connection = connection;
        if (findByUsername(User.DEFAULT_ADMIN_USERNAME) == null) {
            saveNewUser(User.defaultAdmin());       // First run: create "Talel" / "admin123"
        }
    }

    // Checks the sign-up details, then creates and saves a new User.
    // If something is wrong, it throws an exception whose message is shown on the form.
    public User register(String name, String email, String username, String password) {
        if (name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Please fill in every field.");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("That username is already registered.");
        }
        User user = new User(UUID.randomUUID().toString(), name.trim(), email.trim(), username.trim(), password);
        saveNewUser(user);
        return user;
    }

    // Returns the User if the username and password are right, or null if they aren't.
    public User login(String username, String password) {
        User user = findByUsername(username.trim());
        if (user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    // All host accounts, sorted by username (for the admin's "Host accounts" list).
    public List<User> getAllHosts() {
        List<User> hosts = new ArrayList<>();
        try {
            PreparedStatement query = connection.prepareStatement("SELECT * FROM hosts ORDER BY username");
            ResultSet row = query.executeQuery();
            while (row.next()) {                                // One loop per row
                hosts.add(userFromRow(row));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load the host accounts: " + exception.getMessage());
        }
        return hosts;
    }

    // Deletes a host account. The built-in admin can't be deleted (it is re-created on every start anyway).
    // The caller should delete the host's events first (EventManager.deleteEventsByHost).
    public void deleteHost(String userId) {
        if (userId.equals(User.DEFAULT_ADMIN_ID)) {
            throw new IllegalArgumentException("The main admin account cannot be deleted.");
        }
        try {
            PreparedStatement delete = connection.prepareStatement("DELETE FROM hosts WHERE user_id = ?");
            delete.setString(1, userId);                        // Fill in blank number 1
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not delete the host account: " + exception.getMessage());
        }
    }

    // ---- Private helpers ----

    // Looks up one host by username (ignoring capital letters). Returns null if there is none.
    private User findByUsername(String username) {
        try {
            // "?" is a blank filled in by setString(). Using blanks means whatever a person types
            // is always treated as data, never as part of the SQL command.
            PreparedStatement query = connection.prepareStatement(
                    "SELECT * FROM hosts WHERE LOWER(username) = LOWER(?)");
            query.setString(1, username);
            ResultSet row = query.executeQuery();
            if (row.next()) {                                   // next() is false when there are no rows
                return userFromRow(row);
            }
            return null;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not look up the host account: " + exception.getMessage());
        }
    }

    // Saves a new host row.
    private void saveNewUser(User user) {
        try {
            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO hosts (user_id, name, email, username, password) VALUES (?, ?, ?, ?, ?)");
            insert.setString(1, user.getUserId());
            insert.setString(2, user.getName());
            insert.setString(3, user.getEmail());
            insert.setString(4, user.getUsername());
            insert.setString(5, user.getPassword());
            insert.executeUpdate();                             // Runs the INSERT
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save the host account: " + exception.getMessage());
        }
    }

    // Turns the current database row into a User object.
    private User userFromRow(ResultSet row) throws SQLException {
        return new User(row.getString("user_id"), row.getString("name"), row.getString("email"),
                row.getString("username"), row.getString("password"));
    }
}
