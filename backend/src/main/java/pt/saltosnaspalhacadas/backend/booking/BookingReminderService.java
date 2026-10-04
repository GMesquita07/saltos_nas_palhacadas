package pt.saltosnaspalhacadas.backend.booking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookingReminderService {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderService.class);

    private final BookingRepository bookings;
    private final BookingNotificationService notifications;
    private final int daysBefore;
    private final Clock clock;
    private final TransactionTemplate transaction;

    @Autowired
    public BookingReminderService(
            BookingRepository bookings,
            BookingNotificationService notifications,
            PlatformTransactionManager transactionManager,
            @Value("${app.booking.reminder.days-before:5}") int daysBefore,
            @Value("${app.booking.reminder.zone:Europe/Lisbon}") String reminderZone) {
        this(bookings, notifications, transactionManager, daysBefore, Clock.system(ZoneId.of(reminderZone)));
    }

    BookingReminderService(BookingRepository bookings, BookingNotificationService notifications,
            PlatformTransactionManager transactionManager, int daysBefore, Clock clock) {
        this.bookings = bookings;
        this.notifications = notifications;
        this.daysBefore = Math.max(0, daysBefore);
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "${app.booking.reminder.cron:0 0 9 * * *}", zone = "${app.booking.reminder.zone:Europe/Lisbon}")
    public void sendScheduledReminders() {
        sendDueRemindersNow();
    }

    public int sendDueRemindersNow() {
        int sent = sendDueRemindersForDate(LocalDate.now(clock));
        if (sent > 0) {
            log.info("{} agendamentos tiveram pelo menos um lembrete aceite pelo serviço de email", sent);
        }
        return sent;
    }

    public int sendDueReminders(LocalDate today) {
        return sendDueRemindersForDate(today);
    }

    private int sendDueRemindersForDate(LocalDate today) {
        LocalDate reminderDate = today.plusDays(daysBefore);
        List<Long> dueBookings = bookings.findAcceptedBookingsDueForReminder(BookingStatus.ACCEPTED, today, reminderDate);
        int sentCount = 0;

        for (Long bookingId : dueBookings) {
            try {
                // Commit each booking independently; recheck under a row lock for overlapping jobs.
                boolean sent = Boolean.TRUE.equals(transaction.execute(status ->
                        sendForBooking(bookingId, today, reminderDate)));
                if (sent) sentCount++;
            } catch (RuntimeException exception) {
                log.warn("Falha ao processar lembretes do agendamento #{}", bookingId, exception);
            }
        }

        return sentCount;
    }

    private boolean sendForBooking(Long bookingId, LocalDate today, LocalDate through) {
        Booking booking = bookings.findByIdForReminder(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.ACCEPTED
                || booking.getEventDate().isBefore(today) || booking.getEventDate().isAfter(through)) {
            return false;
        }
        boolean sent = false;
        if (booking.needsCustomerReminder() && notifications.sendEventReminder(booking)) {
            booking.markReminderSent(clock.instant());
            sent = true;
        }
        if (booking.needsArtistReminder() && notifications.sendArtistEventReminder(booking)) {
            booking.markArtistReminderSent(clock.instant());
            sent = true;
        }
        return sent;
    }
}
