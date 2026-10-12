package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import models.User;

/**
 * Controller for host accounts: signing up, signing in, and saving accounts to a file.
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
    private final Path accountFile;
    // Key = lower-case username, value = that host's User object.
    private final Map<String, User> accounts = new LinkedHashMap<>();

    // Default constructor: uses the standard file location in the user's home folder.
    public AccountManager() {
        this(Path.of(System.getProperty("user.home"), ".sakatickets", "hosts.properties"));
    }

    // Overloaded constructor: lets you choose a different file (useful for testing).
    // Loads saved accounts and makes sure the default admin exists.
    public AccountManager(Path accountFile) {
        this.accountFile = accountFile.toAbsolutePath();
        try {
            loadAccounts();
            ensureDefaultAdmin();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load local host accounts", exception);
        }
    }

    // Public method (part of the class's interface): checks the input, creates a new User
    // object, and saves it. Bad input is reported by throwing an exception with a message.
    public synchronized User register(String name, String email, String username, char[] password)
            throws IOException {
        String cleanName = requireText(name, "Name");
        String cleanEmail = requireText(email, "Email");
        String cleanUsername = requireText(username, "Username");
        String key = accountKey(cleanUsername);
        if (accounts.containsKey(key)) {
            throw new IllegalArgumentException("That username is already registered.");
        }
        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!cleanUsername.matches("[A-Za-z0-9_.-]{3,32}")) {
            throw new IllegalArgumentException("Username must be 3-32 characters using letters, numbers, '.', '_' or '-'.");
        }
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        User user = new User(UUID.randomUUID().toString(), cleanName, cleanEmail, cleanUsername, password);
        accounts.put(key, user);
        try {
            saveAccounts();
        } catch (IOException exception) {
            accounts.remove(key);
            throw exception;
        }
        return user;
    }

    // Finds the user, then asks that User object to check its own password (delegation).
    // Optional means "maybe a User": empty when the login fails.
    public synchronized Optional<User> authenticate(String username, char[] password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        User user = accounts.get(accountKey(username));
        return user != null && user.authenticate(password) ? Optional.of(user) : Optional.empty();
    }

    // The private methods below are internal helpers. Other classes can't call them (encapsulation).
    private void ensureDefaultAdmin() throws IOException {
        String key = accountKey(User.DEFAULT_ADMIN_USERNAME);
        if (!accounts.containsKey(key)) {
            User admin = User.defaultAdmin();
            accounts.put(key, admin);
            saveAccounts();
        }
    }

    private void loadAccounts() throws IOException {
        if (!Files.exists(accountFile)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(accountFile)) {
            properties.load(input);
        }
        for (String encodedKey : properties.stringPropertyNames()) {
            if (!encodedKey.startsWith("account.") || !encodedKey.endsWith(".username")) {
                continue;
            }
            String prefix = encodedKey.substring(0, encodedKey.length() - ".username".length());
            String username = properties.getProperty(encodedKey);
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
            String prefix = "account." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(accountKey(user.getUsername()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            properties.setProperty(prefix + ".id", user.getUserId());
            properties.setProperty(prefix + ".name", user.getName());
            properties.setProperty(prefix + ".email", user.getEmail());
            properties.setProperty(prefix + ".username", user.getUsername());
            properties.setProperty(prefix + ".salt", user.getPasswordSalt());
            properties.setProperty(prefix + ".hash", user.getPasswordHash());
        }
        Files.createDirectories(accountFile.getParent());
        Path temporaryFile = Files.createTempFile(accountFile.getParent(), "hosts-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "SakaTickets host accounts");
            }
            try {
                Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String accountKey(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}