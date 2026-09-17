package pt.saltosnaspalhacadas.backend.maintenance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.booking.BookingReminderService;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaService;

@RestController
@RequestMapping("/internal/maintenance")
class MaintenanceController {
    private static final String MAINTENANCE_KEY_HEADER = "X-Maintenance-Key";

    private final BookingReminderService bookingReminders;
    private final ManagedMediaService mediaService;
    private final String apiKey;

    MaintenanceController(
            BookingReminderService bookingReminders,
            ManagedMediaService mediaService,
            @Value("${app.maintenance.api-key:}") String apiKey) {
        this.bookingReminders = bookingReminders;
        this.mediaService = mediaService;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @PostMapping("/booking-reminders")
    MaintenanceResponse runBookingReminders(@RequestHeader(name = MAINTENANCE_KEY_HEADER, required = false) String providedKey) {
        requireMaintenanceKey(providedKey);
        return new MaintenanceResponse(bookingReminders.sendDueRemindersNow());
    }

    @PostMapping("/private-media-cleanup")
    MaintenanceResponse cleanupPrivateMedia(@RequestHeader(name = MAINTENANCE_KEY_HEADER, required = false) String providedKey) {
        requireMaintenanceKey(providedKey);
        return new MaintenanceResponse(mediaService.cleanupExpiredPrivateUploadsNow());
    }

    private void requireMaintenanceKey(String providedKey) {
        if (apiKey.isBlank() || providedKey == null || providedKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        byte[] expected = apiKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = providedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private record MaintenanceResponse(int processed) {
    }
}
