package models;

/**
 * A host account that can sign in and manage events.
 *
 * Core concepts:
 * - Encapsulation: private profile data is read through getters and checkPassword().
 * - Static factory method: defaultAdmin() creates the built-in demo host.
 * - Class-level constants: static final values identify the built-in account.
 * - Object state: each User instance stores one host's profile.
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

    // Getters provide controlled read access to this object's private profile data.
    /** Returns the unique ID used to associate this host with events. */
    public String getUserId() { return userId; }
    /** Returns the host's display name. */
    public String getName() { return name; }
    /** Returns the host's contact email. */
    public String getEmail() { return email; }
    /** Returns the host's sign-in name. */
    public String getUsername() { return username; }
    /** Used by the demo account file; do not use plain-text password storage in real apps. */
    public String getPassword() { return password; }
}
