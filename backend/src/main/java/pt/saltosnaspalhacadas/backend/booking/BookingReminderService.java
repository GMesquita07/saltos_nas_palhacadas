package pt.saltosnaspalhacadas.backend.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingReminderService {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderService.class);

    private final BookingRepository bookings;
    private final BookingNotificationService notifications;
    private final int daysBefore;
    private final ZoneId reminderZone;

    public BookingReminderService(
            BookingRepository bookings,
            BookingNotificationService notifications,
            @Value("${app.booking.reminder.days-before:5}") int daysBefore,
            @Value("${app.booking.reminder.zone:Europe/Lisbon}") String reminderZone) {
        this.bookings = bookings;
        this.notifications = notifications;
        this.daysBefore = Math.max(0, daysBefore);
        this.reminderZone = ZoneId.of(reminderZone);
    }

    @Scheduled(cron = "${app.booking.reminder.cron:0 0 9 * * *}", zone = "${app.booking.reminder.zone:Europe/Lisbon}")
    @Transactional
    public void sendScheduledReminders() {
        int sent = sendDueReminders(LocalDate.now(reminderZone));
        if (sent > 0) {
            log.info("Foram preparados/enviados {} lembretes de eventos", sent);
        }
    }

    @Transactional
    public int sendDueReminders(LocalDate today) {
        LocalDate reminderDate = today.plusDays(daysBefore);
        List<Booking> dueBookings = bookings.findAcceptedBookingsDueForReminder(BookingStatus.ACCEPTED, reminderDate);
        int sentCount = 0;

        for (Booking booking : dueBookings) {
            if (notifications.sendEventReminder(booking)) {
                booking.markReminderSent(Instant.now());
                sentCount++;
            }
        }

        return sentCount;
    }
}
