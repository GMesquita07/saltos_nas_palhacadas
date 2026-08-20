package pt.saltosnaspalhacadas.backend.profile;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String role;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    @Column(name = "profile_image_position", nullable = false, length = 20)
    private String profileImagePosition = "50% 50%";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Profile() { }

    public Profile(String slug, String name, String role, String description, String profileImageUrl) {
        this(slug, name, role, description, profileImageUrl, "50% 50%");
    }

    public Profile(String slug, String name, String role, String description, String profileImageUrl, String profileImagePosition) {
        this.slug = slug;
        this.name = name;
        this.role = role;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.profileImagePosition = profileImagePosition == null ? "50% 50%" : profileImagePosition;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getDescription() { return description; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getProfileImagePosition() { return profileImagePosition; }
    public boolean isActive() { return active; }
}
