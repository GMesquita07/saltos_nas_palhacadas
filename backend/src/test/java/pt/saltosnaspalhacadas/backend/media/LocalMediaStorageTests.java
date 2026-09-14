package pt.saltosnaspalhacadas.backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class LocalMediaStorageTests {
    @TempDir
    Path tempDirectory;

    @Test
    void storesPublishesReadsAndDeletesMediaLocally() throws Exception {
        LocalMediaStorage storage = storage();

        StoredMedia media = storage.storePrivate(new MockMultipartFile("file", "festa.png", "image/png", pngHeader()));

        assertThat(storage.isValidStorageKey(media.storageKey())).isTrue();
        assertThat(storage.privateExists(media.storageKey())).isTrue();
        assertThat(storage.readPrivate(media.storageKey()).contentLength()).isEqualTo((long) pngHeader().length);

        storage.publishPrivate(media.storageKey());

        assertThat(storage.privateExists(media.storageKey())).isFalse();
        assertThat(storage.readPublic(media.storageKey()).contentType()).isEqualTo("image/png");

        storage.deletePublic(media.storageKey());

        assertThatThrownBy(() -> storage.readPublic(media.storageKey()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsUnsupportedContent() {
        LocalMediaStorage storage = storage();
        MockMultipartFile svg = new MockMultipartFile("file", "script.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> storage.storePrivate(svg))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsImagesOverTenMegabytes() {
        LocalMediaStorage storage = storage();
        byte[] content = new byte[(10 * 1024 * 1024) + 1];
        byte[] header = pngHeader();
        System.arraycopy(header, 0, content, 0, header.length);

        MockMultipartFile file = new MockMultipartFile("file", "grande.png", "image/png", content);

        assertThatThrownBy(() -> storage.storePrivate(file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void rejectsMagicBytesThatDoNotMatchContentType() {
        LocalMediaStorage storage = storage();
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not-a-png".getBytes());

        assertThatThrownBy(() -> storage.storePrivate(file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private LocalMediaStorage storage() {
        return new LocalMediaStorage(
                new MediaFileValidator(),
                tempDirectory.resolve("public").toString(),
                tempDirectory.resolve("private").toString());
    }

    private static byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }
}
