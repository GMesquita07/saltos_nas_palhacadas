package pt.saltosnaspalhacadas.backend.usernotification;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;
import pt.saltosnaspalhacadas.backend.booking.CounterProposalDecision;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@Service
public class UserNotificationService {
    private static final int INBOX_LIMIT = 50;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", Locale.forLanguageTag("pt-PT"));

    private final UserNotificationRepository notifications;
    private final AppUserRepository users;

    public UserNotificationService(UserNotificationRepository notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void createForAdminDecision(Booking booking) {
        Instant resolvedAt = Instant.now();

        // Uma decisão administrativa resolve qualquer ação operacional pendente
        // relativa ao mesmo booking para todos os administradores.
        notifications.markReadByBookingIdAndType(
                booking.getId(),
                UserNotificationType.BOOKING_CREATED,
                resolvedAt);
        notifications.markReadByBookingIdAndType(
                booking.getId(),
                UserNotificationType.BOOKING_CUSTOMER_COUNTER_PROPOSED,
                resolvedAt);

        if (!booking.getUser().isActive()) {
            return;
        }

        // Uma nova proposta administrativa substitui a anterior para o owner.
        if (booking.getStatus() == BookingStatus.COUNTER_PROPOSED) {
            notifications.markUnreadByUserIdAndBookingIdAndType(
                    booking.getUser().getId(),
                    booking.getId(),
                    UserNotificationType.BOOKING_COUNTER_PROPOSED,
                    resolvedAt);
        }

        NotificationCopy copy = copyFor(booking);
        notifications.save(new UserNotification(
                booking.getUser(),
                booking,
                copy.type(),
                copy.title(),
                copy.message()));
    }

    @Transactional
    public void createForCustomerCounterProposal(Booking booking) {
        createForAdmins(
                booking,
                UserNotificationType.BOOKING_CUSTOMER_COUNTER_PROPOSED,
                "Contraproposta do cliente",
                "Foi recebida uma contraproposta para o agendamento #%d com %s, atualmente marcado para %s. Requer decisão da equipa."
                        .formatted(
                                booking.getId(),
                                booking.getProfile().getName(),
                                booking.getEventDate().format(DATE_FORMAT)));
    }

    @Transactional
    public void createForBookingCreated(Booking booking) {
        createForAdmins(
                booking,
                UserNotificationType.BOOKING_CREATED,
                "Novo pedido de agendamento",
                "Foi recebido o pedido #%d para %s em %s. Requer análise da equipa."
                        .formatted(
                                booking.getId(),
                                booking.getProfile().getName(),
                                booking.getEventDate().format(DATE_FORMAT)));
    }

    @Transactional
    public void createForCustomerCounterDecision(Booking booking, CounterProposalDecision decision) {
        boolean accepted = decision == CounterProposalDecision.ACCEPTED;
        createForAdmins(
                booking,
                accepted
                        ? UserNotificationType.BOOKING_CUSTOMER_COUNTER_ACCEPTED
                        : UserNotificationType.BOOKING_CUSTOMER_COUNTER_DECLINED,
                accepted ? "Cliente aceitou a proposta" : "Cliente recusou a proposta",
                accepted
                        ? "O cliente aceitou a proposta do agendamento #%d com %s."
                                .formatted(booking.getId(), booking.getProfile().getName())
                        : "O cliente recusou a proposta do agendamento #%d com %s."
                                .formatted(booking.getId(), booking.getProfile().getName()));
    }

    @Transactional
    public void createForCustomerCancellation(Booking booking) {
        Instant resolvedAt = Instant.now();
        notifications.markReadByBookingIdAndType(
                booking.getId(),
                UserNotificationType.BOOKING_CREATED,
                resolvedAt);
        notifications.markReadByBookingIdAndType(
                booking.getId(),
                UserNotificationType.BOOKING_CUSTOMER_COUNTER_PROPOSED,
                resolvedAt);

        createForAdmins(
                booking,
                UserNotificationType.BOOKING_CUSTOMER_CANCELLED,
                "Agendamento cancelado pelo cliente",
                "O cliente cancelou o agendamento #%d com %s."
                        .formatted(booking.getId(), booking.getProfile().getName()));
    }

    private void createForAdmins(
            Booking booking,
            UserNotificationType type,
            String title,
            String message) {
        for (AppUser admin : users.findAllByRoleAndActiveTrue(UserRole.ADMIN)) {
            notifications.save(new UserNotification(admin, booking, type, title, message));
        }
    }

    @Transactional
    public boolean createFiveDayReminder(Booking booking, LocalDate today) {
        if (!booking.getUser().isActive()) {
            return false;
        }
        String location = booking.getLocation() == null || booking.getLocation().isBlank()
                ? "local combinado"
                : booking.getLocation().trim();
        notifications.save(new UserNotification(
                booking.getUser(),
                booking,
                UserNotificationType.BOOKING_REMINDER_5_DAYS,
                "O teu evento está próximo",
                reminderMessage(booking, today, location)));
        return true;
    }

    private static String reminderMessage(Booking booking, LocalDate today, String location) {
        long days = ChronoUnit.DAYS.between(today, booking.getEventDate());
        String when = days == 0 ? "é hoje" : days == 1 ? "é amanhã" : "é daqui a %d dias".formatted(days);
        return "O teu evento com %s %s, em %s.".formatted(booking.getProfile().getName(), when, location);
    }

    @Transactional(readOnly = true)
    public NotificationInbox findMine(String email) {
        AppUser user = findActiveUser(email);
        List<UserNotification> items = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                user.getId(),
                PageRequest.of(0, INBOX_LIMIT));
        return new NotificationInbox(items, notifications.countByUserIdAndReadAtIsNull(user.getId()));
    }

    @Transactional
    public void markRead(String email, Long notificationId) {
        AppUser user = findActiveUser(email);
        UserNotification notification = notifications.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));
        notification.markRead(Instant.now());
    }

    @Transactional
    public void markAllRead(String email) {
        AppUser user = findActiveUser(email);
        notifications.markAllReadByUserId(user.getId(), Instant.now());
    }

    @Transactional
    public void resolveCounterProposalAction(Booking booking) {
        notifications.markUnreadByUserIdAndBookingIdAndType(
                booking.getUser().getId(),
                booking.getId(),
                UserNotificationType.BOOKING_COUNTER_PROPOSED,
                Instant.now());
    }

    private AppUser findActiveUser(String email) {
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }
        return users.findByEmailAndActiveTrue(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));
    }

    private static NotificationCopy copyFor(Booking booking) {
        String artist = booking.getProfile().getName();
        String eventDate = booking.getEventDate().format(DATE_FORMAT);
        BookingStatus status = booking.getStatus();

        return switch (status) {
            case ACCEPTED -> new NotificationCopy(
                    UserNotificationType.BOOKING_ACCEPTED,
                    "Agendamento confirmado",
                    "O teu agendamento com %s para %s foi aceite.".formatted(artist, eventDate));
            case DECLINED -> new NotificationCopy(
                    UserNotificationType.BOOKING_DECLINED,
                    "Pedido não aceite",
                    "O teu pedido de agendamento com %s para %s não foi aceite.".formatted(artist, eventDate));
            case COUNTER_PROPOSED -> new NotificationCopy(
                    UserNotificationType.BOOKING_COUNTER_PROPOSED,
                    "Nova proposta de agendamento",
                    counterProposalMessage(booking, artist));
            case CANCELLED -> new NotificationCopy(
                    UserNotificationType.BOOKING_CANCELLED,
                    "Agendamento cancelado",
                    "O agendamento com %s para %s foi cancelado pela administração.".formatted(artist, eventDate));
            case PENDING -> throw new IllegalArgumentException("O estado pendente não gera notificações in-app");
        };
    }

    private static String counterProposalMessage(Booking booking, String artist) {
        StringBuilder changes = new StringBuilder();

        if (booking.getCounterEventDate() != null) {
            changes.append("nova data de ")
                    .append(booking.getCounterEventDate().format(DATE_FORMAT));
        }

        if (booking.getCounterBudget() != null) {
            if (!changes.isEmpty()) {
                changes.append(" e ");
            }
            changes.append("orçamento proposto de ")
                    .append(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-PT"))
                            .format(booking.getCounterBudget()));
        }

        if (booking.getCounterStartTime() != null && booking.getCounterEndTime() != null) {
            if (!changes.isEmpty()) {
                changes.append(" e ");
            }
            changes.append("horário ")
                    .append(booking.getCounterStartTime())
                    .append("-")
                    .append(booking.getCounterEndTime());
        }

        if (changes.isEmpty()) {
            return "Recebeste uma nova proposta para o agendamento com %s que mantém ou restaura os termos atuais."
                    .formatted(artist);
        }

        return "Recebeste uma nova proposta para o agendamento com %s: %s."
                .formatted(artist, changes);
    }

    public record NotificationInbox(List<UserNotification> items, long unreadCount) {
    }

    private record NotificationCopy(UserNotificationType type, String title, String message) {
    }
}
