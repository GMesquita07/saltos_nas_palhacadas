package pt.saltosnaspalhacadas.backend.security;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PublicUrlValidator {
    private PublicUrlValidator() {
    }

    public static String optional(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return validate(value, errorMessage);
    }

    public static String required(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return validate(value, errorMessage);
    }

    private static String validate(String value, String errorMessage) {
        String candidate = value.trim();
        if (isInternalMediaPath(candidate)) {
            return candidate;
        }

        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            if (uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return candidate;
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private static boolean isInternalMediaPath(String value) {
        return value.startsWith("/api/v1/media/")
                && !value.contains("..")
                && !value.contains("\\")
                && !value.contains("\0");
    }
}
