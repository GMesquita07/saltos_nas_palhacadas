package pt.saltosnaspalhacadas.backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.UserRole;

class ClientContentMediaServiceTests {
    private static final String STORAGE_KEY = "123e4567-e89b-12d3-a456-426614174000.png";

    private final MediaStorage storage = mock(MediaStorage.class);
    private final ManagedMediaRepository media = mock(ManagedMediaRepository.class);
    private final ClientContentPostRepository clientPosts = mock(ClientContentPostRepository.class);
    private final ClientContentMediaService service = new ClientContentMediaService(
            storage,
            media,
            clientPosts,
            12,
            314572800,
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
        verifyNoInteractions(clientPosts);
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
        verifyNoInteractions(clientPosts);
    }

    @Test
    void deniesLegacyPrivateMediaBeforeStorageLookupWhenUserCannotSeePost() throws Exception {
        AppUser otherUser = user(2L, UserRole.CUSTOMER);
        when(storage.isValidStorageKey(STORAGE_KEY)).thenReturn(true);
        when(media.findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY)).thenReturn(Optional.empty());
        when(clientPosts.existsVisibleLegacyPrivateMedia(MediaPaths.privatePath(STORAGE_KEY), otherUser.getId(), false)).thenReturn(false);

        assertThatThrownBy(() -> service.requirePrivateDownload(STORAGE_KEY, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        InOrder order = inOrder(storage, media, clientPosts);
        order.verify(storage).isValidStorageKey(STORAGE_KEY);
        order.verify(media).findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY);
        order.verify(clientPosts).existsVisibleLegacyPrivateMedia(MediaPaths.privatePath(STORAGE_KEY), otherUser.getId(), false);
        verify(storage, never()).privateExists(anyString());
        verify(storage, never()).readPrivate(anyString());
    }

    @Test
    void checksLegacyPrivateMediaExistsOnlyAfterUserCanSeePost() throws Exception {
        AppUser owner = user(1L, UserRole.CUSTOMER);
        MediaDownload storedDownload = download("image/png");
        when(storage.isValidStorageKey(STORAGE_KEY)).thenReturn(true);
        when(media.findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY)).thenReturn(Optional.empty());
        when(clientPosts.existsVisibleLegacyPrivateMedia(MediaPaths.privatePath(STORAGE_KEY), owner.getId(), false)).thenReturn(true);
        when(storage.privateExists(STORAGE_KEY)).thenReturn(true);
        when(storage.readPrivate(STORAGE_KEY)).thenReturn(storedDownload);

        MediaDownload result = service.requirePrivateDownload(STORAGE_KEY, owner);

        assertThat(result).isSameAs(storedDownload);
        InOrder order = inOrder(storage, media, clientPosts);
        order.verify(storage).isValidStorageKey(STORAGE_KEY);
        order.verify(media).findByStorageKeyAndDeletedAtIsNull(STORAGE_KEY);
        order.verify(clientPosts).existsVisibleLegacyPrivateMedia(MediaPaths.privatePath(STORAGE_KEY), owner.getId(), false);
        order.verify(storage).privateExists(STORAGE_KEY);
        order.verify(storage).readPrivate(STORAGE_KEY);
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
