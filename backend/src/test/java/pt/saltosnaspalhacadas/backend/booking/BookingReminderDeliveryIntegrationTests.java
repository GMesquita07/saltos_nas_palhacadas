package pt.saltosnaspalhacadas.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pt.saltosnaspalhacadas.backend.notification.EmailService;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;
import pt.saltosnaspalhacadas.backend.usernotification.UserNotificationRepository;

@SpringBootTest(properties = "app.booking.reminder.cron=-")
@ActiveProfiles("test")
class BookingReminderDeliveryIntegrationTests {
    private static final LocalDate TODAY = LocalDate.of(2035, 7, 2);
    private static final String CLIENT = "client-reminder@example.test";
    private static final String ARTIST = "artist-reminder@example.test";
    @Autowired private BookingRepository bookings;
    @Autowired private ProfileRepository profiles;
    @Autowired private AppUserRepository users;
    @Autowired private BookingReminderService reminders;
    @Autowired private UserNotificationRepository userNotifications;
    @MockitoBean private EmailService email;
    private AppUser user;
    private final List<Long> bookingIds = new ArrayList<>();
    private final List<Long> profileIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        user = users.save(new AppUser("reminder-" + UUID.randomUUID() + "@example.test", "unused-test-hash", UserRole.CUSTOMER));
        when(email.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        bookings.deleteAllById(bookingIds);
        profiles.deleteAllById(profileIds);
        users.deleteById(user.getId());
    }

    @Test
    void bothRecipientsReceiveDistinctEmailsOnceAndTimestampsAreCommitted() {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertThat(reminders.sendDueReminders(TODAY)).isZero();
        assertSent(booking, true, true);
        assertInApp(booking, true);
        assertThat(userNotifications.countByUserIdAndReadAtIsNull(user.getId())).isEqualTo(1);
        verify(email).send(eq(CLIENT), eq("Lembrete do teu evento"), argThat(body ->
                body.contains("Olá Cliente Teste") && body.contains("DJ Reminder")
                && body.contains("7 de julho de 2035 entre as 10:00 e as 12:00")
                && body.contains("entra em contacto")));
        verify(email).send(eq(ARTIST), eq("Saltos nas Palhaçadas: lembrete de evento #" + booking.getId()), argThat(body ->
                body.contains("Pedido: #" + booking.getId()) && body.contains("Artista: DJ Reminder")
                && body.contains("Tipo de evento: Aniversário") && body.contains("Local: Viseu")
                && body.contains("Cliente: Cliente Teste") && body.contains("Email: " + CLIENT)
                && body.contains("Telemóvel: 912345678") && body.contains("Descrição: Festa de anos")
                && body.contains("Notas: Montagem às 9h") && body.contains("10:00 e as 12:00")));
        verifyNoMoreInteractions(email);
    }

    @Test
    void artistRetriesNextDayWithoutDuplicatingSuccessfulCustomer() {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        when(email.send(eq(ARTIST), anyString(), anyString())).thenReturn(false, true);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertSent(booking, true, false);
        assertThat(reminders.sendDueReminders(TODAY.plusDays(1))).isEqualTo(1);
        assertSent(booking, true, true);
        verify(email, times(1)).send(eq(CLIENT), anyString(), anyString());
        verify(email, times(2)).send(eq(ARTIST), anyString(), anyString());
        assertThat(reminders.sendDueReminders(TODAY.plusDays(1))).isZero();
    }

    @Test
    void historicalCustomerTimestampIsPreservedWhileArtistReceivesFirstReminder() {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        Instant historical = Instant.parse("2035-07-01T08:00:00Z");
        booking.markReminderSent(historical);
        bookings.save(booking);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertSent(booking, true, true);
        assertThat(bookings.findById(booking.getId()).orElseThrow().getReminderSentAt()).isEqualTo(historical);
        verify(email).send(eq(ARTIST), anyString(), anyString());
        verifyNoMoreInteractions(email);
    }

    @Test
    void customerExceptionDoesNotBlockArtistAndOnlyCustomerIsRetried() {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        when(email.send(eq(CLIENT), anyString(), anyString())).thenThrow(new IllegalStateException("SMTP unavailable")).thenReturn(true);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertSent(booking, false, true);
        assertThat(reminders.sendDueReminders(TODAY.plusDays(1))).isEqualTo(1);
        assertSent(booking, true, true);
        verify(email, times(2)).send(eq(CLIENT), anyString(), anyString());
        verify(email, times(1)).send(eq(ARTIST), anyString(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void missingArtistEmailDoesNotBlockCustomerOrCreateRepeatedWork(String artistEmail) {
        Booking booking = booking(5, CLIENT, artistEmail, BookingStatus.ACCEPTED);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertSent(booking, true, false);
        assertThat(reminders.sendDueReminders(TODAY)).isZero();
        verify(email).send(eq(CLIENT), anyString(), anyString());
        verifyNoMoreInteractions(email);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void missingCustomerEmailStillAllowsArtistReminder(String customerEmail) {
        Booking booking = booking(5, customerEmail, ARTIST, BookingStatus.ACCEPTED);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertSent(booking, false, true);
        assertThat(reminders.sendDueReminders(TODAY)).isZero();
        verify(email).send(eq(ARTIST), anyString(), anyString());
        verifyNoMoreInteractions(email);
    }

    @Test
    void missingEmailRecipientsStillCreatesTheInAppReminder() {
        Booking booking = booking(5, null, null, BookingStatus.ACCEPTED);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertInApp(booking, true);
        verifyNoInteractions(email);
    }

    @Test
    void inactiveCustomerNeverReceivesAnInAppReminder() {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        user.anonymizeForDeletion("deleted-" + UUID.randomUUID() + "@example.invalid", "disabled-password");
        users.save(user);

        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);

        assertSent(booking, true, true);
        assertInApp(booking, false);
        assertThat(userNotifications.countByUserIdAndReadAtIsNull(user.getId())).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"PENDING", "DECLINED", "CANCELLED", "COUNTER_PROPOSED"})
    void nonAcceptedBookingsNeverSend(BookingStatus status) {
        Booking booking = booking(5, CLIENT, ARTIST, status);
        assertThat(reminders.sendDueReminders(TODAY)).isZero();
        assertSent(booking, false, false);
        assertInApp(booking, false);
        verifyNoInteractions(email);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2, 5, 6})
    void windowIncludesTodayAndFiveDaysButExcludesPastAndLaterEvents(int days) {
        Booking booking = booking(days, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        boolean eligible = days >= 0 && days <= 5;
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(eligible ? 1 : 0);
        assertSent(booking, eligible, eligible);
        assertInApp(booking, eligible);
        verify(email, times(eligible ? 2 : 0)).send(anyString(), anyString(), anyString());
    }

    @Test
    void retriesStopAfterEventDay() {
        Booking booking = booking(0, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        when(email.send(anyString(), anyString(), anyString())).thenReturn(false);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
        assertThat(reminders.sendDueReminders(TODAY.plusDays(1))).isZero();
        assertSent(booking, false, false);
        assertInApp(booking, true);
        verify(email, times(2)).send(anyString(), anyString(), anyString());
    }

    @Test
    void reschedulingReacceptanceAndCounterProposalResetBothButIdenticalAcceptanceDoesNot() {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        reminders.sendDueReminders(TODAY);
        booking = bookings.findById(booking.getId()).orElseThrow();
        booking.accept(TODAY.plusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0), null, "Nota atualizada");
        bookings.save(booking);
        assertThat(reminders.sendDueReminders(TODAY)).isZero();

        booking.accept(TODAY.plusDays(4), LocalTime.of(11, 0), LocalTime.of(13, 0), null, "Reagendado");
        bookings.save(booking);
        assertSent(booking, false, false);
        assertInApp(booking, false);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);

        booking = bookings.findById(booking.getId()).orElseThrow();
        booking.cancel("Cancelado");
        booking.accept(TODAY.plusDays(4), LocalTime.of(11, 0), LocalTime.of(13, 0), null, "Reaceite");
        bookings.save(booking);
        assertSent(booking, false, false);
        assertInApp(booking, false);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);

        booking = bookings.findById(booking.getId()).orElseThrow();
        booking.counterPropose("Outra data", null, TODAY.plusDays(3));
        booking.acceptCounterProposal(TODAY.plusDays(3), null);
        bookings.save(booking);
        assertSent(booking, false, false);
        assertInApp(booking, false);
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(1);
    }

    @Test
    void failingBookingDoesNotPreventOtherBookingsFromBeingProcessed() {
        Booking failed = booking(5, "failure@example.test", "failed-artist@example.test", BookingStatus.ACCEPTED);
        Booking successful = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        when(email.send(eq("failure@example.test"), anyString(), anyString())).thenThrow(new IllegalStateException("SMTP unavailable"));
        when(email.send(eq("failed-artist@example.test"), anyString(), anyString())).thenThrow(new IllegalStateException("SMTP unavailable"));
        assertThat(reminders.sendDueReminders(TODAY)).isEqualTo(2);
        assertSent(failed, false, false);
        assertSent(successful, true, true);
        assertInApp(failed, true);
        assertInApp(successful, true);
        verify(email, times(4)).send(anyString(), anyString(), anyString());
    }

    @Test
    @EnabledIfEnvironmentVariable(
            named = "SPRING_DATASOURCE_DRIVER_CLASS_NAME",
            matches = "org\\.postgresql\\.Driver",
            disabledReason = "Concurrency locking semantics are validated by the PostgreSQL CI job; H2 does not reliably reproduce them")
    void overlappingExecutionsDoNotDuplicateEmails() throws Exception {
        Booking booking = booking(5, CLIENT, ARTIST, BookingStatus.ACCEPTED);
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        when(email.send(eq(CLIENT), anyString(), anyString())).thenAnswer(invocation -> {
            sending.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return true;
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> reminders.sendDueReminders(TODAY));
            try {
                assertThat(sending.await(5, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> {
                    secondStarted.countDown();
                    return reminders.sendDueReminders(TODAY);
                });
                assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
                release.countDown();
                assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(1);
            } finally {
                release.countDown();
            }
        }
        assertSent(booking, true, true);
        verify(email, times(1)).send(eq(CLIENT), anyString(), anyString());
        verify(email, times(1)).send(eq(ARTIST), anyString(), anyString());
    }

    private Booking booking(int days, String customerEmail, String artistEmail, BookingStatus status) {
        Profile profile = profiles.save(new Profile("reminder-" + UUID.randomUUID(), "DJ Reminder", "DJ", "Teste", null,
                "50% 50%", 1, null, 0, artistEmail));
        profileIds.add(profile.getId());
        Booking booking = new Booking(user, profile, TODAY.plusDays(days), LocalTime.of(10, 0), LocalTime.of(12, 0),
                BookingEventType.BIRTHDAY, null, null, "Viseu", "Cliente Teste", customerEmail, "912345678", "Festa de anos", "Montagem às 9h");
        switch (status) {
            case ACCEPTED -> booking.accept(booking.getEventDate(), booking.getStartTime(), booking.getEndTime(), null, null);
            case DECLINED -> booking.decline("Não aceite");
            case CANCELLED -> booking.cancel("Cancelado");
            case COUNTER_PROPOSED -> booking.counterPropose("Outra data", null, TODAY.plusDays(6));
            case PENDING -> { }
        }
        booking = bookings.save(booking);
        bookingIds.add(booking.getId());
        return booking;
    }

    private void assertSent(Booking booking, boolean customer, boolean artist) {
        Booking saved = bookings.findById(booking.getId()).orElseThrow();
        assertThat(saved.getReminderSentAt() != null).isEqualTo(customer);
        assertThat(saved.getArtistReminderSentAt() != null).isEqualTo(artist);
    }

    private void assertInApp(Booking booking, boolean sent) {
        Booking saved = bookings.findById(booking.getId()).orElseThrow();
        assertThat(saved.getCustomerInAppReminderSentAt() != null).isEqualTo(sent);
    }
}
