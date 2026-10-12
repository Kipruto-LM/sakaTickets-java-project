// This file belongs to the "models" package: the data classes (the M in MVC).
package models;

/**
 * A host account: someone who can sign in and manage events.
 *
 * Note: to keep this class project simple, the password is stored as plain text.
 * A real app would never do this - it would store a "hash" of the password instead.
 *
 * OOP ideas shown here:
 * - Encapsulation: the fields are private. Other classes use the getters, and check a password
 *   with checkPassword() instead of reading the password themselves.
 * - Static vs instance members: "static" constants and the defaultAdmin() method belong to the
 *   User class as a whole, while fields like name and email belong to each User object.
 * - Factory method: defaultAdmin() is a static method that builds and returns a ready-made User.
 */
public class User {
    // Class-wide constants ("static final"): shared by every User and never changed.
    // By convention, constant names are written in CAPITAL_LETTERS.
    public static final String DEFAULT_ADMIN_ID = "admin-001";          // Built-in host account ID
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";        // Built-in host account name
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";     // Its password

    // Instance fields: every User object has its own copy of these.
    private String userId;        // Unique ID, e.g. a random UUID
    private String name;          // Display name, e.g. "Talel"
    private String email;
    private String username;      // What the host types to log in
    private String password;      // What the host types as their password

    // Constructor: copies each value into this object's fields.
    public User(String userId, String name, String email, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    // Static factory method: called on the class (User.defaultAdmin()), not on an object.
    // Builds the built-in host account used for the sample events.
    public static User defaultAdmin() {
        return new User(DEFAULT_ADMIN_ID, DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    // True if the typed password matches this user's password.
    public boolean checkPassword(String typedPassword) {
        return password.equals(typedPassword);
    }

    // Getters (encapsulation): the public way to read the private fields.
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
