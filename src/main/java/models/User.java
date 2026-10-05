package models;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 
 * Represents a Host account profile. 
 * Implements secure password handling by deriving a PBKDF2-HMAC-SHA256 hash 
 * with a unique salt. Passwords are never stored or exposed in plaintext.
 */
public class User {
    // Default system admin credentials used to seed the initial application state
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    
    // Cryptographic constants for PBKDF2 hashing
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Profile data
    private String userId;
    private String name;
    private String email;
    private String username;
    
    // One-way password verifier material (Base64 encoded)
    private String passwordSalt;
    private String passwordHash;

    /** No-argument constructor for bean-style serialization tools. */
    public User() {
    }

    public User(String userId, String name, String email) {
        this(userId, name, email, name, "");
    }

    public User(String userId, String name, String email, String password) {
        this(userId, name, email, name, password);
    }

    /** Constructs a user and immediately secures their plaintext password. */
    public User(String userId, String name, String email, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        setPassword(password);
    }

    /** 
     * Secure constructor that accepts a char[] for the password, allowing the caller 
     * to wipe the array from memory immediately after creation.
     */
    public User(String userId, String name, String email, String username, char[] password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        setPassword(password);
    }

    /** Generates the default administrative account. */
    public static User defaultAdmin() {
        return new User("admin-001", DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    /** 
     * Converts a String password into a char array for authentication. 
     * Using char[] is more secure as strings are immutable and linger in memory.
     */
    public boolean authenticate(String password) {
        if (password == null) {
            return false;
        }
        char[] characters = password.toCharArray();
        try {
            return authenticate(characters);
        } finally {
            Arrays.fill(characters, '\0'); // Wipe temporary password from memory
        }
    }

    /** 
     * Validates a login attempt by hashing the input password with the stored salt 
     * and performing a constant-time comparison against the stored hash.
     */
    public boolean authenticate(char[] password) {
        if (password == null || passwordSalt == null || passwordHash == null) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(passwordSalt);
        byte[] expected = Base64.getDecoder().decode(passwordHash);
        byte[] actual = deriveHash(password, salt);
        
        // MessageDigest.isEqual prevents timing attacks during string comparison
        boolean matches = java.security.MessageDigest.isEqual(expected, actual);
        
        // Zero-out sensitive data arrays
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(expected, (byte) 0);
        Arrays.fill(actual, (byte) 0);
        return matches;
    }

    /** Converts a String to a char array before delegating to the secure setter. */
    public void setPassword(String password) {
        char[] characters = password == null ? new char[0] : password.toCharArray();
        setPassword(characters);
        Arrays.fill(characters, '\0');
    }

    /** 
     * Secures a new password by generating a fresh 16-byte random salt, 
     * calculating the PBKDF2 hash, and storing both as Base64 strings.
     */
    public void setPassword(char[] password) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        this.passwordSalt = Base64.getEncoder().encodeToString(salt);
        
        byte[] hash = deriveHash(password, salt);
        this.passwordHash = Base64.getEncoder().encodeToString(hash);
        
        Arrays.fill(hash, (byte) 0); // Wipe intermediate hash
    }

    /** 
     * Factory method used by the AccountManager to reconstruct a User object from 
     * saved disk storage without requiring the plaintext password.
     */
    public static User fromPasswordHash(String userId, String name, String email, String username,
            String salt, String hash) {
        User user = new User();
        user.userId = userId;
        user.name = name;
        user.email = email;
        user.username = username;
        user.passwordSalt = salt;
        user.passwordHash = hash;
        return user;
    }

    /** 
     * Derives a fixed-length PBKDF2-HMAC-SHA256 verifier key using Java's Cryptography Architecture.
     * Clears the temporary KeySpec from memory automatically.
     */
    private static byte[] deriveHash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword(); // Erases the password char array inside the spec
        }
    }

    // Standard profile accessors
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // Intentionally exposing only the salt and hash, not the plaintext password
    public String getPasswordSalt() { return passwordSalt; }
    public String getPasswordHash() { return passwordHash; }
}