package pt.saltosnaspalhacadas.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.booking.BookingRepository;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationFeatureIntegrationTests {

    private static final String ADMIN_EMAIL = "admin@example.test";

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
    private ReviewRepository reviews;

    @Autowired
    private PasswordEncoder passwords;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void prepareEmailMockAndAdmin() {
        if (users.findByEmailAndActiveTrue(ADMIN_EMAIL).isEmpty()) {
            users.save(new AppUser(ADMIN_EMAIL, passwords.encode("change-me-now"), UserRole.ADMIN));
        }
        reset(emailService);
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void adminCanManagePrivateProfileNotificationEmailAndPublicProfilesDoNotExposeIt() throws Exception {
        String suffix = suffix();
        String slug = "notificacoes-" + suffix;
        AppUser customer = users.save(new AppUser("privacy-customer-" + suffix + "@example.test", passwords.encode("password-segura"), UserRole.CUSTOMER));

        try {
            mockMvc.perform(post("/api/v1/admin/profiles")
                            .header("Authorization", bearer(admin()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"slug":"%s","name":"DJ Privado","role":"DJ","description":"Perfil privado para notificações","notificationEmail":"Artista.Notifica@Example.TEST"}
                                    """.formatted(slug)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.slug").value(slug))
                    .andExpect(jsonPath("$.notificationEmail").value("artista.notifica@example.test"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", bearer(admin()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"DJ Privado","role":"DJ","description":"Perfil privado para notificações","profileImageUrl":null,"profileImagePosition":"50% 50%","profileImageZoom":1,"featuredVideoUrl":null,"notificationEmail":"Nova.Notifica@Example.TEST"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notificationEmail").value("nova.notifica@example.test"));

            mockMvc.perform(get("/api/v1/admin/profiles")
                            .header("Authorization", bearer(admin())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.slug == '" + slug + "')].notificationEmail").value("nova.notifica@example.test"));

            mockMvc.perform(get("/api/v1/admin/profiles")
                            .header("Authorization", bearer(customer)))
                    .andExpect(status().isForbidden());

            String publicList = mockMvc.perform(get("/api/v1/profiles"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertThat(publicList).doesNotContain("notificationEmail", "notification_email", "nova.notifica@example.test");

            String publicProfile = mockMvc.perform(get("/api/v1/profiles/{slug}", slug))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertThat(publicProfile).doesNotContain("notificationEmail", "notification_email", "nova.notifica@example.test");
        } finally {
            profiles.findBySlugAndActiveTrue(slug).ifPresent(profiles::delete);
            users.findById(customer.getId()).ifPresent(users::delete);
        }
    }

    @Test
    void newBookingNotifiesCustomerConfiguredArtistAndOnlyActiveAdminsWithoutDuplicateRecipient() throws Exception {
        String suffix = suffix();
        AppUser adminTwo = users.save(new AppUser("booking-admin-" + suffix + "@example.test", passwords.encode("change-me-now"), UserRole.ADMIN));
        AppUser inactiveAdmin = users.save(new AppUser("inactive-admin-" + suffix + "@example.test", passwords.encode("change-me-now"), UserRole.ADMIN));
        inactiveAdmin.anonymizeForDeletion(inactiveAdmin.getEmail(), passwords.encode("disabled"));
        inactiveAdmin = users.save(inactiveAdmin);
        AppUser customerRoleUser = users.save(new AppUser("not-admin-" + suffix + "@example.test", passwords.encode("change-me-now"), UserRole.CUSTOMER));
        AppUser customer = users.save(new AppUser("booking-customer-" + suffix + "@example.test", passwords.encode("password-segura"), UserRole.CUSTOMER));
        Profile profile = profiles.save(new Profile("booking-notify-" + suffix, "DJ Notificações", "DJ", "Perfil de teste para notificações", null, "50% 50%", 1.0, null, 0, ADMIN_EMAIL));

        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingBody(profile.getSlug(), LocalDate.now().plusDays(20), "Cliente Booking " + suffix, "10:00", "12:00")))
                    .andExpect(status().isCreated());

            verify(emailService).send(
                    eq("cliente-" + profile.getSlug() + "@example.test"),
                    eq("Pedido de agendamento recebido"),
                    argThat(body -> body.contains("A equipa irá analisar o teu pedido") && !body.contains("O animador vai analisar")));
            verify(emailService).send(eq(ADMIN_EMAIL), contains("novo pedido de agendamento"), contains("Pedido: #"));
            verify(emailService).send(eq(adminTwo.getEmail()), contains("novo pedido de agendamento"), contains("Artista: DJ Notificações"));
            verify(emailService, never()).send(eq(inactiveAdmin.getEmail()), anyString(), anyString());
            verify(emailService, never()).send(eq(customerRoleUser.getEmail()), anyString(), anyString());
        } finally {
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
            users.deleteById(customerRoleUser.getId());
            users.deleteById(inactiveAdmin.getId());
            users.deleteById(adminTwo.getId());
        }
    }

    @Test
    void bookingEmailFailuresAndMissingArtistEmailDoNotBlockBooking() throws Exception {
        String suffix = suffix();
        AppUser customer = users.save(new AppUser("booking-failure-" + suffix + "@example.test", passwords.encode("password-segura"), UserRole.CUSTOMER));
        Profile profile = profiles.save(new Profile("booking-no-artist-" + suffix, "DJ Sem Email", "DJ", "Perfil sem email de artista", null));
        reset(emailService);
        when(emailService.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("smtp down"));

        try {
            mockMvc.perform(post("/api/v1/bookings")
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingBody(profile.getSlug(), LocalDate.now().plusDays(21), "Cliente Falha " + suffix, null, null)))
                    .andExpect(status().isCreated());
        } finally {
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void bookingDecisionsCancellationCounterProposalsAndCounterResponsesNotifyStakeholders() throws Exception {
        String suffix = suffix();
        AppUser customer = users.save(new AppUser("booking-events-" + suffix + "@example.test", passwords.encode("password-segura"), UserRole.CUSTOMER));
        Profile profile = profiles.save(new Profile("booking-events-" + suffix, "DJ Eventos", "DJ", "Perfil para eventos de email", null, "50% 50%", 1.0, null, 0, "artist-" + suffix + "@example.test"));

        try {
            Long acceptedId = createBooking(customer, profile, LocalDate.now().plusDays(22), "Cliente Aceite " + suffix);
            resetEmailMock();
            decide(acceptedId, "{\"status\":\"ACCEPTED\",\"eventDate\":\"" + LocalDate.now().plusDays(22) + "\",\"startTime\":\"10:00\",\"endTime\":\"12:00\",\"message\":\"Confirmado.\"}");
            verify(emailService).send(eq("cliente-" + profile.getSlug() + "@example.test"), eq("Pedido de agendamento aceite"), contains("foi aceite"));
            verify(emailService).send(eq(profile.getNotificationEmail()), contains("pedido aceite"), contains("O pedido foi aceite"));
            verify(emailService).send(eq(ADMIN_EMAIL), contains("pedido aceite"), contains("Estado: ACCEPTED"));

            Long declinedId = createBooking(customer, profile, LocalDate.now().plusDays(23), "Cliente Recusado " + suffix);
            resetEmailMock();
            decide(declinedId, "{\"status\":\"DECLINED\",\"message\":\"Sem disponibilidade.\"}");
            verify(emailService).send(eq(profile.getNotificationEmail()), contains("pedido não aceite"), contains("O pedido foi marcado como não aceite"));

            Long counterId = createBooking(customer, profile, LocalDate.now().plusDays(24), "Cliente Contraproposta " + suffix);
            resetEmailMock();
            decide(counterId, "{\"status\":\"COUNTER_PROPOSED\",\"message\":\"Pode ser noutra data.\",\"counterEventDate\":\"" + LocalDate.now().plusDays(25) + "\"}");
            verify(emailService).send(eq(profile.getNotificationEmail()), contains("alteração proposta"), contains("propôs uma alteração"));

            resetEmailMock();
            mockMvc.perform(put("/api/v1/bookings/{id}/counter-proposal/decision", counterId)
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"DECLINED\"}"))
                    .andExpect(status().isOk());
            verify(emailService).send(eq("cliente-" + profile.getSlug() + "@example.test"), eq("Contraproposta recusada"), contains("pedido foi encerrado"));
            verify(emailService).send(eq(profile.getNotificationEmail()), contains("contraproposta recusada"), contains("O cliente recusou a contraproposta"));

            Long cancelledId = createBooking(customer, profile, LocalDate.now().plusDays(26), "Cliente Cancela " + suffix);
            resetEmailMock();
            mockMvc.perform(put("/api/v1/bookings/{id}/cancel", cancelledId)
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Já não preciso.\"}"))
                    .andExpect(status().isOk());
            verify(emailService).send(eq(profile.getNotificationEmail()), contains("agendamento cancelado"), contains("O agendamento foi cancelado"));
        } finally {
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void registrationNotifiesAdminsWithoutPasswordAndNotificationFailureDoesNotBlockRegistration() throws Exception {
        String suffix = suffix();
        String email = "registration-" + suffix + "@example.test";
        String failureEmail = "registration-failure-" + suffix + "@example.test";

        try {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","username":"reg%s","firstName":"Cliente","lastName":"Novo","phone":"+351 912 345 678","password":"password-segura"}
                                    """.formatted(email, suffix)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService, atLeastOnce()).send(eq(ADMIN_EMAIL), contains("novo registo de cliente"), bodyCaptor.capture());
            assertThat(bodyCaptor.getAllValues()).allSatisfy(body -> assertThat(body).doesNotContain("password-segura", "password"));

            reset(emailService);
            when(emailService.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("smtp down"));
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","username":"regfail%s","firstName":"Cliente","lastName":"Falha","phone":"+351 912 345 679","password":"password-segura"}
                                    """.formatted(failureEmail, suffix)))
                    .andExpect(status().isCreated());

            assertThat(users.findByEmailAndActiveTrue(failureEmail)).isPresent();
        } finally {
            users.findByEmailAndActiveTrue(email).ifPresent(users::delete);
            users.findByEmailAndActiveTrue(failureEmail).ifPresent(users::delete);
        }
    }

    @Test
    void reviewSubmissionNotifiesAdminsAndNotificationFailureDoesNotBlockReview() throws Exception {
        String suffix = suffix();
        AppUser customer = users.save(new AppUser("review-customer-" + suffix + "@example.test", passwords.encode("password-segura"), UserRole.CUSTOMER));
        Profile profile = profiles.save(new Profile("review-notify-" + suffix, "DJ Avaliado", "DJ", "Perfil para reviews", null));

        try {
            mockMvc.perform(post("/api/v1/profiles/{slug}/reviews", profile.getSlug())
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reviewerName":"Cliente Review","title":"Grande festa","comment":"A animação foi excelente e todos adoraram.","rating":5}
                                    """))
                    .andExpect(status().isCreated());
            verify(emailService).send(eq(ADMIN_EMAIL), contains("nova avaliação pendente"), contains("Grande festa"));

            reset(emailService);
            when(emailService.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("smtp down"));
            mockMvc.perform(post("/api/v1/profiles/{slug}/reviews", profile.getSlug())
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reviewerName":"Cliente Review","title":"Outra avaliação","comment":"Mesmo com email indisponível a review deve entrar.","rating":4}
                                    """))
                    .andExpect(status().isCreated());
        } finally {
            reviews.deleteAll(reviews.findAllByUserId(customer.getId()));
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    private Long createBooking(AppUser customer, Profile profile, LocalDate eventDate, String contactName) throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(profile.getSlug(), eventDate, contactName, "10:00", "12:00")))
                .andExpect(status().isCreated());
        return bookings.findAllByUserIdWithProfileOrderByCreatedAtDesc(customer.getId()).getFirst().getId();
    }

    private void decide(Long bookingId, String body) throws Exception {
        mockMvc.perform(put("/api/v1/admin/bookings/{id}/decision", bookingId)
                        .header("Authorization", bearer(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private String bookingBody(String profileSlug, LocalDate eventDate, String contactName, String startTime, String endTime) {
        return """
                {"profileSlug":"%s","eventDate":"%s","startTime":%s,"endTime":%s,"eventType":"BIRTHDAY","location":"Quinta de Viseu","contactName":"%s","contactEmail":"cliente-%s@example.test","contactPhone":"915 123 456","description":"Festa de aniversário com animação.","notes":"Montagem a partir das 15h."}
                """.formatted(profileSlug, eventDate, jsonNullable(startTime), jsonNullable(endTime), contactName, profileSlug);
    }

    private void resetEmailMock() {
        reset(emailService);
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    private AppUser admin() {
        return users.findByEmailAndActiveTrue(ADMIN_EMAIL).orElseThrow();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.createToken(user);
    }

    private static String jsonNullable(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
