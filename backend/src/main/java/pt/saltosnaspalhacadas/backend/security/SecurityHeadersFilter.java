package pt.saltosnaspalhacadas.backend.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private final boolean hstsEnabled;
    private final String hstsHeader;

    public SecurityHeadersFilter(
            @Value("${app.security.hsts.enabled:false}") boolean hstsEnabled,
            @Value("${app.security.hsts.max-age-seconds:31536000}") long hstsMaxAgeSeconds,
            @Value("${app.security.hsts.include-subdomains:true}") boolean hstsIncludeSubdomains,
            @Value("${app.security.hsts.preload:false}") boolean hstsPreload) {
        this.hstsEnabled = hstsEnabled;
        this.hstsHeader = hstsHeader(hstsMaxAgeSeconds, hstsIncludeSubdomains, hstsPreload);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");

        if (hstsEnabled && isHttps(request)) {
            response.setHeader("Strict-Transport-Security", hstsHeader);
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/bookings") || path.startsWith("/api/v1/admin") || path.startsWith("/api/v1/private-media")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null || forwardedProto.isBlank()) {
            return false;
        }

        String firstProto = forwardedProto.split(",")[0].trim();
        return "https".equalsIgnoreCase(firstProto);
    }

    private static String hstsHeader(long maxAgeSeconds, boolean includeSubdomains, boolean preload) {
        long safeMaxAge = Math.max(0, maxAgeSeconds);
        StringBuilder header = new StringBuilder("max-age=").append(safeMaxAge);
        if (includeSubdomains) {
            header.append("; includeSubDomains");
        }
        if (preload) {
            header.append("; preload");
        }
        return header.toString();
    }
}
