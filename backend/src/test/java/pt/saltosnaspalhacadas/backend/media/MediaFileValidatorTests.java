package pt.saltosnaspalhacadas.backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class MediaFileValidatorTests {
    private static final long MIB = 1024 * 1024;

    private final MediaFileValidator validator = new MediaFileValidator();

    @Test
    void acceptsImagesUpToTenMebibytesBySize() throws Exception {
        StoredMedia media = validator.validate(file("festa.png", "image/png", 10 * MIB, pngHeader()));

        assertThat(media.contentType()).isEqualTo("image/png");
        assertThat(media.storageKey()).endsWith(".png");
    }

    @Test
    void rejectsImagesOverTenMebibytes() {
        assertThatThrownBy(() -> validator.validate(file("festa.png", "image/png", (10 * MIB) + 1, pngHeader())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void acceptsVideosUpToThirtyMebibytesBySize() throws Exception {
        StoredMedia media = validator.validate(file("evento.mp4", "video/mp4", 30 * MIB, mp4Header()));

        assertThat(media.contentType()).isEqualTo("video/mp4");
        assertThat(media.storageKey()).endsWith(".mp4");
    }

    @Test
    void rejectsVideosOverThirtyMebibytes() {
        assertThatThrownBy(() -> validator.validate(file("evento.mp4", "video/mp4", (30 * MIB) + 1, mp4Header())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedTypes() {
        assertThatThrownBy(() -> validator.validate(file("script.svg", "image/svg+xml", 512, "<svg/>".getBytes())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsMagicBytesThatDoNotMatchContentType() {
        assertThatThrownBy(() -> validator.validate(file("fake.png", "image/png", 512, "not-a-png".getBytes())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static SizedMultipartFile file(String originalFilename, String contentType, long size, byte[] content) {
        return new SizedMultipartFile(originalFilename, contentType, size, content);
    }

    private static byte[] mp4Header() {
        return new byte[] {0, 0, 0, 24, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d, 0, 0, 0, 1};
    }

    private static byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }

    private record SizedMultipartFile(String originalFilename, String contentType, long size, byte[] content) implements MultipartFile {
        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            throw new UnsupportedOperationException("Not needed for validator tests");
        }
    }
}
