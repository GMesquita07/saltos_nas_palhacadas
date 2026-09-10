package pt.saltosnaspalhacadas.backend.media;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

final class MediaStorageKeys {
    private static final Pattern SAFE_STORAGE_KEY = Pattern.compile("[0-9a-fA-F-]{36}\\.(jpg|png|webp|gif|mp4|webm|mov)");

    private MediaStorageKeys() {
    }

    static boolean isValid(String storageKey) {
        return storageKey != null && SAFE_STORAGE_KEY.matcher(storageKey).matches();
    }

    static Optional<String> fromPathOrUrl(String value, String prefix) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String path;
        String trimmed = value.trim();
        try {
            path = trimmed.startsWith("/") ? trimmed : URI.create(trimmed).getPath();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        if (path == null || !path.startsWith(prefix)) {
            return Optional.empty();
        }

        String storageKey = URLDecoder.decode(path.substring(prefix.length()), StandardCharsets.UTF_8);
        if (!isValid(storageKey)) {
            return Optional.empty();
        }
        return Optional.of(storageKey);
    }
}
