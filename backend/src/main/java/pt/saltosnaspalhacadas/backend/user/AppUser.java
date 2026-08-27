package pt.saltosnaspalhacadas.backend.user;

import java.time.Instant;
import java.util.Locale;
import jakarta.persistence.*;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(length = 80) private String username;
    @Column(name = "first_name", length = 80) private String firstName;
    @Column(name = "last_name", length = 80) private String lastName;
    @Column(length = 30) private String phone;
    @Column(name = "profile_image_url", length = 2048) private String profileImageUrl;
    @ManyToOne
    @JoinColumn(name = "profile_media_id")
    private ManagedMedia profileMedia;
    @Column(name = "profile_image_position", nullable = false, length = 32) private String profileImagePosition = "50% 50%";
    @Column(name = "profile_image_zoom", nullable = false) private double profileImageZoom = 1.0;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserRole role;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    protected AppUser() { }
    public AppUser(String email, String passwordHash, UserRole role) {
        this(email, inferUsername(email), null, null, null, null, "50% 50%", 1.0, passwordHash, role);
    }
    public AppUser(String email, String username, String firstName, String lastName, String phone, String profileImageUrl, String passwordHash, UserRole role) {
        this(email, username, firstName, lastName, phone, profileImageUrl, "50% 50%", 1.0, passwordHash, role);
    }
    public AppUser(String email, String username, String firstName, String lastName, String phone, String profileImageUrl, String profileImagePosition, double profileImageZoom, String passwordHash, UserRole role) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.username = normalizeUsername(username);
        this.firstName = normalize(firstName);
        this.lastName = normalize(lastName);
        this.phone = normalize(phone);
        this.profileImageUrl = normalize(profileImageUrl);
        this.profileImagePosition = normalizeImagePosition(profileImagePosition);
        this.profileImageZoom = normalizeImageZoom(profileImageZoom);
        this.passwordHash = passwordHash;
        this.role = role;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; } public String getEmail() { return email; } public String getUsername() { return username; } public String getFirstName() { return firstName; } public String getLastName() { return lastName; } public String getPhone() { return phone; } public String getProfileImageUrl() { return profileImageUrl; } public ManagedMedia getProfileMedia() { return profileMedia; } public String getProfileImagePosition() { return profileImagePosition; } public double getProfileImageZoom() { return profileImageZoom; } public String getPasswordHash() { return passwordHash; } public UserRole getRole() { return role; } public boolean isActive() { return active; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; } public Instant getDeletedAt() { return deletedAt; }
    public void updateProfile(String username, String firstName, String lastName, String phone, String profileImageUrl, String profileImagePosition, double profileImageZoom) {
        updateProfile(username, firstName, lastName, phone, profileImageUrl, null, profileImagePosition, profileImageZoom);
    }

    public void updateProfile(String username, String firstName, String lastName, String phone, String profileImageUrl, ManagedMedia profileMedia, String profileImagePosition, double profileImageZoom) {
        this.username = normalizeUsername(username);
        this.firstName = normalize(firstName);
        this.lastName = normalize(lastName);
        this.phone = normalize(phone);
        this.profileImageUrl = normalize(profileImageUrl);
        this.profileMedia = profileMedia;
        this.profileImagePosition = normalizeImagePosition(profileImagePosition);
        this.profileImageZoom = normalizeImageZoom(profileImageZoom);
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void anonymizeForDeletion(String anonymizedEmail, String disabledPasswordHash) {
        this.email = anonymizedEmail.trim().toLowerCase(Locale.ROOT);
        this.username = null;
        this.firstName = null;
        this.lastName = null;
        this.phone = null;
        this.profileImageUrl = null;
        this.profileMedia = null;
        this.profileImagePosition = "50% 50%";
        this.profileImageZoom = 1.0;
        this.passwordHash = disabledPasswordHash;
        this.active = false;
        this.deletedAt = Instant.now();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeUsername(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeImagePosition(String value) {
        return value == null || value.isBlank() ? "50% 50%" : value.trim();
    }

    private static double normalizeImageZoom(double value) {
        if (Double.isNaN(value)) return 1.0;
        return Math.min(3.0, Math.max(1.0, value));
    }

    private static String inferUsername(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        String local = at > 0 ? normalized.substring(0, at) : normalized;
        String safe = local.replaceAll("[^a-z0-9._]", "");
        return safe.isBlank() ? null : safe;
    }
}
