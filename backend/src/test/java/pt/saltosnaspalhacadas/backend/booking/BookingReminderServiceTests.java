package pt.saltosnaspalhacadas.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BookingReminderServiceTests {

    @Test
    void sendDueRemindersNowUsesConfiguredZoneAndReturnsSentCount() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationService notifications = mock(BookingNotificationService.class);
        BookingReminderService service = new BookingReminderService(bookings, notifications, 5, "Europe/Lisbon");
        Booking booking = mock(Booking.class);
        LocalDate reminderDate = LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(5);
        when(bookings.findAcceptedBookingsDueForReminder(BookingStatus.ACCEPTED, reminderDate)).thenReturn(List.of(booking));
        when(notifications.sendEventReminder(booking)).thenReturn(true);

        int sent = service.sendDueRemindersNow();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<LocalDate> reminderDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(bookings).findAcceptedBookingsDueForReminder(eq(BookingStatus.ACCEPTED), reminderDateCaptor.capture());
        assertThat(reminderDateCaptor.getValue()).isEqualTo(reminderDate);
        verify(notifications).sendEventReminder(booking);
        verify(booking).markReminderSent(any(Instant.class));
    }
}
