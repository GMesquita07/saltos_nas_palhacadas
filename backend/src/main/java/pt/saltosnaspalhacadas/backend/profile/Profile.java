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

    @Column(name = "profile_image_zoom", nullable = false)
    private double profileImageZoom = 1.0;

    @Column(name = "featured_video_url", length = 2048)
    private String featuredVideoUrl;

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
        this(slug, name, role, description, profileImageUrl, profileImagePosition, null);
    }

    public Profile(String slug, String name, String role, String description, String profileImageUrl, String profileImagePosition, String featuredVideoUrl) {
        this(slug, name, role, description, profileImageUrl, profileImagePosition, 1.0, featuredVideoUrl);
    }

    public Profile(String slug, String name, String role, String description, String profileImageUrl, String profileImagePosition, double profileImageZoom, String featuredVideoUrl) {
        this.slug = slug;
        this.name = name;
        this.role = role;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.profileImagePosition = profileImagePosition == null ? "50% 50%" : profileImagePosition;
        this.profileImageZoom = normalizeZoom(profileImageZoom);
        this.featuredVideoUrl = featuredVideoUrl;
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
    public double getProfileImageZoom() { return profileImageZoom; }
    public String getFeaturedVideoUrl() { return featuredVideoUrl; }
    public boolean isActive() { return active; }
    public void update(String name, String role, String description, String profileImageUrl, String profileImagePosition) {
        update(name, role, description, profileImageUrl, profileImagePosition, 1.0, null);
    }
    public void update(String name, String role, String description, String profileImageUrl, String profileImagePosition, String featuredVideoUrl) {
        update(name, role, description, profileImageUrl, profileImagePosition, 1.0, featuredVideoUrl);
    }
    public void update(String name, String role, String description, String profileImageUrl, String profileImagePosition, double profileImageZoom, String featuredVideoUrl) {
        this.name = name; this.role = role; this.description = description; this.profileImageUrl = profileImageUrl;
        this.profileImagePosition = profileImagePosition == null ? "50% 50%" : profileImagePosition;
        this.profileImageZoom = normalizeZoom(profileImageZoom);
        this.featuredVideoUrl = featuredVideoUrl;
    }

    private static double normalizeZoom(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 1.0;
        return Math.min(3.0, Math.max(1.0, value));
    }
}
