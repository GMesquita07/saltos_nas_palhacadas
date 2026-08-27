package pt.saltosnaspalhacadas.backend.booking;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import pt.saltosnaspalhacadas.backend.notification.EmailService;

@Service
public class BookingNotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final EmailService emailService;

    public BookingNotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendReceivedConfirmation(Booking booking) {
        send(booking, "Pedido de agendamento recebido", """
                Olá %s,

                Recebemos o teu pedido de orçamento e agendamento para %s.

                O animador vai analisar a informação enviada e entrará em contacto contigo por email ou telemóvel.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(
                booking.getContactName(),
                DATE_FORMATTER.format(booking.getEventDate())));
    }

    public void sendDecisionNotification(Booking booking) {
        if (booking.getStatus() == BookingStatus.ACCEPTED) {
            sendAccepted(booking);
        } else if (booking.getStatus() == BookingStatus.DECLINED) {
            sendDeclined(booking);
        } else if (booking.getStatus() == BookingStatus.COUNTER_PROPOSED) {
            sendCounterProposal(booking);
        } else if (booking.getStatus() == BookingStatus.CANCELLED) {
            sendCancelled(booking);
        }
    }

    public void sendCounterProposalResponseConfirmation(Booking booking, CounterProposalDecision decision) {
        if (decision == CounterProposalDecision.ACCEPTED) {
            sendAccepted(booking);
            return;
        }

        send(booking, "Contraproposta recusada", """
                Olá %s,

                A tua resposta à alteração proposta foi registada e o pedido foi encerrado.

                Se quiseres fazer um novo pedido, podes voltar ao calendário no site.

                Obrigado,
                Saltos nas Palhaçadas
                """.formatted(booking.getContactName()));
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

        return emailService.send(booking.getContactEmail(), subject, body);
    }

    private void sendAccepted(Booking booking) {
        send(booking, "Pedido de agendamento aceite", """
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
        send(booking, "Pedido de agendamento não aceite", """
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
        send(booking, "Alteração proposta ao teu pedido", """
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
        send(booking, "Agendamento cancelado", """
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

    private void send(Booking booking, String subject, String body) {
        emailService.send(booking.getContactEmail(), subject, body);
    }

    private static String formatSchedule(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return "";
        }
        return " entre as %s e as %s".formatted(
                TIME_FORMATTER.format(booking.getStartTime()),
                TIME_FORMATTER.format(booking.getEndTime()));
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

    private static String adminMessageOrDefault(Booking booking, String fallback) {
        return booking.getAdminMessage() == null || booking.getAdminMessage().isBlank()
                ? fallback
                : booking.getAdminMessage();
    }

    private static String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-PT")).format(value);
    }
}
