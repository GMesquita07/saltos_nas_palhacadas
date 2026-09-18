package pt.saltosnaspalhacadas.backend.notification;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@ExtendWith(MockitoExtension.class)
class SiteNotificationServiceTests {

    @Mock
    private EmailService emailService;

    @Mock
    private AppUserRepository users;

    @Test
    void adminRecipientsAreDeduplicatedAndExcludedCaseInsensitively() {
        SiteNotificationService notifications = new SiteNotificationService(emailService, users);
        AppUser firstAdmin = new AppUser("admin@example.test", "hash", UserRole.ADMIN);
        AppUser duplicateAdmin = new AppUser("admin@example.test", "hash", UserRole.ADMIN);
        AppUser secondAdmin = new AppUser("second@example.test", "hash", UserRole.ADMIN);

        when(users.findAllByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(List.of(firstAdmin, duplicateAdmin, secondAdmin));
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(true);

        notifications.notifyAdmins("unit-test", "Assunto", "Corpo", Set.of("ADMIN@EXAMPLE.TEST"));

        verify(emailService, never()).send(eq("admin@example.test"), anyString(), anyString());
        verify(emailService).send(eq("second@example.test"), eq("Assunto"), eq("Corpo"));
    }
}
