package pt.saltosnaspalhacadas.backend.profile.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingEventType;
import pt.saltosnaspalhacadas.backend.booking.BookingRepository;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileDeletionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProfileRepository profiles;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private ReviewRepository reviews;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwords;

    @BeforeEach
    void ensureAdmin() {
        if (users.findByEmailAndActiveTrue("admin@example.test").isEmpty()) {
            users.save(new AppUser(
                    "admin@example.test",
                    passwords.encode("change-me-now"),
                    UserRole.ADMIN));
        }
    }

    @Test
    void pendingBookingBlocksProfileDeactivation() throws Exception {
        assertBookingStatusBlocksDeactivation(BookingStatus.PENDING);
    }

    @Test
    void acceptedBookingBlocksProfileDeactivation() throws Exception {
        assertBookingStatusBlocksDeactivation(BookingStatus.ACCEPTED);
    }

    @Test
    void counterProposedBookingBlocksProfileDeactivation() throws Exception {
        assertBookingStatusBlocksDeactivation(BookingStatus.COUNTER_PROPOSED);
    }

    @Test
    void cancelledBookingAllowsDeactivationAndPreservesHistoryAndReview() throws Exception {
        Fixture fixture = createFixture();

        try {
            Booking booking = createBooking(fixture, BookingStatus.CANCELLED);

            Review review = reviews.save(new Review(
                    fixture.profile(),
                    fixture.customer(),
                    "Cliente teste",
                    "Evento teste",
                    "Review que deve permanecer depois de desativar o perfil.",
                    5,
                    LocalDate.now(),
                    0,
                    true));

            mockMvc.perform(delete("/api/v1/admin/profiles/{slug}", fixture.profile().getSlug())
                            .header("Authorization", bearer(admin())))
                    .andExpect(status().isNoContent());

            Profile storedProfile = profiles.findById(fixture.profile().getId()).orElseThrow();

            assertThat(storedProfile.isActive()).isFalse();

            assertThat(bookings.findById(booking.getId()))
                    .isPresent()
                    .get()
                    .extracting(Booking::getStatus)
                    .isEqualTo(BookingStatus.CANCELLED);

            assertThat(reviews.findById(review.getId())).isPresent();

            mockMvc.perform(get("/api/v1/profiles/{slug}", fixture.profile().getSlug()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/v1/profiles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(
                            "$[?(@.slug == '" + fixture.profile().getSlug() + "')]"
                    ).isEmpty());
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void declinedBookingAllowsProfileDeactivationAndPreservesBooking() throws Exception {
        Fixture fixture = createFixture();

        try {
            Booking booking = createBooking(fixture, BookingStatus.DECLINED);

            mockMvc.perform(delete("/api/v1/admin/profiles/{slug}", fixture.profile().getSlug())
                            .header("Authorization", bearer(admin())))
                    .andExpect(status().isNoContent());

            assertThat(profiles.findById(fixture.profile().getId()))
                    .isPresent()
                    .get()
                    .extracting(Profile::isActive)
                    .isEqualTo(false);

            assertThat(bookings.findById(booking.getId()))
                    .isPresent()
                    .get()
                    .extracting(Booking::getStatus)
                    .isEqualTo(BookingStatus.DECLINED);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void creatingProfileWithInactiveSlugReturnsConflict() throws Exception {
        Fixture fixture = createFixture();

        try {
            Profile profile = fixture.profile();
            profile.deactivate();
            profiles.saveAndFlush(profile);

            mockMvc.perform(post("/api/v1/admin/profiles")
                            .header("Authorization", bearer(admin()))
                            .contentType("application/json")
                            .content("""
                                    {
                                      "slug": "%s",
                                      "name": "Perfil duplicado",
                                      "role": "DJ",
                                      "description": "Não deve ser criado."
                                    }
                                    """.formatted(profile.getSlug())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail")
                            .value("Já existe um perfil com este slug"));

            long matchingProfiles = profiles.findAll()
                    .stream()
                    .filter(existing -> existing.getSlug().equals(profile.getSlug()))
                    .count();

            assertThat(matchingProfiles).isEqualTo(1);
        } finally {
            cleanup(fixture);
        }
    }

    private void assertBookingStatusBlocksDeactivation(BookingStatus status) throws Exception {
        Fixture fixture = createFixture();

        try {
            createBooking(fixture, status);

            mockMvc.perform(delete("/api/v1/admin/profiles/{slug}", fixture.profile().getSlug())
                            .header("Authorization", bearer(admin())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(
                            "Não é possível eliminar este perfil porque tem agendamentos ativos. Resolve ou cancela os agendamentos primeiro."));

            assertThat(profiles.findById(fixture.profile().getId()))
                    .isPresent()
                    .get()
                    .extracting(Profile::isActive)
                    .isEqualTo(true);
        } finally {
            cleanup(fixture);
        }
    }

    private Booking createBooking(Fixture fixture, BookingStatus status) {
        LocalDate eventDate = LocalDate.now().plusDays(30);

        Booking booking = new Booking(
                fixture.customer(),
                fixture.profile(),
                eventDate,
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                BookingEventType.BIRTHDAY,
                null,
                null,
                "Viseu",
                "Cliente teste",
                fixture.customer().getEmail(),
                "910000000",
                "Pedido de teste",
                null);

        switch (status) {
            case PENDING -> {
                // Estado default.
            }
            case ACCEPTED -> booking.accept(
                    eventDate,
                    LocalTime.of(18, 0),
                    LocalTime.of(20, 0),
                    null,
                    "Aceite para teste.");
            case COUNTER_PROPOSED -> booking.counterPropose(
                    "Contraproposta de teste.",
                    BigDecimal.valueOf(250),
                    eventDate.plusDays(1));
            case CANCELLED -> booking.cancel("Cancelado para teste.");
            case DECLINED -> booking.decline("Recusado para teste.");
        }

        return bookings.save(booking);
    }

    private Fixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Profile profile = profiles.save(new Profile(
                "perfil-delete-" + suffix,
                "Perfil Delete",
                "DJ",
                "Perfil criado para testar desativação.",
                null));

        AppUser customer = users.save(new AppUser(
                "cliente-delete-" + suffix + "@example.test",
                passwords.encode("palavra123"),
                UserRole.CUSTOMER));

        return new Fixture(profile, customer);
    }

    private void cleanup(Fixture fixture) {
        reviews.findAllByUserId(fixture.customer().getId())
                .forEach(reviews::delete);

        bookings.findAllByUserIdOrderByCreatedAtDesc(fixture.customer().getId())
                .forEach(bookings::delete);

        profiles.findById(fixture.profile().getId())
                .ifPresent(profiles::delete);

        users.findById(fixture.customer().getId())
                .ifPresent(users::delete);
    }

    private AppUser admin() {
        return users.findByEmailAndActiveTrue("admin@example.test").orElseThrow();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.createToken(user);
    }

    private record Fixture(Profile profile, AppUser customer) {
    }
}
