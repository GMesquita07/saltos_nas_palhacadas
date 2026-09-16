package pt.saltosnaspalhacadas.backend.config;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseLimits {
    private static final int MAX_PUBLIC_ITEMS = 500;
    private static final int MAX_PRIVATE_ITEMS = 500;
    private static final int MAX_ADMIN_ITEMS = 1_000;

    private final int publicResponseLimit;
    private final int privateResponseLimit;
    private final int adminResponseLimit;

    public ApiResponseLimits(
            @Value("${app.api.public-response-limit:200}") int publicResponseLimit,
            @Value("${app.api.private-response-limit:200}") int privateResponseLimit,
            @Value("${app.api.admin-response-limit:500}") int adminResponseLimit) {
        this.publicResponseLimit = normalize(publicResponseLimit, 200, MAX_PUBLIC_ITEMS);
        this.privateResponseLimit = normalize(privateResponseLimit, 200, MAX_PRIVATE_ITEMS);
        this.adminResponseLimit = normalize(adminResponseLimit, 500, MAX_ADMIN_ITEMS);
    }

    public <T> List<T> publicList(Stream<T> stream) {
        return stream.limit(publicResponseLimit).toList();
    }

    public <T> List<T> privateList(Stream<T> stream) {
        return stream.limit(privateResponseLimit).toList();
    }

    public <T> List<T> adminList(Stream<T> stream) {
        return stream.limit(adminResponseLimit).toList();
    }

    private static int normalize(int value, int fallback, int max) {
        if (value <= 0) {
            return fallback;
        }
        return Math.min(value, max);
    }
}
