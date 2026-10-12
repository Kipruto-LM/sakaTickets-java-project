package models;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A host account: someone who can sign in and manage events.
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
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Instance fields: every User object has its own copy of these.
    private String userId;
    private String name;
    private String email;
    private String username;
    private String passwordSalt;
    private String passwordHash;

    // Several overloaded constructors. The shorter ones fill in defaults and pass the
    // rest of the work on to the full version with this(...).
    public User() {
    }

    public User(String userId, String name, String email) {
        this(userId, name, email, name, "");
    }

    public User(String userId, String name, String email, String password) {
        this(userId, name, email, name, password);
    }

    public User(String userId, String name, String email, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        setPassword(password);
    }

    public User(String userId, String name, String email, String username, char[] password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        setPassword(password);
    }

    // Static factory method: called on the class (User.defaultAdmin()), not on an object.
    public static User defaultAdmin() {
        return new User("admin-001", DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    // Overloaded authenticate(): this String version converts the password to char[]
    // and calls the char[] version below.
    public boolean authenticate(String password) {
        if (password == null) {
            return false;
        }
        char[] characters = password.toCharArray();
        try {
            return authenticate(characters);
        } finally {
            Arrays.fill(characters, '\0');
        }
    }

    // Hashes the typed password with the stored salt and compares it with the stored hash.
    public boolean authenticate(char[] password) {
        if (password == null || passwordSalt == null || passwordHash == null) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(passwordSalt);
        byte[] expected = Base64.getDecoder().decode(passwordHash);
        byte[] actual = deriveHash(password, salt);
        boolean matches = java.security.MessageDigest.isEqual(expected, actual);
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(expected, (byte) 0);
        Arrays.fill(actual, (byte) 0);
        return matches;
    }

    // A setter that does more than assign a value: it turns the password into a salt and a hash.
    public void setPassword(String password) {
        char[] characters = password == null ? new char[0] : password.toCharArray();
        setPassword(characters);
        Arrays.fill(characters, '\0');
    }

    public void setPassword(char[] password) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        this.passwordSalt = Base64.getEncoder().encodeToString(salt);
        byte[] hash = deriveHash(password, salt);
        this.passwordHash = Base64.getEncoder().encodeToString(hash);
        Arrays.fill(hash, (byte) 0);
    }

    // Factory method used when loading a saved account: it puts back the stored hash
    // instead of hashing a new password. It is inside User, so it can set private fields directly.
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

    // Private helper (abstraction): only User needs to know how hashing works.
    private static byte[] deriveHash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

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
