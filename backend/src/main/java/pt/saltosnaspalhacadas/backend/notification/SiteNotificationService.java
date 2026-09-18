package pt.saltosnaspalhacadas.backend.notification;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@Service
public class SiteNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SiteNotificationService.class);

    private final EmailService emailService;
    private final AppUserRepository users;

    public SiteNotificationService(EmailService emailService, AppUserRepository users) {
        this.emailService = emailService;
        this.users = users;
    }

    public void notifyAdmins(String eventType, String subject, String body) {
        notifyAdmins(eventType, subject, body, Set.of());
    }

    public void notifyAdmins(String eventType, String subject, String body, Collection<String> excludedEmails) {
        try {
            Set<String> excluded = normalizedEmails(excludedEmails);
            for (String email : activeAdminRecipients().values()) {
                if (!excluded.contains(normalize(email))) {
                    sendBestEffort(eventType, email, subject, body);
                }
            }
        } catch (RuntimeException exception) {
            log.warn("Falha ao preparar notificação '{}' para administradores ativos", eventType, exception);
        }
    }

    public boolean sendBestEffort(String eventType, String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            return false;
        }
        try {
            boolean sent = emailService.send(recipient, subject, body);
            if (!sent) {
                log.warn("Notificação '{}' não enviada para {}", eventType, maskEmail(recipient));
            }
            return sent;
        } catch (RuntimeException exception) {
            log.warn("Falha inesperada ao enviar notificação '{}' para {}", eventType, maskEmail(recipient), exception);
            return false;
        }
    }

    public void notifyNewCustomerRegistration(AppUser user) {
        notifyAdmins(
                "customer-registration",
                "Saltos nas Palhaçadas: novo registo de cliente",
                """
                        Novo cliente registado no site.

                        Email: %s
                        Nome de utilizador: %s
                        Nome: %s %s

                        Não foi incluída qualquer palavra-passe nesta notificação.
                        """.formatted(
                        user.getEmail(),
                        blankFallback(user.getUsername()),
                        blankFallback(user.getFirstName()),
                        blankFallback(user.getLastName())));
    }

    public void notifyNewReview(Review review) {
        String profileName = review.getProfile() == null ? "Perfil indisponível" : review.getProfile().getName();
        notifyAdmins(
                "review-submitted",
                "Saltos nas Palhaçadas: nova avaliação pendente",
                """
                        Foi submetida uma nova avaliação para moderação.

                        Artista: %s
                        Avaliador: %s
                        Classificação: %d/5
                        Título: %s
                        Comentário: %s
                        """.formatted(
                        profileName,
                        review.getReviewerName(),
                        review.getRating(),
                        review.getTitle(),
                        review.getComment()));
    }

    private Map<String, String> activeAdminRecipients() {
        Map<String, String> recipients = new LinkedHashMap<>();
        for (AppUser admin : users.findAllByRoleAndActiveTrue(UserRole.ADMIN)) {
            String normalized = normalize(admin.getEmail());
            if (!normalized.isBlank()) {
                recipients.putIfAbsent(normalized, admin.getEmail().trim());
            }
        }
        return recipients;
    }

    private static Set<String> normalizedEmails(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Set.of();
        }
        return emails.stream()
                .map(SiteNotificationService::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String maskEmail(String email) {
        String normalized = email == null ? "" : email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return normalized.charAt(0) + "***" + normalized.substring(atIndex);
    }
}
