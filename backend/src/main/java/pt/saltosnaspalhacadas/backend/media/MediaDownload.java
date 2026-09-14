package pt.saltosnaspalhacadas.backend.media;

import org.springframework.core.io.Resource;

public record MediaDownload(String storageKey, Resource resource, String contentType, Long contentLength) {
    public MediaDownload withContentType(String nextContentType) {
        if (nextContentType == null || nextContentType.isBlank()) {
            return this;
        }
        return new MediaDownload(storageKey, resource, nextContentType.trim(), contentLength);
    }
}
