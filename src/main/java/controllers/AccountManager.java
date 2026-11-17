package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import models.User;

/**
 * Controller for host accounts: validates registration, checks logins, and saves
 * profiles in a small local properties file. SQLite stores event and booking data;
 * host accounts remain in this separate file for the simplified class project.
 *
 * Core concepts:
 * - Encapsulation: the account file and account map are private implementation details.
 * - Composition: the map contains User objects managed by this controller.
 * - Abstraction: views call register/authenticate instead of handling file storage.
 * - Static helpers: username normalization and text validation do not need an object.
 */
public class AccountManager {
    // Keep demo data separate from files written by earlier hashed-password versions.
    private final Path accountFile;

    // A username key points to its User object; names are normalized for case-insensitive lookup.
    private final Map<String, User> accounts = new LinkedHashMap<>();

    /** Stores the demo accounts in the user's home folder, not in the project source tree. */
    public AccountManager() {
        this(Path.of(System.getProperty("user.home"), ".sakatickets", "hosts-demo.properties"));
    }

    /** Loads saved profiles, then adds the built-in account if it is not already present. */
    public AccountManager(Path accountFile) {
        this.accountFile = accountFile.toAbsolutePath();
        try {
            loadAccounts();
            ensureDefaultAdmin();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load local host accounts", exception);
        }
    }

    /**
     * Validates the form values, creates a User, and saves it.
     *
     * @return the newly registered user
     */
    /**
     * Validates input, creates a User, and persists the new profile.
     *
     * @param name display name entered by the host
     * @param email host contact email
     * @param username case-insensitive sign-in name
     * @param password demo password stored as plain text for this class project only
     * @return the newly registered user
     * @throws IllegalArgumentException if the values fail validation or the username is taken
     * @throws IOException if the profile cannot be saved
     */
    public User register(String name, String email, String username, String password)
            throws IOException {
        String cleanName = requireText(name, "Name");
        String cleanEmail = requireText(email, "Email");
        String cleanUsername = requireText(username, "Username");
        String key = accountKey(cleanUsername);
        
        // Form Validation Rules
        if (accounts.containsKey(key)) {
            throw new IllegalArgumentException("That username is already registered.");
        }
        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!cleanUsername.matches("[A-Za-z0-9_.-]{3,32}")) {
            throw new IllegalArgumentException("Username must be 3-32 characters using letters, numbers, '.', '_' or '-'.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        User user = new User(UUID.randomUUID().toString(), cleanName, cleanEmail,
                cleanUsername, password);
        accounts.put(key, user);

        // Do not leave a user registered in memory if saving their profile fails.
        try {
            saveAccounts();
        } catch (IOException exception) {
            accounts.remove(key);
            throw exception;
        }
        return user;
    }

    /** Finds the username without regard to letter case and checks its password. */
    /**
     * Looks up a username without regard to case and checks its password.
     *
     * @return the matching user, or an empty result when the credentials do not match
     */
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        User user = accounts.get(accountKey(username));
        return user != null && user.checkPassword(password) ? Optional.of(user) : Optional.empty();
    }

    /** Adds the sample account when the properties file does not yet contain it. */
    private void ensureDefaultAdmin() throws IOException {
        String key = accountKey(User.DEFAULT_ADMIN_USERNAME);
        if (!accounts.containsKey(key)) {
            User admin = User.defaultAdmin();
            accounts.put(key, admin);
            saveAccounts();
        }
    }

    /** Reads saved profile values and rebuilds the corresponding User objects. */
    private void loadAccounts() throws IOException {
        if (!Files.exists(accountFile)) {
            return; // No file exists yet, skip loading
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(accountFile)) {
            properties.load(input);
        }
        for (String propertyKey : properties.stringPropertyNames()) {
            if (!propertyKey.startsWith("account.") || !propertyKey.endsWith(".username")) {
                continue;
            }
            String prefix = propertyKey.substring(0, propertyKey.length() - ".username".length());
            String username = properties.getProperty(propertyKey);
            String password = properties.getProperty(prefix + ".password");
            if (username == null || password == null) {
                continue;
            }
            User user = new User(
                    properties.getProperty(prefix + ".id"),
                    properties.getProperty(prefix + ".name"),
                    properties.getProperty(prefix + ".email"),
                    username,
                    password);
            accounts.put(accountKey(username), user);
        }
    }

    /** Writes each profile as a small group of related properties. */
    private void saveAccounts() throws IOException {
        Properties properties = new Properties();
        for (User user : accounts.values()) {
            String prefix = "account." + accountKey(user.getUsername());
            properties.setProperty(prefix + ".id", user.getUserId());
            properties.setProperty(prefix + ".name", user.getName());
            properties.setProperty(prefix + ".email", user.getEmail());
            properties.setProperty(prefix + ".username", user.getUsername());
            properties.setProperty(prefix + ".password", user.getPassword());
        }

        Files.createDirectories(accountFile.getParent());
        try (OutputStream output = Files.newOutputStream(accountFile)) {
            properties.store(output, "SakaTickets demo host accounts");
        }
    }

    /** Standardizes usernames to lowercase so "Talel" and "talel" map to the same account. */
    private static String accountKey(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** Simple validation helper to check for blank inputs. */
    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}