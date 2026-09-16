package pt.saltosnaspalhacadas.backend.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpAddress {
    private ClientIpAddress() {
    }

    public static String from(HttpServletRequest request) {
        String forwardedFor = firstForwardedAddress(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = clean(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String remoteAddress = clean(request.getRemoteAddr());
        return remoteAddress == null ? "unknown" : remoteAddress;
    }

    private static String firstForwardedAddress(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }

        int comma = cleaned.indexOf(',');
        return comma >= 0 ? clean(cleaned.substring(0, comma)) : cleaned;
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
