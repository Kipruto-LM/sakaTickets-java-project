// This file belongs to the "controllers" package: the classes that hold the app's logic (the C in MVC).
package controllers;

import java.io.IOException;                          // Error thrown when reading/writing a file fails
import java.io.InputStream;                          // Reads bytes from a file
import java.io.OutputStream;                         // Writes bytes to a file
import java.nio.file.AtomicMoveNotSupportedException; // Thrown if the computer can't do an all-at-once file move
import java.nio.file.Files;                          // Helpers for working with files
import java.nio.file.Path;                           // A file or folder location
import java.nio.file.StandardCopyOption;             // Options for moving/copying files
import java.util.Base64;                             // Turns bytes into safe text
import java.util.LinkedHashMap;                      // A map that keeps insertion order
import java.util.Locale;                             // Language rules, used for lower-casing text
import java.util.Map;                                // The general map (key -> value) type
import java.util.Optional;                           // A box that may or may not hold a value
import java.util.Properties;                         // Simple "key=value" settings files
import java.util.UUID;                               // Random unique IDs
import models.User;                                  // The host account model

/**
 * Controller for host accounts: signing up, signing in, and saving accounts to a file.
 *
 * Accounts are saved in a file called hosts.properties inside a ".sakatickets" folder in the
 * computer user's home folder, so they are still there next time the app starts.
 *
 * OOP ideas shown here:
 * - MVC (Model-View-Controller): this is the "Controller". The login screen (View) never
 *   touches {@link User} objects (Model) or the file directly. It asks this class instead.
 * - Abstraction: callers just call register() or authenticate(). How accounts are stored
 *   (a .properties file, temp files, encoding) is hidden in private methods.
 * - Aggregation: the manager holds a collection of User objects in the accounts map.
 */
public class AccountManager {
    // "final" means these references are set once in the constructor and never replaced.
    private final Path accountFile;      // Where the accounts are saved on disk
    // Key = lower-case username, value = that host's User object.
    private final Map<String, User> accounts = new LinkedHashMap<>();

    // Default constructor: uses the standard file location in the user's home folder.
    // System.getProperty("user.home") gives the home folder, e.g. /home/talel.
    public AccountManager() {
        this(Path.of(System.getProperty("user.home"), ".sakatickets", "hosts.properties"));
    }

    // Overloaded constructor: lets you choose a different file (useful for testing).
    // Loads saved accounts and makes sure the default admin exists.
    public AccountManager(Path accountFile) {
        this.accountFile = accountFile.toAbsolutePath();   // Store the full path
        try {
            loadAccounts();          // Read any accounts saved last time
            ensureDefaultAdmin();    // Add the built-in "Talel" account if it's missing
        } catch (IOException exception) {
            // The app can't work without accounts, so stop with a clear message.
            throw new IllegalStateException("Unable to load local host accounts", exception);
        }
    }

    // Public method (part of the class's interface): checks the input, creates a new User
    // object, and saves it. Bad input is reported by throwing an exception with a message.
    // "synchronized" means only one thread can run this at a time, so two sign-ups can't clash.
    public synchronized User register(String name, String email, String username, char[] password)
            throws IOException {
        // Each field must be filled in; requireText also trims extra spaces.
        String cleanName = requireText(name, "Name");
        String cleanEmail = requireText(email, "Email");
        String cleanUsername = requireText(username, "Username");
        String key = accountKey(cleanUsername);   // Lower-case version used for lookups
        // Rule 1: usernames must be unique.
        if (accounts.containsKey(key)) {
            throw new IllegalArgumentException("That username is already registered.");
        }
        // Rule 2: the email must look like something@something.something.
        // This pattern is a "regular expression" describing that shape.
        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        // Rule 3: username is 3-32 letters, digits, underscores, dots or dashes.
        if (!cleanUsername.matches("[A-Za-z0-9_.-]{3,32}")) {
            throw new IllegalArgumentException("Username must be 3-32 characters using letters, numbers, '.', '_' or '-'.");
        }
        // Rule 4: password must be at least 8 characters.
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        // All checks passed: create the User object with a random unique ID.
        User user = new User(UUID.randomUUID().toString(), cleanName, cleanEmail, cleanUsername, password);
        accounts.put(key, user);       // Add it to the map
        try {
            saveAccounts();            // Write all accounts to the file
        } catch (IOException exception) {
            accounts.remove(key);      // Saving failed: undo the add so memory matches the file
            throw exception;           // Pass the error on to the screen
        }
        return user;
    }

    // Finds the user, then asks that User object to check its own password (delegation).
    // Optional means "maybe a User": empty when the login fails.
    public synchronized Optional<User> authenticate(String username, char[] password) {
        if (username == null || password == null) {
            return Optional.empty();                         // Nothing typed: fail
        }
        User user = accounts.get(accountKey(username));      // null if no such username
        // If the user exists AND the password is right, return it; otherwise return empty.
        return user != null && user.authenticate(password) ? Optional.of(user) : Optional.empty();
    }

    // The private methods below are internal helpers. Other classes can't call them (encapsulation).

    // Makes sure the built-in host account exists, creating and saving it if not.
    private void ensureDefaultAdmin() throws IOException {
        String key = accountKey(User.DEFAULT_ADMIN_USERNAME);
        if (!accounts.containsKey(key)) {
            User admin = User.defaultAdmin();   // Static factory method on User
            accounts.put(key, admin);
            saveAccounts();
        }
    }

    // Reads every account from the file into the accounts map.
    // The file stores each account as several lines like:
    //   account.<code>.username=Talel
    //   account.<code>.salt=...
    private void loadAccounts() throws IOException {
        if (!Files.exists(accountFile)) {
            return;                              // First run: no file yet, nothing to load
        }
        Properties properties = new Properties();
        // "try (...)" is try-with-resources: Java closes the file automatically afterwards.
        try (InputStream input = Files.newInputStream(accountFile)) {
            properties.load(input);
        }
        // Go through every key in the file, but only act on the ".username" lines.
        for (String encodedKey : properties.stringPropertyNames()) {
            if (!encodedKey.startsWith("account.") || !encodedKey.endsWith(".username")) {
                continue;                        // Skip to the next key
            }
            // Cut ".username" off the end to get this account's prefix, e.g. "account.VGFsZWw".
            String prefix = encodedKey.substring(0, encodedKey.length() - ".username".length());
            String username = properties.getProperty(encodedKey);
            // Rebuild the User from its saved pieces (the password stays hashed).
            User user = User.fromPasswordHash(properties.getProperty(prefix + ".id"),
                    properties.getProperty(prefix + ".name"), properties.getProperty(prefix + ".email"),
                    username, properties.getProperty(prefix + ".salt"), properties.getProperty(prefix + ".hash"));
            accounts.put(accountKey(username), user);
        }
    }

    // Writes to a temporary file first, then swaps it in, so a crash can't leave a half-written file.
    private void saveAccounts() throws IOException {
        Properties properties = new Properties();
        for (User user : accounts.values()) {
            // Build a safe prefix for this account from its username (Base64 avoids odd characters).
            String prefix = "account." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(accountKey(user.getUsername()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // One line per field.
            properties.setProperty(prefix + ".id", user.getUserId());
            properties.setProperty(prefix + ".name", user.getName());
            properties.setProperty(prefix + ".email", user.getEmail());
            properties.setProperty(prefix + ".username", user.getUsername());
            properties.setProperty(prefix + ".salt", user.getPasswordSalt());
            properties.setProperty(prefix + ".hash", user.getPasswordHash());
        }
        Files.createDirectories(accountFile.getParent());   // Make the .sakatickets folder if needed
        // Write everything into a new temporary file in the same folder.
        Path temporaryFile = Files.createTempFile(accountFile.getParent(), "hosts-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "SakaTickets host accounts");   // The text is a comment at the top
            }
            try {
                // Replace the real file with the temporary one in a single step.
                Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                // Some systems can't do the single-step move; fall back to a normal move.
                Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);   // Clean up if the temp file is still there
        }
    }

    // Turns a username into its lookup key: no surrounding spaces, all lower-case,
    // so "Talel" and "talel " count as the same account.
    private static String accountKey(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    // Checks a form value isn't empty. Returns it trimmed, or throws an error naming the field.
    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
