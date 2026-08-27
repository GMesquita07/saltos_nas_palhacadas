package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@Service
public class ClientContentMediaService {
    private static final Logger log = LoggerFactory.getLogger(ClientContentMediaService.class);
    private static final Set<ManagedMediaStatus> QUOTA_STATUSES = Set.of(ManagedMediaStatus.PENDING, ManagedMediaStatus.ATTACHED);

    private final LocalMediaStorage storage;
    private final ManagedMediaRepository media;
    private final ClientContentPostRepository clientPosts;
    private final int maxPendingUploadsPerUser;
    private final long maxPendingUploadBytesPerUser;
    private final int privateUploadRetentionHours;

    public ClientContentMediaService(
            LocalMediaStorage storage,
            ManagedMediaRepository media,
            ClientContentPostRepository clientPosts,
            @Value("${app.media.client-content.max-pending-uploads-per-user:12}") int maxPendingUploadsPerUser,
            @Value("${app.media.client-content.max-pending-upload-bytes-per-user:314572800}") long maxPendingUploadBytesPerUser,
            @Value("${app.media.client-content.private-upload-retention-hours:24}") int privateUploadRetentionHours) {
        this.storage = storage;
        this.media = media;
        this.clientPosts = clientPosts;
        this.maxPendingUploadsPerUser = Math.max(1, maxPendingUploadsPerUser);
        this.maxPendingUploadBytesPerUser = Math.max(1, maxPendingUploadBytesPerUser);
        this.privateUploadRetentionHours = Math.max(1, privateUploadRetentionHours);
    }

    @Transactional
    public ManagedMedia uploadPrivate(AppUser owner, MultipartFile file) throws IOException {
        assertQuotaAvailable(owner, file.getSize());
        LocalMediaStorage.StoredMedia stored = storage.storePrivate(file);
        try {
            return media.save(new ManagedMedia(owner, stored.filename(), stored.contentType(), file.getSize()));
        } catch (RuntimeException exception) {
            storage.deletePrivate(stored.filename());
            throw exception;
        }
    }

    @Transactional
    public ManagedMedia attachOwnedPendingMedia(UUID mediaId, AppUser owner, MediaType expectedType, String missingMessage) {
        if (mediaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingMessage);
        }

        ManagedMedia managedMedia = media.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ficheiro enviado já não está disponível"));

        if (!managedMedia.isOwnedBy(owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        if (managedMedia.getStatus() != ManagedMediaStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este ficheiro já foi usado noutra publicação");
        }
        if (!matchesExpectedType(managedMedia, expectedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo do ficheiro não corresponde à publicação");
        }

        managedMedia.markAttached(Instant.now());
        return media.save(managedMedia);
    }

    @Transactional(readOnly = true)
    public PrivateMediaDownload requirePrivateDownload(String filename, AppUser currentUser) {
        if (!storage.isSafeFilename(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }

        if (!storage.privateExists(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }

        return media.findByStorageKeyAndDeletedAtIsNull(filename)
                .map(managedMedia -> requireManagedPrivateDownload(managedMedia, currentUser))
                .orElseGet(() -> requireLegacyPrivateDownload(filename, currentUser));
    }

    @Transactional
    public String publish(ManagedMedia managedMedia) throws IOException {
        if (managedMedia == null) {
            return null;
        }
        if (managedMedia.getStatus() == ManagedMediaStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ficheiro já foi apagado");
        }
        if (managedMedia.getStatus() != ManagedMediaStatus.PUBLIC) {
            storage.publishPrivate(managedMedia.getStorageKey());
            managedMedia.markPublic(Instant.now());
            media.save(managedMedia);
        }
        return LocalMediaStorage.PUBLIC_MEDIA_PATH + managedMedia.getStorageKey();
    }

    @Transactional
    public void delete(ManagedMedia managedMedia) throws IOException {
        if (managedMedia == null || managedMedia.getStatus() == ManagedMediaStatus.DELETED) {
            return;
        }

        if (managedMedia.getStatus() == ManagedMediaStatus.PUBLIC) {
            storage.deletePublic(managedMedia.getStorageKey());
        } else {
            storage.deletePrivate(managedMedia.getStorageKey());
        }
        managedMedia.markDeleted(Instant.now());
        media.save(managedMedia);
    }

    @Scheduled(cron = "${app.media.client-content.cleanup-cron:0 30 3 * * *}", zone = "${app.booking.reminder.zone:Europe/Lisbon}")
    @Transactional
    public void cleanupExpiredPrivateUploads() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(privateUploadRetentionHours));
        int deleted = 0;

        for (ManagedMedia managedMedia : media.findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(ManagedMediaStatus.PENDING, cutoff)) {
            try {
                delete(managedMedia);
                deleted++;
            } catch (IOException exception) {
                log.warn("Não foi possível apagar upload privado órfão {}", managedMedia.getId(), exception);
            }
        }

        if (deleted > 0) {
            log.info("Foram apagados {} uploads privados órfãos", deleted);
        }
    }

    private void assertQuotaAvailable(AppUser owner, long nextUploadBytes) {
        long pendingUploads = media.countByOwnerIdAndStatusInAndDeletedAtIsNull(owner.getId(), QUOTA_STATUSES);
        if (pendingUploads >= maxPendingUploadsPerUser) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Tens demasiados uploads pendentes. Aguarda aprovação ou tenta mais tarde.");
        }

        long usedBytes = media.sumSizeBytesByOwnerIdAndStatusIn(owner.getId(), QUOTA_STATUSES);
        if (usedBytes + Math.max(0, nextUploadBytes) > maxPendingUploadBytesPerUser) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "A tua conta atingiu o limite temporário de uploads pendentes.");
        }
    }

    private static boolean matchesExpectedType(ManagedMedia managedMedia, MediaType expectedType) {
        boolean isVideo = managedMedia.getContentType() != null && managedMedia.getContentType().startsWith("video/");
        return expectedType == MediaType.VIDEO ? isVideo : !isVideo;
    }

    private static PrivateMediaDownload requireManagedPrivateDownload(ManagedMedia managedMedia, AppUser currentUser) {
        if (managedMedia.getStatus() == ManagedMediaStatus.PUBLIC || managedMedia.getStatus() == ManagedMediaStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        if (!managedMedia.isOwnedBy(currentUser) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        return new PrivateMediaDownload(managedMedia.getContentType());
    }

    private PrivateMediaDownload requireLegacyPrivateDownload(String filename, AppUser currentUser) {
        String privateMediaPath = LocalMediaStorage.PRIVATE_MEDIA_PATH + filename;
        boolean allowed = clientPosts.existsVisibleLegacyPrivateMedia(
                privateMediaPath,
                currentUser.getId(),
                currentUser.getRole() == UserRole.ADMIN);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        return new PrivateMediaDownload(null);
    }

    public record PrivateMediaDownload(String contentType) {
    }
}
