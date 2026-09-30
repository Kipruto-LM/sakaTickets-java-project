package models;

public class User {
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private String userId;
    private String name;
    private String email;
    private String password;

    public User() {
    }

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = "";
    }

    public User(String userId, String name, String email, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public static User defaultAdmin() {
        return new User("admin-001", DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local", DEFAULT_ADMIN_PASSWORD);
    }

    public boolean authenticate(String password) {
        return password != null && password.equals(this.password);
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
