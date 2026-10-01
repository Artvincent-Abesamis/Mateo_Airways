package airline;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Base class for every account type. Fields are private (encapsulation); role is supplied by subclasses. */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private boolean active = true;

    protected User(String username, String password, String fullName, String email) {
        this.username = username;
        this.passwordHash = hash(password);
        this.fullName = fullName;
        this.email = email;
    }

    public abstract Role getRole();

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(boolean active) { this.active = active; }
    public void changePassword(String newPassword) { this.passwordHash = hash(newPassword); }

    public boolean checkPassword(String raw) { return passwordHash.equals(hash(raw)); }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return String.format("%-14s %-22s %-10s %-26s %s",
                username, fullName, getRole(), email, active ? "Active" : "Deactivated");
    }
}
