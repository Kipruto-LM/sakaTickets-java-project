package models;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class User {
    public static final String DEFAULT_ADMIN_USERNAME = "Talel";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String userId;
    private String name;
    private String email;
    private String username;
    private String passwordSalt;
    private String passwordHash;

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

    public static User defaultAdmin() {
        return new User("admin-001", DEFAULT_ADMIN_USERNAME, "admin@sakatickets.local",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

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

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
