package pt.saltosnaspalhacadas.backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.UserRole;

class ManagedMediaServiceTests {
    private static final String STORAGE_KEY = "123e4567-e89b-12d3-a456-426614174000.png";

    private final MediaStorage storage = mock(MediaStorage.class);
    private final ManagedMediaRepository media = mock(ManagedMediaRepository.class);
    private final ManagedMediaService service = new ManagedMediaService(
            storage,
            media,
            24);

    @Test
    void deniesManagedPrivateMediaBeforeStorageLookupWhenUserDoesNotOwnIt() throws Exception {
        AppUser owner = user(1L, UserRole.CUSTOMER);
        AppUser otherUser = user(2L, UserRole.CUSTOMER);
        ManagedMedia managedMedia = new ManagedMedia(owner, STORAGE_KEY, "image/png", 12);
        when(storage.isValidStorageKey(STORAGE_KEY)).thenReturn(true);
        when(media.findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY)).thenReturn(Optional.of(managedMedia));

        assertThatThrownBy(() -> service.requirePrivateDownload(STORAGE_KEY, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        InOrder order = inOrder(storage, media);
        order.verify(storage).isValidStorageKey(STORAGE_KEY);
        order.verify(media).findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY);
        verify(storage, never()).privateExists(anyString());
        verify(storage, never()).readPrivate(anyString());
    }

    @Test
    void checksManagedPrivateMediaExistsOnlyAfterOwnerIsAuthorized() throws Exception {
        AppUser owner = user(1L, UserRole.CUSTOMER);
        ManagedMedia managedMedia = new ManagedMedia(owner, STORAGE_KEY, "image/png", 12);
        MediaDownload storedDownload = download(null);
        when(storage.isValidStorageKey(STORAGE_KEY)).thenReturn(true);
        when(media.findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY)).thenReturn(Optional.of(managedMedia));
        when(storage.privateExists(STORAGE_KEY)).thenReturn(true);
        when(storage.readPrivate(STORAGE_KEY)).thenReturn(storedDownload);

        MediaDownload result = service.requirePrivateDownload(STORAGE_KEY, owner);

        assertThat(result.contentType()).isEqualTo("image/png");
        InOrder order = inOrder(storage, media);
        order.verify(storage).isValidStorageKey(STORAGE_KEY);
        order.verify(media).findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY);
        order.verify(storage).privateExists(STORAGE_KEY);
        order.verify(storage).readPrivate(STORAGE_KEY);
    }

    @Test
    void returnsNotFoundForUnknownManagedPrivateMediaWithoutStorageLookup() throws Exception {
        AppUser otherUser = user(2L, UserRole.CUSTOMER);
        when(storage.isValidStorageKey(STORAGE_KEY)).thenReturn(true);
        when(media.findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requirePrivateDownload(STORAGE_KEY, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        InOrder order = inOrder(storage, media);
        order.verify(storage).isValidStorageKey(STORAGE_KEY);
        order.verify(media).findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY);
        verify(storage, never()).privateExists(anyString());
        verify(storage, never()).readPrivate(anyString());
    }

    @Test
    void allowsAdminToReadManagedPrivateMediaAfterStorageLookup() throws Exception {
        AppUser owner = user(1L, UserRole.CUSTOMER);
        AppUser admin = user(2L, UserRole.ADMIN);
        ManagedMedia managedMedia = new ManagedMedia(owner, STORAGE_KEY, "image/png", 12);
        MediaDownload storedDownload = download("image/png");
        when(storage.isValidStorageKey(STORAGE_KEY)).thenReturn(true);
        when(media.findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY)).thenReturn(Optional.of(managedMedia));
        when(storage.privateExists(STORAGE_KEY)).thenReturn(true);
        when(storage.readPrivate(STORAGE_KEY)).thenReturn(storedDownload);

        MediaDownload result = service.requirePrivateDownload(STORAGE_KEY, admin);

        assertThat(result.contentType()).isEqualTo("image/png");
        InOrder order = inOrder(storage, media);
        order.verify(storage).isValidStorageKey(STORAGE_KEY);
        order.verify(media).findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY);
        order.verify(storage).privateExists(STORAGE_KEY);
        order.verify(storage).readPrivate(STORAGE_KEY);
    }

    @Test
    void cleanupExpiredPrivateUploadsNowDeletesPendingPrivateMediaAndReturnsCount() throws Exception {
        AppUser owner = user(1L, UserRole.CUSTOMER);
        ManagedMedia expiredMedia = new ManagedMedia(owner, STORAGE_KEY, "image/png", 12);
        when(media.findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(eq(ManagedMediaStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(expiredMedia));

        int deleted = service.cleanupExpiredPrivateUploadsNow();

        assertThat(deleted).isEqualTo(1);
        assertThat(expiredMedia.getStatus()).isEqualTo(ManagedMediaStatus.DELETED);
        assertThat(expiredMedia.getDeletedAt()).isNotNull();
        verify(storage).deletePrivate(STORAGE_KEY);
        verify(media).save(expiredMedia);
    }

    private static MediaDownload download(String contentType) {
        return new MediaDownload(
                STORAGE_KEY,
                new ByteArrayResource(new byte[] {1}),
                contentType,
                1L);
    }

    private static AppUser user(Long id, UserRole role) {
        AppUser user = new AppUser("user-" + id + "@example.test", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
