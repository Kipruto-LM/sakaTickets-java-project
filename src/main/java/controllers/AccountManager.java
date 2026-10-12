// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import database.DatabaseException;          // Our own error type for database problems
import java.sql.Connection;                 // The open link to PostgreSQL
import java.sql.PreparedStatement;          // A SQL command with "?" blanks we fill in safely
import java.sql.ResultSet;                  // The rows a SELECT query gives back
import java.sql.SQLException;               // Error thrown by Java's database classes
import java.util.ArrayList;                 // A resizable list
import java.util.List;                      // The general list type
import java.util.Optional;                  // A box that may or may not hold a User
import java.util.UUID;                      // Random unique IDs
import models.User;                         // The host account model

/**
 * Controller for host accounts: signing up and signing in. Accounts are stored in the
 * "hosts" table in PostgreSQL.
 *
 * OOP ideas shown here:
 * - MVC (Model-View-Controller): this is the "Controller". The login screen (View) never
 *   touches {@link User} objects' passwords (Model) or the database directly. It asks this class.
 * - Abstraction: callers just call register() or authenticate(). How accounts are stored (SQL)
 *   is hidden inside this class.
 * - Dependency injection: the database Connection is handed in through the constructor,
 *   so this class doesn't need to know how to open one.
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

    // Checks the sign-up details, creates a new User object, and saves it to the database.
    // Bad input is reported by throwing an exception with a friendly message.
    public User register(String name, String email, String username, String password) {
        // Each field must be filled in; requireText also trims extra spaces.
        String cleanName = requireText(name, "Name");
        String cleanEmail = requireText(email, "Email");
        String cleanUsername = requireText(username, "Username");
        // Rule 1: usernames must be unique.
        if (findByUsername(cleanUsername) != null) {
            throw new IllegalArgumentException("That username is already registered.");
        }
        // Rule 2: the email must look like something@something.something (a "regular expression").
        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        // Rule 3: username is 3-32 letters, digits, underscores, dots or dashes.
        if (!cleanUsername.matches("[A-Za-z0-9_.-]{3,32}")) {
            throw new IllegalArgumentException("Username must be 3-32 characters using letters, numbers, '.', '_' or '-'.");
        }
        // Rule 4: password must be at least 8 characters.
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        // All checks passed: create the User (which hashes the password) and save it.
        User user = new User(UUID.randomUUID().toString(), cleanName, cleanEmail, cleanUsername, password);
        saveNewUser(user);
        return user;
    }

    // Finds the user, then asks that User object to check its own password (delegation).
    // Optional means "maybe a User": empty when the login fails.
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        User user = findByUsername(username.trim());
        if (user != null && user.authenticate(password)) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    // All host accounts, sorted by username (for the admin's "Host accounts" list).
    public List<User> getAllHosts() {
        List<User> hosts = new ArrayList<>();
        String sql = "SELECT user_id, name, email, username, password_salt, password_hash FROM hosts ORDER BY LOWER(username)";
        try (PreparedStatement query = connection.prepareStatement(sql); ResultSet row = query.executeQuery()) {
            while (row.next()) {                                // One User object per row
                hosts.add(User.fromPasswordHash(row.getString("user_id"), row.getString("name"),
                        row.getString("email"), row.getString("username"),
                        row.getString("password_salt"), row.getString("password_hash")));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load the host accounts", exception);
        }
        return hosts;
    }

    // Deletes a host account. The built-in admin can't be deleted (it is re-created on every start anyway).
    // The caller should delete the host's events first (EventManager.deleteEventsByHost).
    public void deleteHost(String userId) {
        if (User.DEFAULT_ADMIN_ID.equals(userId)) {
            throw new IllegalArgumentException("The main admin account cannot be deleted.");
        }
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM hosts WHERE user_id = ?")) {
            delete.setString(1, userId);
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not delete the host account", exception);
        }
    }

    // ---- Database code (private: other classes can't call it) ----

    // Looks up one host by username (ignoring capitals). Returns null if there is none.
    private User findByUsername(String username) {
        // "?" is a blank that setString() fills in. Using blanks (never gluing text into SQL)
        // protects against "SQL injection", where someone types SQL into a form field.
        String sql = "SELECT user_id, name, email, username, password_salt, password_hash "
                + "FROM hosts WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, username);                       // Fill blank number 1
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) {                              // next() moves to the first row; false = no rows
                    return null;
                }
                // Rebuild the User from the row (the password stays hashed).
                return User.fromPasswordHash(row.getString("user_id"), row.getString("name"),
                        row.getString("email"), row.getString("username"),
                        row.getString("password_salt"), row.getString("password_hash"));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not look up the host account", exception);
        }
    }

    // Inserts a new host row.
    private void saveNewUser(User user) {
        String sql = "INSERT INTO hosts (user_id, name, email, username, password_salt, password_hash) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setString(1, user.getUserId());
            insert.setString(2, user.getName());
            insert.setString(3, user.getEmail());
            insert.setString(4, user.getUsername());
            insert.setString(5, user.getPasswordSalt());
            insert.setString(6, user.getPasswordHash());
            insert.executeUpdate();                              // Runs the INSERT
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save the host account", exception);
        }
    }

    // Checks a form value isn't empty. Returns it trimmed, or throws an error naming the field.
    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
