package pt.saltosnaspalhacadas.backend.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private PasswordEncoder passwords;

    @BeforeEach
    void ensureAdmin() {
        if (users.findByEmailAndActiveTrue("admin@example.test").isEmpty()) {
            users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN));
        }
    }

    @Test
    void customerCanCreateAProposalAndOnlyTheyAndAdminsCanSeeItsPrivateDetails() throws Exception {
        TestData data = createTestData();
        LocalDate eventDate = LocalDate.now().plusDays(20);
        String contactName = "Cliente " + data.suffix();

        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), eventDate, contactName, "915 123 456")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.profileSlug").value(data.profile().getSlug()))
                    .andExpect(jsonPath("$.eventType").value("WEDDING"))
                    .andExpect(jsonPath("$.budget").value(450.00))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            mockMvc.perform(get("/api/v1/bookings/mine").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].contactName").value(contactName))
                    .andExpect(jsonPath("$[0].contactPhone").value("915 123 456"));

            mockMvc.perform(get("/api/v1/bookings/mine").header("Authorization", bearer(data.otherCustomer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content(bookingBody(data.profile().getSlug(), eventDate, contactName, "915 123 456")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Já tens uma proposta ativa para este artista nesta data."));

            mockMvc.perform(get("/api/v1/admin/bookings").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", eventDate.minusDays(1).toString())
                            .param("to", eventDate.plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates.length()").value(0));

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
    void acceptingOneProposalBlocksConflictsAndExposesOnlyTheBookedDatePublicly() throws Exception {
        TestData data = createTestData();
        LocalDate eventDate = LocalDate.now().plusDays(30);

        try {
            createBooking(data.customer(), data.profile().getSlug(), eventDate, "Primeiro cliente " + data.suffix());
            createBooking(data.otherCustomer(), data.profile().getSlug(), eventDate, "Segundo cliente " + data.suffix());

            Long firstBookingId = bookingIdFor(data.customer());
            Long secondBookingId = bookingIdFor(data.otherCustomer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", firstBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\",\"message\":\"Disponibilidade confirmada.\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"))
                    .andExpect(jsonPath("$.message").value("Disponibilidade confirmada."));

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", secondBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("O artista já tem um evento aceite nesta data. Escolhe outro dia."));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", eventDate.minusDays(1).toString())
                            .param("to", eventDate.plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates[0]").value(eventDate.toString()))
                    .andExpect(jsonPath("$.bookedDates.length()").value(1));

            mockMvc.perform(get("/api/v1/bookings/mine").header("Authorization", bearer(data.customer())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
        } finally {
            cleanup(data);
        }
    }

    @Test
    void counterProposalRequiresAValidFreeFutureDateOrBudget() throws Exception {
        TestData data = createTestData();
        LocalDate bookedDate = LocalDate.now().plusDays(35);
        LocalDate requestedDate = LocalDate.now().plusDays(40);
        LocalDate validCounterDate = LocalDate.now().plusDays(45);

        try {
            createBooking(data.customer(), data.profile().getSlug(), bookedDate, "Evento aceite " + data.suffix());
            Long acceptedBookingId = bookingIdFor(data.customer());
            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", acceptedBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isOk());

            createBooking(data.otherCustomer(), data.profile().getSlug(), requestedDate, "Evento com contraproposta " + data.suffix());
            Long counterBookingId = bookingIdFor(data.otherCustomer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", counterBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"COUNTER_PROPOSED\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica um novo orçamento, uma nova data, ou ambos na contraproposta"));

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", counterBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"COUNTER_PROPOSED\",\"counterEventDate\":\"" + bookedDate + "\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("O artista já tem um evento aceite nesta data. Escolhe outro dia."));

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", counterBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"COUNTER_PROPOSED\",\"message\":\"Podemos nesta nova data.\",\"counterBudget\":525.00,\"counterEventDate\":\"" + validCounterDate + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COUNTER_PROPOSED"))
                    .andExpect(jsonPath("$.counterProposal.budget").value(525.00))
                    .andExpect(jsonPath("$.counterProposal.eventDate").value(validCounterDate.toString()))
                    .andExpect(jsonPath("$.message").value("Podemos nesta nova data."));

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", counterBookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Esta proposta já não está pendente de decisão da administração"));

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", counterBookingId)
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content("{\"decision\":\"ACCEPTED\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Não tens permissão para responder a esta proposta"));

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", counterBookingId)
                            .header("Authorization", bearer(data.otherCustomer()))
                            .contentType("application/json")
                            .content("{\"decision\":\"ACCEPTED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"))
                    .andExpect(jsonPath("$.eventDate").value(validCounterDate.toString()))
                    .andExpect(jsonPath("$.budget").value(525.00));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", validCounterDate.toString())
                            .param("to", validCounterDate.plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates.length()").value(1))
                    .andExpect(jsonPath("$.bookedDates[0]").value(validCounterDate.toString()));

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", counterBookingId)
                            .header("Authorization", bearer(data.otherCustomer()))
                            .contentType("application/json")
                            .content("{\"decision\":\"DECLINED\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Esta proposta não tem uma contraproposta pendente"));
        } finally {
            cleanup(data);
        }
    }

    @Test
    void ownerCanDeclineAnActiveCounterProposal() throws Exception {
        TestData data = createTestData();
        LocalDate requestedDate = LocalDate.now().plusDays(50);

        try {
            createBooking(data.customer(), data.profile().getSlug(), requestedDate, "Cliente que recusa " + data.suffix());
            Long bookingId = bookingIdFor(data.customer());

            mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("{\"status\":\"COUNTER_PROPOSED\",\"counterBudget\":600.00}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COUNTER_PROPOSED"));

            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", bookingId)
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content("{\"decision\":\"DECLINED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DECLINED"));

            mockMvc.perform(get("/api/v1/profiles/{slug}/availability", data.profile().getSlug())
                            .param("from", requestedDate.minusDays(1).toString())
                            .param("to", requestedDate.plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookedDates.length()").value(0));
        } finally {
            cleanup(data);
        }
    }

    @Test
    void proposalValidationRejectsPastDatesAndInvalidBudget() throws Exception {
        TestData data = createTestData();
        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(data.customer()))
                            .contentType("application/json")
                            .content("""
                                    {"profileSlug":"%s","eventDate":"%s","eventType":"BIRTHDAY","contactName":"Cliente","contactPhone":"......","budget":0,"description":"Festa"}
                                    """.formatted(data.profile().getSlug(), LocalDate.now().minusDays(1))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.eventDate").value("A data do evento não pode ser no passado"))
                    .andExpect(jsonPath("$.errors.budget").value("O orçamento tem de ser superior a zero"))
                    .andExpect(jsonPath("$.errors.contactPhone").value("Indica um contacto telefónico válido"));
        } finally {
            cleanup(data);
        }
    }

    private void createBooking(AppUser customer, String profileSlug, LocalDate eventDate, String contactName) throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", bearer(customer))
                        .contentType("application/json")
                        .content(bookingBody(profileSlug, eventDate, contactName, "919 123 456")))
                .andExpect(status().isCreated());
    }

    private Long bookingIdFor(AppUser customer) {
        return bookings.findAllByUserIdWithProfileOrderByCreatedAtDesc(customer.getId())
                .getFirst()
                .getId();
    }

    private String bookingBody(String profileSlug, LocalDate eventDate, String contactName, String contactPhone) {
        return """
                {"profileSlug":"%s","eventDate":"%s","eventType":"WEDDING","contactName":"%s","contactPhone":"%s","budget":450.00,"description":"Cerimónia e festa durante a tarde.","notes":"Montagem a partir das 15h."}
                """.formatted(profileSlug, eventDate, contactName, contactPhone);
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
