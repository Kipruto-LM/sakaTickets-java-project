// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

import java.security.GeneralSecurityException; // Error type thrown by Java's security classes
import java.security.SecureRandom;            // Random numbers that are safe for security use
import java.util.Arrays;                      // Helpers for arrays, e.g. Arrays.fill()
import java.util.Base64;                      // Turns raw bytes into text (and back) so they can be saved
import javax.crypto.SecretKeyFactory;         // Runs the password-hashing algorithm
import javax.crypto.spec.PBEKeySpec;          // Bundles the password, salt and settings for hashing

/**
 * A host account: someone who can sign in and manage events.
 *
 * How passwords are kept safe (simple version):
 *   1. When a password is set, a random "salt" (16 random bytes) is made.
 *   2. The password + salt go through a slow one-way function (PBKDF2), giving a "hash".
 *   3. Only the salt and the hash are stored - never the password itself.
 *   4. To check a login, the typed password is hashed the same way and the two hashes are compared.
 *
 * OOP ideas shown here:
 * - Encapsulation and information hiding: the real password is never stored. The class
 *   keeps only a salt and a hash, and other classes check passwords through authenticate().
 *   They never see how the checking works.
 * - Static vs instance members: "static" constants and methods belong to the User class
 *   as a whole, while fields like name and email belong to each User object.
 * - Method overloading: authenticate() and setPassword() each come in a String version
 *   and a char[] version. Java picks the right one based on the argument type.
 * - Factory methods: defaultAdmin() and fromPasswordHash() are static methods that
 *   build and return ready-made User objects.
 */
public class User {
    // Class-wide constants ("static final"): shared by every User and never changed.
    // By convention, constant names are written in CAPITAL_LETTERS.
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";        // Built-in host account name
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";     // Its starting password
    private static final int PASSWORD_ITERATIONS = 120_000;  // How many times to repeat hashing (slow = harder to crack)
    private static final int KEY_LENGTH = 256;               // Size of the hash in bits
    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // One shared random generator

    // Instance fields: every User object has its own copy of these.
    private String userId;        // Unique ID, e.g. a random UUID
    private String name;          // Display name, e.g. "Talel"
    private String email;
    private String username;      // What the host types to log in
    private String passwordSalt;  // The random salt, saved as Base64 text
    private String passwordHash;  // The hashed password, saved as Base64 text

    // Several overloaded constructors. The shorter ones fill in defaults and pass the
    // rest of the work on to the full version with this(...).

    // Empty constructor: used by fromPasswordHash() below.
    public User() {
    }

    // Name doubles as the username, and the password is empty.
    public User(String userId, String name, String email) {
        this(userId, name, email, name, "");
    }

    // Name doubles as the username.
    public User(String userId, String name, String email, String password) {
        this(userId, name, email, name, password);
    }

    // Full constructor with the password as a String.
    public User(String userId, String name, String email, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        setPassword(password);   // Never stores the password directly - setPassword() hashes it
    }

    // Full constructor with the password as a char[] (an array of characters).
    // Password boxes in Swing give back char[], which can be wiped from memory after use.
    public User(String userId, String name, String email, String username, char[] password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        setPassword(password);
    }

    // Static factory method: called on the class (User.defaultAdmin()), not on an object.
    // Builds the built-in host account used for the sample events.
    public static User defaultAdmin() {
        return new User("admin-001", DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    // Overloaded authenticate(): this String version converts the password to char[]
    // and calls the char[] version below.
    public boolean authenticate(String password) {
        if (password == null) {      // No password typed: login fails
            return false;
        }
        char[] characters = password.toCharArray();
        try {
            return authenticate(characters);
        } finally {
            // "finally" always runs, even after return: wipe the copy of the password from memory.
            Arrays.fill(characters, '\0');
        }
    }

    // Hashes the typed password with the stored salt and compares it with the stored hash.
    // Returns true only if they match (the password is correct).
    public boolean authenticate(char[] password) {
        // Can't check anything if a piece is missing.
        if (password == null || passwordSalt == null || passwordHash == null) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(passwordSalt);        // Text back into bytes
        byte[] expected = Base64.getDecoder().decode(passwordHash);    // The stored hash
        byte[] actual = deriveHash(password, salt);                    // Hash of what was typed
        // isEqual compares in a way that doesn't leak timing hints to attackers.
        boolean matches = java.security.MessageDigest.isEqual(expected, actual);
        // Wipe the temporary byte arrays.
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(expected, (byte) 0);
        Arrays.fill(actual, (byte) 0);
        return matches;
    }

    // A setter that does more than assign a value: it turns the password into a salt and a hash.
    public void setPassword(String password) {
        // Treat a missing password as an empty one, then use the char[] version.
        char[] characters = password == null ? new char[0] : password.toCharArray();
        setPassword(characters);
        Arrays.fill(characters, '\0');   // Wipe the temporary copy
    }

    // The char[] version does the real work.
    public void setPassword(char[] password) {
        byte[] salt = new byte[16];                                   // Room for 16 random bytes
        SECURE_RANDOM.nextBytes(salt);                                // Fill them with random values
        this.passwordSalt = Base64.getEncoder().encodeToString(salt); // Save the salt as text
        byte[] hash = deriveHash(password, salt);                     // Hash password + salt
        this.passwordHash = Base64.getEncoder().encodeToString(hash); // Save the hash as text
        Arrays.fill(hash, (byte) 0);                                  // Wipe the raw hash bytes
    }

    // Factory method used when loading a saved account: it puts back the stored hash
    // instead of hashing a new password. It is inside User, so it can set private fields directly.
    public static User fromPasswordHash(String userId, String name, String email, String username,
            String salt, String hash) {
        User user = new User();    // Start with an empty User object
        user.userId = userId;
        user.name = name;
        user.email = email;
        user.username = username;
        user.passwordSalt = salt;
        user.passwordHash = hash;
        return user;
    }

    // Private helper (abstraction): only User needs to know how hashing works.
    // Runs PBKDF2 with SHA-256 over the password and salt and returns the hash bytes.
    private static byte[] deriveHash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            // Should never happen on a normal Java install; turn it into an unchecked error.
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword();   // Wipe the password copy held inside spec
        }
    }

    // Ordinary getters and setters for the profile fields (encapsulation).

    // Returns the user's ID.
    public String getUserId() {
        return userId;
    }

    // Changes the user's ID.
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Returns the display name.
    public String getName() {
        return name;
    }

    // Changes the display name.
    public void setName(String name) {
        this.name = name;
    }

    // Returns the email address.
    public String getEmail() {
        return email;
    }

    // Changes the email address.
    public void setEmail(String email) {
        this.email = email;
    }

    // Returns the login username.
    public String getUsername() {
        return username;
    }

    // Changes the login username.
    public void setUsername(String username) {
        this.username = username;
    }

    // Read-only access: there are getters for the salt and hash but no setters,
    // so they can only change through setPassword().
    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
