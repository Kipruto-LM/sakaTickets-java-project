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
 * Manages host account registration, authentication, and secure local persistence.
 * Uses atomic file operations to ensure the properties file is never corrupted during a crash.
 */
public class AccountManager {
    // The target file location for persisting accounts across application restarts
    private final Path accountFile;
    
    // In-memory cache of accounts, keyed by a normalized (lowercase) username for case-insensitive lookup
    private final Map<String, User> accounts = new LinkedHashMap<>();

    /** 
     * Default constructor uses the operating system's user home directory 
     * to create a hidden '.sakatickets' configuration folder.
     */
    public AccountManager() {
        this(Path.of(System.getProperty("user.home"), ".sakatickets", "hosts.properties"));
    }

    /** Loads existing accounts from disk and ensures the default admin account is seeded. */
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
     * Validates input fields, creates a new User profile, and persists it to disk.
     * Uses synchronized keyword to prevent race conditions during concurrent registrations.
     * 
     * @return The newly registered User object.
     */
    public synchronized User register(String name, String email, String username, char[] password)
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
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        User user = new User(UUID.randomUUID().toString(), cleanName, cleanEmail, cleanUsername, password);
        accounts.put(key, user);
        
        // Attempt to save to disk. If it fails, remove the user from memory to maintain consistency.
        try {
            saveAccounts();
        } catch (IOException exception) {
            accounts.remove(key);
            throw exception;
        }
        return user;
    }

    /** 
     * Searches for the username (case-insensitive) and validates the password.
     * Returns an Optional to cleanly handle failed logins without returning null.
     */
    public synchronized Optional<User> authenticate(String username, char[] password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        User user = accounts.get(accountKey(username));
        return user != null && user.authenticate(password) ? Optional.of(user) : Optional.empty();
    }

    /** 
     * Seeds the application with a default admin account if it's the very first time launching. 
     */
    private void ensureDefaultAdmin() throws IOException {
        String key = accountKey(User.DEFAULT_ADMIN_USERNAME);
        if (!accounts.containsKey(key)) {
            User admin = User.defaultAdmin();
            accounts.put(key, admin);
            saveAccounts();
        }
    }

    /** Reads the properties file from disk and reconstructs the User objects using saved hashes. */
    private void loadAccounts() throws IOException {
        if (!Files.exists(accountFile)) {
            return; // No file exists yet, skip loading
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
            User user = User.fromPasswordHash(
                    properties.getProperty(prefix + ".id"),
                    properties.getProperty(prefix + ".name"), 
                    properties.getProperty(prefix + ".email"),
                    username, 
                    properties.getProperty(prefix + ".salt"), 
                    properties.getProperty(prefix + ".hash"));
            accounts.put(accountKey(username), user);
        }
    }

    /** 
     * Serializes accounts to a temporary file, then performs an atomic swap with the main file.
     * This prevents corrupting the hosts.properties file if the app crashes during a write operation.
     */
    private void saveAccounts() throws IOException {
        Properties properties = new Properties();
        for (User user : accounts.values()) {
            // Encode the username as a safe properties prefix key
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
            // Atomically replace the old file with the new complete temp file
            try {
                Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                // Fallback for file systems that do not support atomic moves
                Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile); // Clean up temp file if something failed
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