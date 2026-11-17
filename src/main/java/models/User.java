package models;

/**
 * A host account that can sign in and manage events.
 *
 * This model demonstrates encapsulation: its data is private, and other classes
 * use getters or checkPassword() instead of changing or inspecting fields directly.
 * Passwords are plain text only to keep this class-project demo simple. Never use
 * this approach for real accounts or real passwords.
 */
public class User {
    // These static constants belong to the User class, not to one particular user.
    public static final String DEFAULT_ADMIN_ID = "admin-001";
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    // Each User object has its own profile values.
    private String userId;
    private String name;
    private String email;
    private String username;
    private String password;

    /** Constructs a complete host profile by copying values into this object. */
    public User(String userId, String name, String email, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    /** Factory method that creates the built-in host account used for the demo. */
    public static User defaultAdmin() {
        return new User(DEFAULT_ADMIN_ID, DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    /** Returns true when the supplied password matches this account's demo password. */
    public boolean checkPassword(String typedPassword) {
        return password != null && password.equals(typedPassword);
    }

    // Getters provide controlled access to this object's private profile data.
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    /** Used by the demo account file; do not use plain-text password storage in real apps. */
    public String getPassword() { return password; }
}
