package pt.saltosnaspalhacadas.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ProfileRepository profiles;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private BookingReminderService reminders;

    @Autowired
    private PasswordEncoder passwords;

    @BeforeEach
    void ensureAdmin() {
        if (users.findByEmailAndActiveTrue("admin@example.test").isEmpty()) {
            users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN));
        }
    }

    @Test
    void customerCanCreateARequestAndPendingSlotsAppearAsStandby() throws Exception {
        TestData data = createTestData();
        LocalDate eventDate = LocalDate.now().plusDays(20);
        String contactName = "Cliente " + data.suffix();

        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), eventDate, "WEDDING", contactName, "915 123 456", "10:00", "12:00")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.profileSlug").value(data.profile().getSlug()))
                    .andExpect(jsonPath("$.eventType").value("WEDDING"))
                    .andExpect(jsonPath("$.weddingCoupleNames").value("Ana e Miguel"))
                    .andExpect(jsonPath("$.location").value("Quinta de Viseu"))
                    .andExpect(jsonPath("$.contactEmail").value("cliente-" + data.profile().getSlug() + "@example.test"))
                    .andExpect(jsonPath("$.startTime").value("10:00:00"))
                    .andExpect(jsonPath("$.endTime").value("12:00:00"))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            mockMvc.perform(get("/api/v1/bookings/mine").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].contactName").value(contactName))
                    .andExpect(jsonPath("$[0].contactPhone").value("915 123 456"))
                    .andExpect(jsonPath("$[0].location").value("Quinta de Viseu"));

            mockMvc.perform(get("/api/v1/bookings/mine").header("Authorization", bearer(data.otherCustomer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), eventDate, "WEDDING", contactName, "915 123 456", "11:00", "13:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Já tens um pedido ativo para este artista nesse horário."));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", eventDate.minusDays(1).toString())
                            .param("to", eventDate.plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates.length()").value(0))
                    .andExpect(jsonPath("$.slots.length()").value(1))
                    .andExpect(jsonPath("$.slots[0].date").value(eventDate.toString()))
                    .andExpect(jsonPath("$.slots[0].status").value("PENDING"));

            mockMvc.perform(get("/api/v1/admin/bookings?status=PENDING")
                            .header("Authorization", bearer(admin())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.contactName == '" + contactName + "')].description")
                            .value("Cerimónia e festa durante a tarde."));

            mockMvc.perform(delete("/api/v1/admin/profiles/{slug}", data.profile().getSlug())
                            .header("Authorization", bearer(admin())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Não é possível eliminar este perfil porque tem agendamentos associados. Resolve ou cancela os agendamentos primeiro."));
        } finally {
            cleanup(data);
        }
    }

    @Test
    void acceptedTimedBookingsCanShareADayWhenTheyDoNotOverlap() throws Exception {
        TestData data = createTestData();
        LocalDate eventDate = LocalDate.now().plusDays(30);

        try {
            createBooking(data.customer(), data.profile().getSlug(), eventDate, "Primeiro cliente " + data.suffix(), "10:00", "12:00");
            createBooking(data.otherCustomer(), data.profile().getSlug(), eventDate, "Segundo cliente " + data.suffix(), "18:00", "19:00");

            Long firstBookingId = bookingIdFor(data.customer());
            Long secondBookingId = bookingIdFor(data.otherCustomer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", firstBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\",\"eventDate\":\"" + eventDate + "\",\"startTime\":\"10:00\",\"endTime\":\"12:00\",\"message\":\"Disponibilidade confirmada.\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"))
                    .andExpect(jsonPath("$.message").value("Disponibilidade confirmada."));

            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.otherCustomer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), eventDate, "BIRTHDAY", "Pedido sobreposto " + data.suffix(), "916 123 456", "11:00", "13:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("O artista já tem um evento aceite nesse horário. Escolhe outro intervalo."));

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", secondBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\",\"eventDate\":\"" + eventDate + "\",\"startTime\":\"18:00\",\"endTime\":\"19:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", eventDate.minusDays(1).toString())
                            .param("to", eventDate.plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates.length()").value(0))
                    .andExpect(jsonPath("$.slots.length()").value(2));
        } finally {
            cleanup(data);
        }
    }

    @Test
    void wholeDayAcceptedBookingBlocksTheDateAndCancellationFreesIt() throws Exception {
        TestData data = createTestData();
        LocalDate eventDate = LocalDate.now().plusDays(35);

        try {
            createBooking(data.customer(), data.profile().getSlug(), eventDate, "Evento sem horas " + data.suffix(), null, null);
            Long bookingId = bookingIdFor(data.customer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\",\"eventDate\":\"" + eventDate + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", eventDate.toString())
                            .param("to", eventDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates[0]").value(eventDate.toString()))
                    .andExpect(jsonPath("$.slots[0].status").value("ACCEPTED"));

            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.otherCustomer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), eventDate, "BIRTHDAY", "Tentativa bloqueada " + data.suffix(), "916 123 456", "18:00", "19:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("O artista já tem um evento aceite nesse horário. Escolhe outro intervalo."));

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"CANCELLED\",\"message\":\"Cliente cancelou o evento.\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.message").value("Cliente cancelou o evento."));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", eventDate.toString())
                            .param("to", eventDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates.length()").value(0))
                    .andExpect(jsonPath("$.slots.length()").value(0));

            createBooking(data.otherCustomer(), data.profile().getSlug(), eventDate, "Agora livre " + data.suffix(), "18:00", "19:00");
        } finally {
            cleanup(data);
        }
    }

    @Test
    void adminCanConfirmWithAdjustedDateAndTime() throws Exception {
        TestData data = createTestData();
        LocalDate requestedDate = LocalDate.now().plusDays(40);
        LocalDate confirmedDate = requestedDate.plusDays(2);

        try {
            createBooking(data.customer(), data.profile().getSlug(), requestedDate, "Cliente ajustado " + data.suffix(), "10:00", "12:00");
            Long bookingId = bookingIdFor(data.customer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\",\"eventDate\":\"" + confirmedDate + "\",\"startTime\":\"18:00\",\"endTime\":\"19:00\",\"agreedBudget\":725.00,\"message\":\"Horário ajustado por telefone.\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"))
                    .andExpect(jsonPath("$.eventDate").value(confirmedDate.toString()))
                    .andExpect(jsonPath("$.startTime").value("18:00:00"))
                    .andExpect(jsonPath("$.endTime").value("19:00:00"))
                    .andExpect(jsonPath("$.budget").value(725.00))
                    .andExpect(jsonPath("$.message").value("Horário ajustado por telefone."));

            mockMvc.perform(get("/api/v1/bookings/mine").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].budget").value(nullValue()));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", confirmedDate.toString())
                            .param("to", confirmedDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slots[0].date").value(confirmedDate.toString()))
                    .andExpect(jsonPath("$.slots[0].status").value("ACCEPTED"));
        } finally {
            cleanup(data);
        }
    }

    @Test
    void acceptedBookingsReceiveOneReminderFiveDaysBeforeTheEvent() throws Exception {
        TestData data = createTestData();
        LocalDate today = LocalDate.now();
        LocalDate eventDate = today.plusDays(5);

        try {
            createBooking(data.customer(), data.profile().getSlug(), eventDate, "Cliente lembrete " + data.suffix(), "10:00", "12:00");
            Long bookingId = bookingIdFor(data.customer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\",\"eventDate\":\"" + eventDate + "\",\"startTime\":\"10:00\",\"endTime\":\"12:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));

            assertThat(reminders.sendDueReminders(today)).isEqualTo(1);
            assertThat(bookings.findById(bookingId).orElseThrow().getReminderSentAt()).isNotNull();
            assertThat(reminders.sendDueReminders(today)).isZero();
        } finally {
            cleanup(data);
        }
    }

    @Test
    void requestValidationRejectsInvalidDatesPhonesTimesAndConditionalFields() throws Exception {
        TestData data = createTestData();

        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content("""
                                    {"profileSlug":"%s","eventDate":"%s","eventType":"BIRTHDAY","location":"","contactName":"Cliente","contactEmail":"email-invalido","contactPhone":"......","description":"Festa"}
                                    """.formatted(data.profile().getSlug(), LocalDate.now().minusDays(1))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.eventDate").value("A data do evento não pode ser no passado"))
                    .andExpect(jsonPath("$.errors.location").value("Indica o local do evento"))
                    .andExpect(jsonPath("$.errors.contactEmail").value("Indica um email válido"))
                    .andExpect(jsonPath("$.errors.contactPhone").value("Indica um contacto telefónico válido"));

            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), LocalDate.now().plusDays(10), "WEDDING", "Sem noivos", "915 123 456", "13:00", "12:00").replace("\"weddingCoupleNames\":\"Ana e Miguel\",", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("A hora de fim tem de ser posterior à hora de início"));

            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), LocalDate.now().plusDays(12), "OTHER", "Outro evento", "915 123 456", null, null).replace("\"customEventType\":\"Evento privado\",", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica o tipo de evento"));
        } finally {
            cleanup(data);
        }
    }

    private void createBooking(AppUser customer, String profileSlug, LocalDate eventDate, String contactName, String startTime, String endTime) throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", bearer(customer))
                        .contentType("application/json")
                        .content(bookingBody(profileSlug, eventDate, "WEDDING", contactName, "919 123 456", startTime, endTime)))
                .andExpect(status().isCreated());
    }

    private Long bookingIdFor(AppUser customer) {
        return bookings.findAllByUserIdWithProfileOrderByCreatedAtDesc(customer.getId())
                .getFirst()
                .getId();
    }

    private String bookingBody(
            String profileSlug,
            LocalDate eventDate,
            String eventType,
            String contactName,
            String contactPhone,
            String startTime,
            String endTime) {
        return """
                {"profileSlug":"%s","eventDate":"%s","startTime":%s,"endTime":%s,"eventType":"%s",%s%s"location":"Quinta de Viseu","contactName":"%s","contactEmail":"cliente-%s@example.test","contactPhone":"%s","description":"Cerimónia e festa durante a tarde.","notes":"Montagem a partir das 15h."}
                """.formatted(
                profileSlug,
                eventDate,
                jsonNullable(startTime),
                jsonNullable(endTime),
                eventType,
                "WEDDING".equals(eventType) ? "\"weddingCoupleNames\":\"Ana e Miguel\"," : "",
                "OTHER".equals(eventType) ? "\"customEventType\":\"Evento privado\"," : "",
                contactName,
                profileSlug,
                contactPhone);
    }

    private static String jsonNullable(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private AppUser admin() {
        return users.findByEmailAndActiveTrue("admin@example.test").orElseThrow();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.createToken(user);
    }

    private TestData createTestData() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile(
                "dj-agendamento-" + suffix,
                "DJ Agendamento",
                "DJ",
                "Perfil de teste para agendamentos",
                null));
        AppUser customer = users.save(new AppUser(
                "cliente-agendamento-" + suffix + "@example.test",
                passwords.encode("palavra123"),
                UserRole.CUSTOMER));
        AppUser otherCustomer = users.save(new AppUser(
                "outro-agendamento-" + suffix + "@example.test",
                passwords.encode("palavra123"),
                UserRole.CUSTOMER));
        return new TestData(suffix, profile, customer, otherCustomer);
    }

    private void cleanup(TestData data) {
        if (profiles.existsById(data.profile().getId())) {
            profiles.deleteById(data.profile().getId());
        }
        users.findById(data.customer().getId()).ifPresent(users::delete);
        users.findById(data.otherCustomer().getId()).ifPresent(users::delete);
    }

    private record TestData(String suffix, Profile profile, AppUser customer, AppUser otherCustomer) {
    }
}
