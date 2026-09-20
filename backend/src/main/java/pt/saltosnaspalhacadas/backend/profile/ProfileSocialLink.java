package pt.saltosnaspalhacadas.backend.profile;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "profile_social_links")
public class ProfileSocialLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(nullable = false, length = 40)
    private String platform;

    @Column(length = 80)
    private String label;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProfileSocialLink() {
    }

    public ProfileSocialLink(String platform, String label, String url, int displayOrder) {
        this.platform = platform;
        this.label = label;
        this.url = url;
        this.displayOrder = displayOrder;
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
    public Profile getProfile() { return profile; }
    public String getPlatform() { return platform; }
    public String getLabel() { return label; }
    public String getUrl() { return url; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }

    void attachTo(Profile profile) {
        this.profile = profile;
    }

    public void deactivate() {
        this.active = false;
    }
}
