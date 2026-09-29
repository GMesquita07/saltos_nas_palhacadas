package pt.saltosnaspalhacadas.backend.profile.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingEventType;
import pt.saltosnaspalhacadas.backend.booking.BookingRepository;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItem;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItemRepository;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.profile.ProfileSocialLink;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PortfolioItemRepository portfolioItemRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwords;
    @Autowired private AppUserRepository users;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        portfolioItemRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    void returnsOnlyActiveProfiles() throws Exception {
        profileRepository.save(new Profile("joao-tomas", "João Tomás", "DJ & Animador", "Descrição", null));

        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(jsonPath("$[0].slug").value("joao-tomas"))
                .andExpect(jsonPath("$[0].name").value("João Tomás"));

        mockMvc.perform(get("/api/v1/profiles/joao-tomas"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(jsonPath("$.slug").value("joao-tomas"));
    }

    @Test
    void publicProfilesExposeCompletedAcceptedEventsUntilToday() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profileRepository.save(new Profile("eventos-" + suffix, "DJ Eventos", "DJ", "Descrição", null));
        Profile otherProfile = profileRepository.save(new Profile("outro-eventos-" + suffix, "DJ Outro", "DJ", "Descrição", null));
        AppUser customer = users.save(new AppUser(
                "cliente-eventos-" + suffix + "@example.test",
                passwords.encode("palavra123"),
                UserRole.CUSTOMER));
        LocalDate today = LocalDate.now();

        bookingRepository.save(booking(customer, profile, today.minusDays(10), BookingStatus.ACCEPTED));
        bookingRepository.save(booking(customer, profile, today, BookingStatus.ACCEPTED));
        bookingRepository.save(booking(customer, profile, today.plusDays(1), BookingStatus.ACCEPTED));
        bookingRepository.save(booking(customer, profile, today.minusDays(3), BookingStatus.PENDING));
        bookingRepository.save(booking(customer, profile, today.minusDays(4), BookingStatus.DECLINED));
        bookingRepository.save(booking(customer, profile, today.minusDays(5), BookingStatus.COUNTER_PROPOSED));
        bookingRepository.save(booking(customer, profile, today.minusDays(6), BookingStatus.CANCELLED));
        bookingRepository.save(booking(customer, otherProfile, today.minusDays(7), BookingStatus.ACCEPTED));

        mockMvc.perform(get("/api/v1/profiles/{slug}", profile.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedEventsCount").value(2));

        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == '" + profile.getSlug() + "')].completedEventsCount").value(2))
                .andExpect(jsonPath("$[?(@.slug == '" + otherProfile.getSlug() + "')].completedEventsCount").value(1));
    }

    @Test
    void returnsOnlyActiveProfileSocialLinksInDisplayOrder() throws Exception {
        Profile profile = new Profile("social-joao", "João Social", "DJ", "Descrição", null);
        ProfileSocialLink inactive = new ProfileSocialLink("FACEBOOK", "Facebook", "https://facebook.com/saltos", 0);
        inactive.deactivate();
        profile.replaceSocialLinks(List.of(
                inactive,
                new ProfileSocialLink("YOUTUBE", "YouTube", "https://youtube.com/@saltos", 2),
                new ProfileSocialLink("INSTAGRAM", "Instagram", "https://instagram.com/saltos", 1)));
        profileRepository.save(profile);

        mockMvc.perform(get("/api/v1/profiles/social-joao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socialLinks.length()").value(2))
                .andExpect(jsonPath("$.socialLinks[0].platform").value("INSTAGRAM"))
                .andExpect(jsonPath("$.socialLinks[0].url").value("https://instagram.com/saltos"))
                .andExpect(jsonPath("$.socialLinks[1].platform").value("YOUTUBE"));
    }

    @Test
    void filtersPublicPortfolioByMediaType() throws Exception {
        Profile profile = profileRepository.save(new Profile("joao-tomas", "João Tomás", "DJ & Animador", "Descrição", null));
        portfolioItemRepository.save(new PortfolioItem(profile, MediaType.VIDEO, "Vídeo público", "Lisboa", LocalDate.of(2026, 6, 15), "https://video.example/test", null, 0, true));
        portfolioItemRepository.save(new PortfolioItem(profile, MediaType.PHOTO, "Foto pública", "Porto", LocalDate.of(2026, 6, 10), "https://image.example/test", null, 1, true));
        portfolioItemRepository.save(new PortfolioItem(profile, MediaType.VIDEO, "Rascunho", "Braga", LocalDate.of(2026, 6, 1), "https://video.example/draft", null, 2, false));

        mockMvc.perform(get("/api/v1/profiles/joao-tomas/portfolio").queryParam("type", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Vídeo público"))
                .andExpect(jsonPath("$[0].type").value("VIDEO"));
    }

    @Test
    void returnsNotFoundForUnknownProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/desconhecido"))
                .andExpect(status().isNotFound());
    }

    private Booking booking(AppUser customer, Profile profile, LocalDate eventDate, BookingStatus status) {
        Booking booking = new Booking(
                customer,
                profile,
                eventDate,
                null,
                null,
                BookingEventType.BIRTHDAY,
                null,
                null,
                "Lisboa",
                "Cliente Eventos",
                customer.getEmail(),
                "915 123 456",
                "Evento de teste.",
                null);

        switch (status) {
            case ACCEPTED -> booking.accept(eventDate, null, null, null, "Confirmado.");
            case DECLINED -> booking.decline("Recusado.");
            case COUNTER_PROPOSED -> booking.counterPropose("Proposta enviada.", null, eventDate.plusDays(1));
            case CANCELLED -> booking.cancel("Cancelado.");
            case PENDING -> { }
        }

        return booking;
    }

    @Test
    void adminCanChooseHomepageProfileDisplayOrder() throws Exception {
        AppUser admin = users.findByEmailAndActiveTrue("admin@example.test")
                .orElseGet(() -> users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN)));
        String token = jwtService.createToken(admin);
        profileRepository.save(new Profile("perfil-a", "Artista A", "DJ", "Descrição A", null));
        profileRepository.save(new Profile("perfil-b", "Artista B", "DJ", "Descrição B", null));

        mockMvc.perform(put("/api/v1/admin/profiles/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileSlugs":["perfil-b","perfil-a"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("perfil-b"))
                .andExpect(jsonPath("$[1].slug").value("perfil-a"));

        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("perfil-b"))
                .andExpect(jsonPath("$[1].slug").value("perfil-a"));

        mockMvc.perform(get("/api/v1/admin/profiles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
