package pt.saltosnaspalhacadas.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

@Service
public class IpRateLimiter {
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public IpRateLimiter() {
        this(Clock.systemUTC());
    }

    IpRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String scope, String subject, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }

        long now = clock.millis();
        long windowMillis = Math.max(Duration.ofSeconds(1).toMillis(), window.toMillis());
        String key = scope + ":" + subject;
        AtomicBoolean allowed = new AtomicBoolean(false);

        windows.compute(key, (ignored, current) -> {
            Window active = current;
            if (active == null || now >= active.resetAtMillis()) {
                active = new Window(now + windowMillis, 0);
            }
            if (active.count() >= limit) {
                return active;
            }
            allowed.set(true);
            return new Window(active.resetAtMillis(), active.count() + 1);
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            cleanupExpired(now);
        }

        return allowed.get();
    }

    private void cleanupExpired(long now) {
        windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtMillis());
    }

    private record Window(long resetAtMillis, int count) {
    }
}
