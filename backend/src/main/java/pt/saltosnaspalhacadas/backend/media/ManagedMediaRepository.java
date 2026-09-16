package pt.saltosnaspalhacadas.backend.media;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManagedMediaRepository extends JpaRepository<ManagedMedia, UUID> {
    Optional<ManagedMedia> findByIdAndDeletedAtIsNull(UUID id);

    Optional<ManagedMedia> findByStorageKeyAndDeletedAtIsNull(String storageKey);

    long countByOwnerIdAndStatusInAndDeletedAtIsNull(Long ownerId, Collection<ManagedMediaStatus> statuses);

    long countByOwnerIdAndPurposeAndStatusInAndDeletedAtIsNull(Long ownerId, ManagedMediaPurpose purpose, Collection<ManagedMediaStatus> statuses);

    @Query("""
            select coalesce(sum(media.sizeBytes), 0)
            from ManagedMedia media
            where media.owner.id = :ownerId
              and media.status in :statuses
              and media.deletedAt is null
            """)
    long sumSizeBytesByOwnerIdAndStatusIn(
            @Param("ownerId") Long ownerId,
            @Param("statuses") Collection<ManagedMediaStatus> statuses);

    @Query("""
            select coalesce(sum(media.sizeBytes), 0)
            from ManagedMedia media
            where media.owner.id = :ownerId
              and media.purpose = :purpose
              and media.status in :statuses
              and media.deletedAt is null
            """)
    long sumSizeBytesByOwnerIdAndPurposeAndStatusIn(
            @Param("ownerId") Long ownerId,
            @Param("purpose") ManagedMediaPurpose purpose,
            @Param("statuses") Collection<ManagedMediaStatus> statuses);

    List<ManagedMedia> findAllByOwnerId(Long ownerId);

    List<ManagedMedia> findAllByOwnerIdAndPurposeAndStatusAndDeletedAtIsNull(Long ownerId, ManagedMediaPurpose purpose, ManagedMediaStatus status);

    List<ManagedMedia> findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(ManagedMediaStatus status, Instant createdBefore);
}
