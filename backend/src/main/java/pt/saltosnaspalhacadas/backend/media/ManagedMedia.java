package pt.saltosnaspalhacadas.backend.media;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import pt.saltosnaspalhacadas.backend.user.AppUser;

@Entity
@Table(name = "media_objects")
public class ManagedMedia {

    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ManagedMediaStatus status = ManagedMediaStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "attached_at")
    private Instant attachedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ManagedMedia() { }

    public ManagedMedia(AppUser owner, String storageKey, String contentType, long sizeBytes) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = Math.max(0, sizeBytes);
        this.status = ManagedMediaStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public ManagedMediaStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAttachedAt() { return attachedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public boolean isOwnedBy(AppUser user) {
        return user != null && owner != null && owner.getId() != null && owner.getId().equals(user.getId());
    }

    public void markAttached(Instant attachedAt) {
        this.status = ManagedMediaStatus.ATTACHED;
        this.attachedAt = attachedAt;
    }

    public void markPublic(Instant publishedAt) {
        this.status = ManagedMediaStatus.PUBLIC;
        this.publishedAt = publishedAt;
    }

    public void markDeleted(Instant deletedAt) {
        this.status = ManagedMediaStatus.DELETED;
        this.deletedAt = deletedAt;
    }
}
