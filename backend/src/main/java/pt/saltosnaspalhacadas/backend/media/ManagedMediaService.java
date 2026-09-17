package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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

import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@Service
public class ManagedMediaService {
    private static final Logger log = LoggerFactory.getLogger(ManagedMediaService.class);

    private final MediaStorage storage;
    private final ManagedMediaRepository media;
    private final int privateUploadRetentionHours;

    public ManagedMediaService(
            MediaStorage storage,
            ManagedMediaRepository media,
            @Value("${app.media.private-upload-retention-hours:24}") int privateUploadRetentionHours) {
        this.storage = storage;
        this.media = media;
        this.privateUploadRetentionHours = Math.max(1, privateUploadRetentionHours);
    }

    @Transactional
    public ManagedMedia uploadPrivateAvatar(AppUser owner, MultipartFile file) throws IOException {
        for (ManagedMedia pendingAvatar : media.findAllByOwnerIdAndPurposeAndStatusAndDeletedAtIsNull(
                owner.getId(),
                ManagedMediaPurpose.PROFILE_AVATAR,
                ManagedMediaStatus.PENDING)) {
            delete(pendingAvatar);
        }

        StoredMedia stored = storage.storePrivate(file);
        if (!stored.contentType().startsWith("image/")) {
            storage.deletePrivate(stored.storageKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleciona uma imagem válida");
        }

        try {
            return media.save(new ManagedMedia(owner, stored.storageKey(), stored.contentType(), file.getSize(), ManagedMediaPurpose.PROFILE_AVATAR));
        } catch (RuntimeException exception) {
            storage.deletePrivate(stored.storageKey());
            throw exception;
        }
    }

    @Transactional
    public ManagedMedia attachOwnedPendingMedia(UUID mediaId, AppUser owner, MediaType expectedType, ManagedMediaPurpose expectedPurpose, String missingMessage) {
        if (mediaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingMessage);
        }

        ManagedMedia managedMedia = media.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ficheiro enviado já não está disponível"));

        if (!managedMedia.isOwnedBy(owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        if (managedMedia.getStatus() != ManagedMediaStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este ficheiro já foi usado noutra operação");
        }
        if (managedMedia.getPurpose() != expectedPurpose) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este ficheiro não pode ser usado aqui");
        }
        if (!matchesExpectedType(managedMedia, expectedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo do ficheiro não corresponde ao uso esperado");
        }

        managedMedia.markAttached(Instant.now());
        return media.save(managedMedia);
    }

    @Transactional(readOnly = true)
    public MediaDownload requirePrivateDownload(String filename, AppUser currentUser) throws IOException {
        if (!storage.isValidStorageKey(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }

        ManagedMedia managedMedia = media.findByStorageKeyAndDeletedAtIsNull(filename)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado"));
        String contentType = requireManagedPrivateDownload(managedMedia, currentUser);
        return requireExistingPrivateDownload(filename, contentType);
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

    @Scheduled(cron = "${app.media.private-upload-cleanup-cron:0 30 3 * * *}", zone = "${app.booking.reminder.zone:Europe/Lisbon}")
    @Transactional
    public void cleanupExpiredPrivateUploads() {
        cleanupExpiredPrivateUploadsNow();
    }

    @Transactional
    public int cleanupExpiredPrivateUploadsNow() {
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
        return deleted;
    }

    private static boolean matchesExpectedType(ManagedMedia managedMedia, MediaType expectedType) {
        boolean isVideo = managedMedia.getContentType() != null && managedMedia.getContentType().startsWith("video/");
        return expectedType == MediaType.VIDEO ? isVideo : !isVideo;
    }

    private MediaDownload requireExistingPrivateDownload(String filename, String contentType) throws IOException {
        if (!storage.privateExists(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        return storage.readPrivate(filename).withContentType(contentType);
    }

    private static String requireManagedPrivateDownload(ManagedMedia managedMedia, AppUser currentUser) {
        if (managedMedia.getStatus() == ManagedMediaStatus.PUBLIC || managedMedia.getStatus() == ManagedMediaStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        if (!managedMedia.isOwnedBy(currentUser) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
        return managedMedia.getContentType();
    }
}
