package pt.saltosnaspalhacadas.backend.media;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;

final class MediaHttpResponses {
    private MediaHttpResponses() {
    }

    static ResponseEntity<Resource> ok(MediaDownload download, boolean noStore) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(contentType(download))
                .header("X-Content-Type-Options", "nosniff");

        if (noStore) {
            response.cacheControl(CacheControl.noStore());
        } else {
            response.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
        }

        if (download.contentLength() != null && download.contentLength() >= 0) {
            response.contentLength(download.contentLength());
        }

        return response.body(download.resource());
    }

    private static MediaType contentType(MediaDownload download) {
        String value = download.contentType();
        if (value != null && !value.isBlank()) {
            try {
                MediaType parsed = MediaType.parseMediaType(value);
                if (parsed.isConcrete()) {
                    return parsed;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to the storage key extension.
            }
        }

        return MediaTypeFactory.getMediaType(download.storageKey())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
