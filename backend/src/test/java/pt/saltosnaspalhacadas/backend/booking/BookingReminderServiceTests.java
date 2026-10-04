package pt.saltosnaspalhacadas.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

class BookingReminderServiceTests {

    @Test
    void sendDueRemindersNowUsesConfiguredZoneAndReturnsSentCount() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationService notifications = mock(BookingNotificationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-01T23:30:00Z"), ZoneId.of("Europe/Lisbon"));
        BookingReminderService service = new BookingReminderService(bookings, notifications,
                mock(PlatformTransactionManager.class), 5, clock);
        Booking booking = mock(Booking.class);
        LocalDate today = LocalDate.of(2026, 7, 2);
        LocalDate reminderDate = today.plusDays(5);
        when(bookings.findAcceptedBookingsDueForReminder(BookingStatus.ACCEPTED, today, reminderDate)).thenReturn(List.of(1L));
        when(bookings.findByIdForReminder(1L)).thenReturn(Optional.of(booking));
        when(booking.getStatus()).thenReturn(BookingStatus.ACCEPTED);
        when(booking.getEventDate()).thenReturn(reminderDate);
        when(booking.needsCustomerReminder()).thenReturn(true);
        when(notifications.sendEventReminder(booking)).thenReturn(true);

        int sent = service.sendDueRemindersNow();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<LocalDate> reminderDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(bookings).findAcceptedBookingsDueForReminder(eq(BookingStatus.ACCEPTED), eq(today), reminderDateCaptor.capture());
        assertThat(reminderDateCaptor.getValue()).isEqualTo(reminderDate);
        verify(notifications).sendEventReminder(booking);
        verify(booking).markReminderSent(any(Instant.class));
    }
}
