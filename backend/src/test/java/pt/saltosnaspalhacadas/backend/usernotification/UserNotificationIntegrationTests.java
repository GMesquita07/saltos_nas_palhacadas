package pt.saltosnaspalhacadas.backend.usernotification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingRepository;
import pt.saltosnaspalhacadas.backend.notification.EmailService;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserNotificationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private AppUserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private BookingRepository bookings;
    @Autowired private UserNotificationRepository notifications;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private EmailService emailService;

    @BeforeEach
    void prepareEmailMockAndAdmin() {
        AppUser admin = users.findByEmailAndActiveTrue("admin@example.test")
                .orElseGet(() -> users.save(new AppUser(
                        "admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN)));
        notifications.deleteAll(notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                admin.getId(), org.springframework.data.domain.Pageable.unpaged()));
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void onlyAdministrativeBookingDecisionsCreateTheExpectedNotifications() throws Exception {
        TestData data = createTestData();
        try {
            Long accepted = createBooking(data, 30);
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isZero();
            decide(accepted, "{\"status\":\"ACCEPTED\"}");

            Long declined = createBooking(data, 31);
            decide(declined, "{\"status\":\"DECLINED\",\"message\":\"Sem disponibilidade.\"}");

            Long counter = createBooking(data, 32);
            decide(counter, "{\"status\":\"COUNTER_PROPOSED\",\"message\":\"Nova proposta.\",\"counterBudget\":250.00,\"counterEventDate\":\"%s\"}"
                    .formatted(LocalDate.now().plusDays(33)));

            Long cancelled = createBooking(data, 34);
            decide(cancelled, "{\"status\":\"CANCELLED\",\"message\":\"Evento indisponível.\"}");

            List<UserNotification> created = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    data.customer().getId(), org.springframework.data.domain.Pageable.unpaged());
            assertThat(created).hasSize(4);
            assertThat(created).extracting(UserNotification::getType).containsExactlyInAnyOrder(
                    UserNotificationType.BOOKING_ACCEPTED,
                    UserNotificationType.BOOKING_DECLINED,
                    UserNotificationType.BOOKING_COUNTER_PROPOSED,
                    UserNotificationType.BOOKING_CANCELLED);
            assertThat(created).allSatisfy(notification -> {
                assertThat(notification.getUser().getId()).isEqualTo(data.customer().getId());
                assertThat(notification.getBooking()).isNotNull();
                assertThat(notification.getMessage()).contains(data.profile().getName());
            });
            assertThat(created.stream()
                    .filter(notification -> notification.getType() == UserNotificationType.BOOKING_COUNTER_PROPOSED)
                    .findFirst()
                    .orElseThrow()
                    .getMessage())
                    .contains("orçamento proposto")
                    .contains("250");

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", counter)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"DECLINED\"}"))
                    .andExpect(status().isOk());

            Long customerCancelled = createBooking(data, 35);
            mockMvc.perform(put("/api/v1/bookings/{id}/cancel", customerCancelled)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Já não é necessário.\"}"))
                    .andExpect(status().isOk());

            assertThat(notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    data.customer().getId(), org.springframework.data.domain.Pageable.unpaged())).hasSize(4);
        } finally {
            cleanup(data);
        }
    }

    @Test
    void inboxIsPrivateOrderedAndSupportsIdempotentReadOperations() throws Exception {
        TestData owner = createTestData();
        TestData other = createTestData();
        try {
            Long firstBooking = createBooking(owner, 40);
            decide(firstBooking, "{\"status\":\"DECLINED\",\"message\":\"Sem disponibilidade.\"}");
            Long secondBooking = createBooking(owner, 41);
            decide(secondBooking, "{\"status\":\"CANCELLED\",\"message\":\"Cancelado.\"}");
            Long otherBooking = createBooking(other, 42);
            decide(otherBooking, "{\"status\":\"ACCEPTED\"}");

            List<UserNotification> ownerNotifications = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    owner.customer().getId(), org.springframework.data.domain.Pageable.unpaged());
            List<UserNotification> expectedOrder = ownerNotifications.stream()
                    .sorted(Comparator.comparing(UserNotification::getCreatedAt).reversed()
                            .thenComparing(UserNotification::getId, Comparator.reverseOrder()))
                    .toList();
            assertThat(ownerNotifications).extracting(UserNotification::getId)
                    .containsExactlyElementsOf(expectedOrder.stream().map(UserNotification::getId).toList());

            mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(owner.customer())))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.notifications.length()").value(2))
                    .andExpect(jsonPath("$.unreadCount").value(2))
                    .andExpect(jsonPath("$.notifications[0].id").value(ownerNotifications.get(0).getId()))
                    .andExpect(jsonPath("$.notifications[0].read").value(false));

            Long ownerNotificationId = ownerNotifications.get(0).getId();
            mockMvc.perform(patch("/api/v1/notifications/{id}/read", ownerNotificationId)
                            .header("Authorization", bearer(owner.customer())))
                    .andExpect(status().isNoContent());
            mockMvc.perform(patch("/api/v1/notifications/{id}/read", ownerNotificationId)
                            .header("Authorization", bearer(owner.customer())))
                    .andExpect(status().isNoContent());
            assertThat(notifications.countByUserIdAndReadAtIsNull(owner.customer().getId())).isEqualTo(1);

            Long otherNotificationId = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    other.customer().getId(), org.springframework.data.domain.Pageable.unpaged()).get(0).getId();
            mockMvc.perform(patch("/api/v1/notifications/{id}/read", otherNotificationId)
                            .header("Authorization", bearer(owner.customer())))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/v1/notifications/read-all")
                            .header("Authorization", bearer(owner.customer())))
                    .andExpect(status().isNoContent());
            assertThat(notifications.countByUserIdAndReadAtIsNull(owner.customer().getId())).isZero();
            assertThat(notifications.countByUserIdAndReadAtIsNull(other.customer().getId())).isEqualTo(1);

            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isForbidden());
        } finally {
            cleanup(owner);
            cleanup(other);
        }
    }

    @Test
    void inboxReturnsLatestFiftyWithDeterministicOrderAndFullUnreadCount() throws Exception {
        TestData data = createTestData();
        try {
            for (int index = 0; index < 55; index++) {
                notifications.saveAndFlush(new UserNotification(
                        data.customer(),
                        null,
                        UserNotificationType.BOOKING_ACCEPTED,
                        "Notificação " + index,
                        "Mensagem " + index));
            }

            List<UserNotification> expected = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    data.customer().getId(),
                    org.springframework.data.domain.PageRequest.of(0, 50));

            mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications.length()").value(50))
                    .andExpect(jsonPath("$.unreadCount").value(55))
                    .andExpect(jsonPath("$.notifications[0].id").value(expected.getFirst().getId()))
                    .andExpect(jsonPath("$.notifications[49].id").value(expected.getLast().getId()));

            assertThat(expected).extracting(UserNotification::getId).isSortedAccordingTo(Comparator.reverseOrder());
        } finally {
            cleanup(data);
        }
    }

    @Test
    void customerCounterProposalCreatesAdminInboxNotificationAndAdminDecisionResolvesIt() throws Exception {
        TestData data = createTestData();
        try {
            Long bookingId = createBooking(data, 58);
            decide(bookingId, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal", bookingId)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"counterBudget\":350,\"message\":\"Prefiro este valor.\"}"))
                    .andExpect(status().isOk());

            AppUser admin = admin();
            List<UserNotification> adminItems = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    admin.getId(), org.springframework.data.domain.Pageable.unpaged());
            UserNotification counterNotification = adminItems.stream()
                    .filter(notification -> notification.getType()
                            == UserNotificationType.BOOKING_CUSTOMER_COUNTER_PROPOSED)
                    .findFirst()
                    .orElseThrow();
            assertThat(counterNotification.getBooking().getId()).isEqualTo(bookingId);
            assertThat(counterNotification.isRead()).isFalse();

            mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(1))
                    .andExpect(jsonPath("$.notifications[0].type")
                            .value("BOOKING_CUSTOMER_COUNTER_PROPOSED"));

            decide(bookingId, "{\"status\":\"ACCEPTED\"}");

            assertThat(notifications.countByUserIdAndReadAtIsNull(admin.getId())).isZero();
            assertThat(notifications.findById(counterNotification.getId()).orElseThrow().isRead()).isTrue();
        } finally {
            cleanup(data);
        }
    }

    @Test
    void adminInboxCoversNewBookingCustomerResponsesAndCancellation() throws Exception {
        TestData data = createTestData();
        try {
            AppUser admin = admin();

            Long newBooking = createBooking(data, 63);
            List<UserNotification> afterCreate = notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    admin.getId(), org.springframework.data.domain.Pageable.unpaged());
            UserNotification createdNotification = afterCreate.stream()
                    .filter(notification -> notification.getType() == UserNotificationType.BOOKING_CREATED)
                    .findFirst()
                    .orElseThrow();
            assertThat(createdNotification.isRead()).isFalse();

            mockMvc.perform(put("/api/v1/bookings/{id}/cancel", newBooking)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Mudança de planos.\"}"))
                    .andExpect(status().isOk());

            assertThat(notifications.findById(createdNotification.getId()).orElseThrow().isRead()).isTrue();
            assertThat(notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    admin.getId(), org.springframework.data.domain.Pageable.unpaged()))
                    .extracting(UserNotification::getType)
                    .contains(UserNotificationType.BOOKING_CUSTOMER_CANCELLED);

            Long acceptedByCustomer = createBooking(data, 64);
            decide(acceptedByCustomer, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");
            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", acceptedByCustomer)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"ACCEPTED\"}"))
                    .andExpect(status().isOk());

            Long declinedByCustomer = createBooking(data, 65);
            decide(declinedByCustomer, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");
            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", declinedByCustomer)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"DECLINED\"}"))
                    .andExpect(status().isOk());

            assertThat(notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    admin.getId(), org.springframework.data.domain.Pageable.unpaged()))
                    .extracting(UserNotification::getType)
                    .contains(
                            UserNotificationType.BOOKING_CUSTOMER_COUNTER_ACCEPTED,
                            UserNotificationType.BOOKING_CUSTOMER_COUNTER_DECLINED,
                            UserNotificationType.BOOKING_CUSTOMER_CANCELLED);
        } finally {
            cleanup(data);
        }
    }

    @Test
    void customerActionsResolveUnreadAdminCounterProposalNotifications() throws Exception {
        TestData data = createTestData();
        try {
            Long revised = createBooking(data, 59);
            decide(revised, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");
            decide(revised, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":350}");

            List<UserNotification> revisionNotifications = notifications
                    .findByUserIdOrderByCreatedAtDescIdDesc(
                            data.customer().getId(), org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(notification -> notification.getBooking() != null
                            && revised.equals(notification.getBooking().getId())
                            && notification.getType() == UserNotificationType.BOOKING_COUNTER_PROPOSED)
                    .toList();
            assertThat(revisionNotifications).hasSize(2);
            assertThat(revisionNotifications).filteredOn(UserNotification::isRead).hasSize(1);
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isEqualTo(1);

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", revised)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"ACCEPTED\"}"))
                    .andExpect(status().isOk());
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isZero();

            Long accepted = createBooking(data, 60);
            decide(accepted, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isEqualTo(1);
            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", accepted)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"ACCEPTED\"}"))
                    .andExpect(status().isOk());
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isZero();

            Long countered = createBooking(data, 61);
            decide(countered, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");
            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal", countered)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"counterBudget\":350}"))
                    .andExpect(status().isOk());
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isZero();

            Long cancelled = createBooking(data, 62);
            decide(cancelled, "{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":300}");
            mockMvc.perform(put("/api/v1/bookings/{id}/cancel", cancelled)
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Já não pretendo avançar.\"}"))
                    .andExpect(status().isOk());
            assertThat(notifications.countByUserIdAndReadAtIsNull(data.customer().getId())).isZero();
        } finally {
            cleanup(data);
        }
    }

    @Test
    void accountExportIncludesNotificationsAndDeletionRemovesThem() throws Exception {
        TestData data = createTestData();
        try {
            Long bookingId = createBooking(data, 50);
            decide(bookingId, "{\"status\":\"DECLINED\",\"message\":\"Sem disponibilidade.\"}");

            Long pendingBookingId = createBooking(data, 51);

            mockMvc.perform(get("/api/v1/auth/me/export").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications.length()").value(1))
                    .andExpect(jsonPath("$.notifications[0].type").value("BOOKING_DECLINED"))
                    .andExpect(jsonPath("$.notifications[0].bookingId").value(bookingId));

            mockMvc.perform(delete("/api/v1/auth/me")
                            .header("Authorization", bearer(data.customer()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password-segura\"}"))
                    .andExpect(status().isNoContent());

            assertThat(notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    data.customer().getId(), org.springframework.data.domain.Pageable.unpaged())).isEmpty();
            assertThat(bookings.findAllByUserIdOrderByCreatedAtDesc(data.customer().getId())).hasSize(2);

            decide(pendingBookingId, "{\"status\":\"DECLINED\",\"message\":\"Conta eliminada.\"}");
            assertThat(notifications.findByUserIdOrderByCreatedAtDescIdDesc(
                    data.customer().getId(), org.springframework.data.domain.Pageable.unpaged())).isEmpty();
        } finally {
            cleanup(data);
        }
    }

    private TestData createTestData() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "inbox-" + suffix + "@example.test",
                "inbox." + suffix,
                "Cliente",
                "Inbox",
                "912345678",
                null,
                passwords.encode("password-segura"),
                UserRole.CUSTOMER));
        Profile profile = profiles.save(new Profile(
                "inbox-" + suffix,
                "Artista " + suffix,
                "DJ",
                "Perfil para testar notificações in-app",
                null));
        return new TestData(customer, profile);
    }

    private Long createBooking(TestData data, int daysFromNow) throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", bearer(data.customer()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileSlug":"%s","eventDate":"%s","startTime":"10:00","endTime":"12:00","eventType":"BIRTHDAY","location":"Lisboa","contactName":"Cliente Inbox","contactEmail":"%s","contactPhone":"912345678","description":"Evento de teste para inbox"}
                                """.formatted(data.profile().getSlug(), LocalDate.now().plusDays(daysFromNow), data.customer().getEmail())))
                .andExpect(status().isCreated());
        return bookings.findAllByUserIdOrderByCreatedAtDesc(data.customer().getId()).get(0).getId();
    }

    private void decide(Long bookingId, String body) throws Exception {
        mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                        .header("Authorization", bearer(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private AppUser admin() {
        return users.findByEmailAndActiveTrue("admin@example.test").orElseThrow();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.createToken(user);
    }

    private void cleanup(TestData data) {
        notifications.deleteAllByUserId(data.customer().getId());
        List<Booking> userBookings = bookings.findAllByUserIdOrderByCreatedAtDesc(data.customer().getId());
        bookings.deleteAll(userBookings);
        if (profiles.existsById(data.profile().getId())) profiles.deleteById(data.profile().getId());
        if (users.existsById(data.customer().getId())) users.deleteById(data.customer().getId());
    }

    private record TestData(AppUser customer, Profile profile) {
    }
}
