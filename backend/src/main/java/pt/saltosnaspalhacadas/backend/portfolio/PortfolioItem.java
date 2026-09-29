package pt.saltosnaspalhacadas.backend.portfolio;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pt.saltosnaspalhacadas.backend.profile.Profile;

@Entity
@Table(name = "portfolio_items")
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 180)
    private String location;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "media_url", nullable = false, length = 2048)
    private String mediaUrl;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "thumbnail_position", nullable = false, length = 20)
    private String thumbnailPosition = "50% 50%";

    @Column(name = "thumbnail_zoom", nullable = false)
    private double thumbnailZoom = 1.0;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PortfolioItem() { }

    public PortfolioItem(Profile profile, MediaType mediaType, String title, String location, LocalDate eventDate, String mediaUrl, String thumbnailUrl, int displayOrder, boolean published) {
        this(profile, mediaType, title, location, eventDate, mediaUrl, thumbnailUrl, "50% 50%", 1.0, displayOrder, published);
    }

    public PortfolioItem(Profile profile, MediaType mediaType, String title, String location, LocalDate eventDate, String mediaUrl, String thumbnailUrl, String thumbnailPosition, double thumbnailZoom, int displayOrder, boolean published) {
        this.profile = profile;
        this.mediaType = mediaType;
        this.title = title;
        this.location = location;
        this.eventDate = eventDate;
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailPosition = thumbnailPosition == null ? "50% 50%" : thumbnailPosition;
        this.thumbnailZoom = normalizeZoom(thumbnailZoom);
        this.displayOrder = displayOrder;
        this.published = published;
    }

    @jakarta.persistence.PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @jakarta.persistence.PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Profile getProfile() { return profile; }
    public MediaType getMediaType() { return mediaType; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public LocalDate getEventDate() { return eventDate; }
    public String getMediaUrl() { return mediaUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getThumbnailPosition() { return thumbnailPosition; }
    public double getThumbnailZoom() { return thumbnailZoom; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isPublished() { return published; }

    public void update(MediaType mediaType, String title, String location, LocalDate eventDate, String mediaUrl, String thumbnailUrl, boolean published) {
        update(mediaType, title, location, eventDate, mediaUrl, thumbnailUrl, thumbnailPosition, thumbnailZoom, published);
    }

    public void update(MediaType mediaType, String title, String location, LocalDate eventDate, String mediaUrl, String thumbnailUrl, String thumbnailPosition, double thumbnailZoom, boolean published) {
        this.mediaType = mediaType;
        this.title = title;
        this.location = location;
        this.eventDate = eventDate;
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailPosition = thumbnailPosition == null ? "50% 50%" : thumbnailPosition;
        this.thumbnailZoom = normalizeZoom(thumbnailZoom);
        this.published = published;
    }

    private static double normalizeZoom(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0;
        }

        return Math.min(3.0, Math.max(1.0, value));
    }
}
