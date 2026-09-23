package pt.saltosnaspalhacadas.backend.profile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @Column(name = "hero_background_image_url", length = 2048)
    private String heroBackgroundImageUrl;

    @Column(name = "notification_email", length = 254)
    private String notificationEmail;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ProfileSocialLink> socialLinks = new ArrayList<>();

    protected Profile() { }

    public Profile(
            String slug,
            String name,
            String role,
            String description,
            String profileImageUrl) {
        this(slug, name, role, description, profileImageUrl, "50% 50%");
    }

    public Profile(
            String slug,
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition) {
        this(
                slug,
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                null);
    }

    public Profile(
            String slug,
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            String featuredVideoUrl) {
        this(
                slug,
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                1.0,
                featuredVideoUrl);
    }

    public Profile(
            String slug,
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            double profileImageZoom,
            String featuredVideoUrl) {
        this(
                slug,
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                profileImageZoom,
                featuredVideoUrl,
                0);
    }

    public Profile(
            String slug,
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            double profileImageZoom,
            String featuredVideoUrl,
            int displayOrder) {
        this(
                slug,
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                profileImageZoom,
                featuredVideoUrl,
                displayOrder,
                null);
    }

    public Profile(
            String slug,
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            double profileImageZoom,
            String featuredVideoUrl,
            int displayOrder,
            String notificationEmail) {
        this.slug = slug;
        this.name = name;
        this.role = role;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.profileImagePosition = profileImagePosition == null
                ? "50% 50%"
                : profileImagePosition;
        this.profileImageZoom = normalizeZoom(profileImageZoom);
        this.featuredVideoUrl = featuredVideoUrl;
        this.displayOrder = displayOrder;
        this.notificationEmail = normalizeNotificationEmail(notificationEmail);
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

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getDescription() {
        return description;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getProfileImagePosition() {
        return profileImagePosition;
    }

    public double getProfileImageZoom() {
        return profileImageZoom;
    }

    public String getFeaturedVideoUrl() {
        return featuredVideoUrl;
    }

    public String getHeroBackgroundImageUrl() {
        return heroBackgroundImageUrl;
    }

    public void updateHeroBackgroundImageUrl(String heroBackgroundImageUrl) {
        this.heroBackgroundImageUrl = heroBackgroundImageUrl;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public List<ProfileSocialLink> getSocialLinks() {
        return socialLinks.stream()
                .sorted(
                        Comparator.comparingInt(ProfileSocialLink::getDisplayOrder)
                                .thenComparing(
                                        link -> link.getId() == null
                                                ? Long.MAX_VALUE
                                                : link.getId()))
                .toList();
    }

    public List<ProfileSocialLink> getActiveSocialLinks() {
        return getSocialLinks().stream()
                .filter(ProfileSocialLink::isActive)
                .toList();
    }

    public void update(
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition) {
        update(
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                1.0,
                null);
    }

    public void update(
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            String featuredVideoUrl) {
        update(
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                1.0,
                featuredVideoUrl);
    }

    public void update(
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            double profileImageZoom,
            String featuredVideoUrl) {
        update(
                name,
                role,
                description,
                profileImageUrl,
                profileImagePosition,
                profileImageZoom,
                featuredVideoUrl,
                notificationEmail);
    }

    public void update(
            String name,
            String role,
            String description,
            String profileImageUrl,
            String profileImagePosition,
            double profileImageZoom,
            String featuredVideoUrl,
            String notificationEmail) {
        this.name = name;
        this.role = role;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.profileImagePosition = profileImagePosition == null
                ? "50% 50%"
                : profileImagePosition;
        this.profileImageZoom = normalizeZoom(profileImageZoom);
        this.featuredVideoUrl = featuredVideoUrl;
        this.notificationEmail = normalizeNotificationEmail(notificationEmail);
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void replaceSocialLinks(List<ProfileSocialLink> links) {
        socialLinks.clear();

        for (ProfileSocialLink link : links) {
            link.attachTo(this);
            socialLinks.add(link);
        }
    }

    public void deactivate() {
        this.active = false;
    }

    private static double normalizeZoom(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0;
        }

        return Math.min(3.0, Math.max(1.0, value));
    }

    private static String normalizeNotificationEmail(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
