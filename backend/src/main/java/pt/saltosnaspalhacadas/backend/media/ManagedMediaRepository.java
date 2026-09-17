package pt.saltosnaspalhacadas.backend.media;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagedMediaRepository extends JpaRepository<ManagedMedia, UUID> {
    Optional<ManagedMedia> findByIdAndDeletedAtIsNull(UUID id);

    Optional<ManagedMedia> findByStorageKeyAndDeletedAtIsNull(String storageKey);

    List<ManagedMedia> findAllByOwnerId(Long ownerId);

    List<ManagedMedia> findAllByOwnerIdAndPurposeAndStatusAndDeletedAtIsNull(Long ownerId, ManagedMediaPurpose purpose, ManagedMediaStatus status);

    List<ManagedMedia> findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(ManagedMediaStatus status, Instant createdBefore);
}
