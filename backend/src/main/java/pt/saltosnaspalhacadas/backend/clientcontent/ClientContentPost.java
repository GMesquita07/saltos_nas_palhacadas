package pt.saltosnaspalhacadas.backend.clientcontent;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.user.AppUser;

@Entity
@Table(name = "client_content_posts")
public class ClientContentPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "profile_id")
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

    @Column(length = 800)
    private String caption;

    @Column(name = "media_url", nullable = false, length = 2048)
    private String mediaUrl;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @ManyToOne
    @JoinColumn(name = "media_object_id")
    private ManagedMedia mediaObject;

    @ManyToOne
    @JoinColumn(name = "thumbnail_object_id")
    private ManagedMedia thumbnailObject;

    @Column(name = "public_display_name", nullable = false, length = 80)
    private String publicDisplayName = "Cliente";

    @Column(name = "show_location", nullable = false)
    private boolean showLocation = false;

    @Column(name = "show_event_date", nullable = false)
    private boolean showEventDate = false;

    @Column(name = "consent_version", length = 40)
    private String consentVersion;

    @Column(name = "consented_at")
    private Instant consentedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientContentStatus status = ClientContentStatus.PENDING;

    @Column(name = "admin_message", length = 600)
    private String adminMessage;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ClientContentPost() { }

    public ClientContentPost(
            AppUser user,
            Profile profile,
            MediaType mediaType,
            String title,
            String location,
            LocalDate eventDate,
            String caption,
            String mediaUrl,
            String thumbnailUrl,
            ManagedMedia mediaObject,
            ManagedMedia thumbnailObject,
            String publicDisplayName,
            boolean showLocation,
            boolean showEventDate,
            String consentVersion,
            Instant consentedAt) {
        this.user = user;
        this.profile = profile;
        this.mediaType = mediaType;
        this.title = title;
        this.location = location;
        this.eventDate = eventDate;
        this.caption = caption;
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaObject = mediaObject;
        this.thumbnailObject = thumbnailObject;
        this.publicDisplayName = normalizePublicDisplayName(publicDisplayName);
        this.showLocation = showLocation;
        this.showEventDate = showEventDate;
        this.consentVersion = consentVersion;
        this.consentedAt = consentedAt;
        this.status = ClientContentStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Profile getProfile() { return profile; }
    public MediaType getMediaType() { return mediaType; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public LocalDate getEventDate() { return eventDate; }
    public String getCaption() { return caption; }
    public String getMediaUrl() { return mediaUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public ManagedMedia getMediaObject() { return mediaObject; }
    public ManagedMedia getThumbnailObject() { return thumbnailObject; }
    public String getPublicDisplayName() { return publicDisplayName; }
    public boolean isShowLocation() { return showLocation; }
    public boolean isShowEventDate() { return showEventDate; }
    public String getConsentVersion() { return consentVersion; }
    public Instant getConsentedAt() { return consentedAt; }
    public ClientContentStatus getStatus() { return status; }
    public String getAdminMessage() { return adminMessage; }
    public Instant getModeratedAt() { return moderatedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void moderate(ClientContentStatus status, String adminMessage) {
        this.status = status;
        this.adminMessage = adminMessage == null || adminMessage.isBlank() ? null : adminMessage.trim();
        this.moderatedAt = Instant.now();
    }

    public void updateMediaUrls(String mediaUrl, String thumbnailUrl) {
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    private static String normalizePublicDisplayName(String value) {
        return value == null || value.isBlank() ? "Cliente" : value.trim();
    }
}
