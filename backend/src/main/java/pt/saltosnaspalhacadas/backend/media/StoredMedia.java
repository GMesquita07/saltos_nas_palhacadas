package pt.saltosnaspalhacadas.backend.media;

public record StoredMedia(String storageKey, String contentType) {
    public String filename() {
        return storageKey;
    }
}
