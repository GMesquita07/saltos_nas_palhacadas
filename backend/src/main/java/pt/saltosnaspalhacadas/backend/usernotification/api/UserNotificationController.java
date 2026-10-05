package pt.saltosnaspalhacadas.backend.usernotification.api;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.usernotification.UserNotification;
import pt.saltosnaspalhacadas.backend.usernotification.UserNotificationService;
import pt.saltosnaspalhacadas.backend.usernotification.UserNotificationType;

@RestController
@RequestMapping("/api/v1/notifications")
public class UserNotificationController {

    private final UserNotificationService notifications;

    public UserNotificationController(UserNotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    NotificationInboxResponse findMine(Authentication authentication) {
        UserNotificationService.NotificationInbox inbox = notifications.findMine(currentEmail(authentication));
        return new NotificationInboxResponse(
                inbox.items().stream().map(NotificationResponse::from).toList(),
                inbox.unreadCount());
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(Authentication authentication, @PathVariable Long notificationId) {
        notifications.markRead(currentEmail(authentication), notificationId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(Authentication authentication) {
        notifications.markAllRead(currentEmail(authentication));
    }

    private static String currentEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }
        return authentication.getName();
    }

    record NotificationInboxResponse(List<NotificationResponse> notifications, long unreadCount) {
    }

    record NotificationResponse(
            Long id,
            UserNotificationType type,
            String title,
            String message,
            Long bookingId,
            Instant createdAt,
            Instant readAt,
            boolean read) {
        static NotificationResponse from(UserNotification notification) {
            return new NotificationResponse(
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getBooking() == null ? null : notification.getBooking().getId(),
                    notification.getCreatedAt(),
                    notification.getReadAt(),
                    notification.isRead());
        }
    }
}
