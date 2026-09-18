package pt.saltosnaspalhacadas.backend.booking;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import pt.saltosnaspalhacadas.backend.notification.SiteNotificationService;

@Service
public class BookingNotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final SiteNotificationService siteNotifications;

    public BookingNotificationService(SiteNotificationService siteNotifications) {
        this.siteNotifications = siteNotifications;
    }

    public void sendReceivedConfirmation(Booking booking) {
        sendCustomer(booking, "Pedido de agendamento recebido", """
                Olá %s,

                Recebemos o teu pedido de orçamento e agendamento para %s.

                A equipa irá analisar o teu pedido e entrará em contacto contigo por email ou telemóvel.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                DATE_FORMATTER.format(booking.getEventDate())));

        notifyBookingStakeholders(
                booking,
                "booking-created",
                "novo pedido de agendamento",
                "Foi submetido um novo pedido de agendamento.");
    }

    public void sendDecisionNotification(Booking booking) {
        if (booking.getStatus() == BookingStatus.ACCEPTED) {
            sendAccepted(booking);
            notifyBookingStakeholders(booking, "booking-accepted", "pedido aceite", "O pedido foi aceite pela administração.");
        } else if (booking.getStatus() == BookingStatus.DECLINED) {
            sendDeclined(booking);
            notifyBookingStakeholders(booking, "booking-declined", "pedido não aceite", "O pedido foi marcado como não aceite.");
        } else if (booking.getStatus() == BookingStatus.COUNTER_PROPOSED) {
            sendCounterProposal(booking);
            notifyBookingStakeholders(booking, "booking-counter-proposed", "alteração proposta", "A administração propôs uma alteração ao pedido.");
        } else if (booking.getStatus() == BookingStatus.CANCELLED) {
            sendCancelled(booking);
            notifyBookingStakeholders(booking, "booking-cancelled", "agendamento cancelado", "O agendamento foi cancelado.");
        }
    }

    public void sendCounterProposalResponseConfirmation(Booking booking, CounterProposalDecision decision) {
        if (decision == CounterProposalDecision.ACCEPTED) {
            sendAccepted(booking);
            notifyBookingStakeholders(
                    booking,
                    "booking-counter-accepted",
                    "contraproposta aceite",
                    "O cliente aceitou a contraproposta.");
            return;
        }

        sendCustomer(booking, "Contraproposta recusada", """
                Olá %s,

                A tua resposta à alteração proposta foi registada e o pedido foi encerrado.

                Se quiseres fazer um novo pedido, podes voltar ao calendário no site.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(booking.getContactName()));
        notifyBookingStakeholders(
                booking,
                "booking-counter-declined",
                "contraproposta recusada",
                "O cliente recusou a contraproposta.");
    }

    public boolean sendEventReminder(Booking booking) {
        if (booking.getContactEmail() == null || booking.getContactEmail().isBlank()) {
            return false;
        }

        String subject = "Lembrete do teu evento";
        String body = """
                Olá %s,

                Este é um lembrete do teu evento com %s, marcado para %s%s.

                Se existir alguma alteração, questão ou detalhe de última hora, entra em contacto connosco por email ou telemóvel.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                booking.getProfile().getName(),
                DATE_FORMATTER.format(booking.getEventDate()),
                formatSchedule(booking));

        return siteNotifications.sendBestEffort("booking-reminder", booking.getContactEmail(), subject, body);
    }

    private void sendAccepted(Booking booking) {
        sendCustomer(booking, "Pedido de agendamento aceite", """
                Olá %s,

                O teu pedido para %s com %s foi aceite%s.

                %s

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                DATE_FORMATTER.format(booking.getEventDate()),
                booking.getProfile().getName(),
                formatSchedule(booking),
                adminMessageOrDefault(booking, "A equipa entrará em contacto se for necessário confirmar algum detalhe.")));
    }

    private void sendDeclined(Booking booking) {
        sendCustomer(booking, "Pedido de agendamento não aceite", """
                Olá %s,

                Depois de analisar o teu pedido para %s, a equipa não conseguiu aceitar este agendamento.

                %s

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                DATE_FORMATTER.format(booking.getEventDate()),
                adminMessageOrDefault(booking, "Podes consultar outras datas ou entrar em contacto connosco para avaliar alternativas.")));
    }

    private void sendCounterProposal(Booking booking) {
        sendCustomer(booking, "Alteração proposta ao teu pedido", """
                Olá %s,

                A equipa analisou o teu pedido e propôs uma alteração.

                %s

                %s

                Entra na tua conta para aceitares ou recusares esta alteração.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                formatCounterProposal(booking),
                adminMessageOrDefault(booking, "Confirma no site se a alteração funciona para o teu evento.")));
    }

    private void sendCancelled(Booking booking) {
        sendCustomer(booking, "Agendamento cancelado", """
                Olá %s,

                O agendamento para %s com %s foi cancelado.

                %s

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                DATE_FORMATTER.format(booking.getEventDate()),
                booking.getProfile().getName(),
                adminMessageOrDefault(booking, "Se precisares de esclarecer alguma questão, entra em contacto connosco.")));
    }

    private void sendCustomer(Booking booking, String subject, String body) {
        siteNotifications.sendBestEffort("booking-customer", booking.getContactEmail(), subject, body);
    }

    private void notifyBookingStakeholders(Booking booking, String eventType, String subjectSuffix, String summary) {
        String artistEmail = booking.getProfile().getNotificationEmail();
        String subject = "Saltos nas Palhaçadas: %s #%d".formatted(subjectSuffix, booking.getId());
        String body = """
                %s

                Pedido: #%d
                Estado: %s
                Artista: %s
                Data: %s%s
                Tipo de evento: %s
                Local: %s
                Contacto: %s
                Email: %s
                Telemóvel: %s
                Descrição: %s
                Notas: %s
                Mensagem administrativa: %s
                %s
                """.formatted(
                summary,
                booking.getId(),
                booking.getStatus(),
                booking.getProfile().getName(),
                DATE_FORMATTER.format(booking.getEventDate()),
                formatSchedule(booking),
                formatEventType(booking),
                blankFallback(booking.getLocation()),
                booking.getContactName(),
                blankFallback(booking.getContactEmail()),
                booking.getContactPhone(),
                booking.getDescription(),
                blankFallback(booking.getNotes()),
                blankFallback(booking.getAdminMessage()),
                formatCounterProposalForOperations(booking));

        siteNotifications.sendBestEffort(eventType + "-artist", artistEmail, subject, body);
        siteNotifications.notifyAdmins(eventType, subject, body, artistEmail == null ? List.of() : List.of(artistEmail));
    }

    private static String formatSchedule(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return "";
        }
        return " entre as %s e as %s".formatted(
                TIME_FORMATTER.format(booking.getStartTime()),
                TIME_FORMATTER.format(booking.getEndTime()));
    }

    private static String formatEventType(Booking booking) {
        if (booking.getEventType() == BookingEventType.OTHER && booking.getCustomEventType() != null) {
            return booking.getCustomEventType();
        }
        return switch (booking.getEventType()) {
            case WEDDING -> "Casamento" + (booking.getWeddingCoupleNames() == null ? "" : " (" + booking.getWeddingCoupleNames() + ")");
            case BAPTISM -> "Batizado";
            case BIRTHDAY -> "Aniversário";
            case CORPORATE -> "Evento empresarial";
            case FESTIVAL -> "Festival";
            case PRIVATE_PARTY -> "Festa privada";
            case OTHER -> "Outro";
        };
    }

    private static String formatCounterProposal(Booking booking) {
        StringBuilder builder = new StringBuilder();
        if (booking.getCounterEventDate() != null) {
            builder.append("Nova data proposta: ").append(DATE_FORMATTER.format(booking.getCounterEventDate())).append(".");
        }
        if (booking.getCounterBudget() != null) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append("Orçamento proposto: ").append(formatCurrency(booking.getCounterBudget())).append(".");
        }
        return builder.isEmpty() ? "A equipa deixou uma mensagem sobre o teu pedido." : builder.toString();
    }

    private static String formatCounterProposalForOperations(Booking booking) {
        if (booking.getCounterBudget() == null && booking.getCounterEventDate() == null) {
            return "Contraproposta: -";
        }
        return "Contraproposta: %s".formatted(formatCounterProposal(booking).replace("\n", " "));
    }

    private static String adminMessageOrDefault(Booking booking, String fallback) {
        return booking.getAdminMessage() == null || booking.getAdminMessage().isBlank()
                ? fallback
                : booking.getAdminMessage();
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-PT")).format(value);
    }
}
