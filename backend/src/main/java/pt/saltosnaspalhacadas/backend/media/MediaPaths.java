package pt.saltosnaspalhacadas.backend.media;

public final class MediaPaths {
    public static final String PUBLIC_MEDIA_PATH = "/api/v1/media/";
    public static final String PRIVATE_MEDIA_PATH = "/api/v1/private-media/";

    private MediaPaths() {
    }

    public static String publicPath(String storageKey) {
        return PUBLIC_MEDIA_PATH + storageKey;
    }

    public static String privatePath(String storageKey) {
        return PRIVATE_MEDIA_PATH + storageKey;
    }
}
