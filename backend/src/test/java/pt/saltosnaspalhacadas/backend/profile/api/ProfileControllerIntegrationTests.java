package pt.saltosnaspalhacadas.backend.profile.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItem;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItemRepository;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PortfolioItemRepository portfolioItemRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwords;
    @Autowired private AppUserRepository users;

    @BeforeEach
    void setUp() {
        portfolioItemRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    void returnsOnlyActiveProfiles() throws Exception {
        profileRepository.save(new Profile("joao-tomas", "João Tomás", "DJ & Animador", "Descrição", null));

        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("joao-tomas"))
                .andExpect(jsonPath("$[0].name").value("João Tomás"));
    }

    @Test
    void filtersPublicPortfolioByMediaType() throws Exception {
        Profile profile = profileRepository.save(new Profile("joao-tomas", "João Tomás", "DJ & Animador", "Descrição", null));
        portfolioItemRepository.save(new PortfolioItem(profile, MediaType.VIDEO, "Vídeo público", "Lisboa", LocalDate.of(2026, 6, 15), "https://video.example/test", null, 0, true));
        portfolioItemRepository.save(new PortfolioItem(profile, MediaType.PHOTO, "Foto pública", "Porto", LocalDate.of(2026, 6, 10), "https://image.example/test", null, 1, true));
        portfolioItemRepository.save(new PortfolioItem(profile, MediaType.VIDEO, "Rascunho", "Braga", LocalDate.of(2026, 6, 1), "https://video.example/draft", null, 2, false));

        mockMvc.perform(get("/api/v1/profiles/joao-tomas/portfolio").queryParam("type", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Vídeo público"))
                .andExpect(jsonPath("$[0].type").value("VIDEO"));
    }

    @Test
    void returnsNotFoundForUnknownProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/desconhecido"))
                .andExpect(status().isNotFound());
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
    }
}
