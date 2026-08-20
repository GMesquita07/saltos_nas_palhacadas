package pt.saltosnaspalhacadas.backend.favorite;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
class FavoriteIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private ProfileRepository profiles;

    @Autowired
    private PortfolioItemRepository portfolioItems;

    @Autowired
    private FavoriteRepository favorites;

    @Test
    void registersCustomerAndExposesTheirAuthenticatedSession() throws Exception {
        String email = "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";

        try {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"palavra123\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));

            AppUser customer = users.findByEmailAndActiveTrue(email).orElseThrow();
            String token = jwtService.createToken(customer);

            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"palavra123\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Já existe uma conta com este email"));
        } finally {
            users.findByEmailAndActiveTrue(email).ifPresent(users::delete);
        }
    }

    @Test
    void customerCanManageOnlyPublishedPortfolioFavorites() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cliente-favoritos-" + suffix + "@example.test";
        Profile profile = profiles.save(new Profile("dj-favoritos-" + suffix, "DJ Favoritos", "DJ", "Perfil para testes", null));
        PortfolioItem publishedItem = portfolioItems.save(new PortfolioItem(
                profile,
                MediaType.PHOTO,
                "Fotografia favorita",
                "Lisboa",
                LocalDate.of(2026, 8, 20),
                "https://example.test/favorita.jpg",
                null,
                0,
                true));
        PortfolioItem draftItem = portfolioItems.save(new PortfolioItem(
                profile,
                MediaType.VIDEO,
                "Vídeo privado",
                "Porto",
                LocalDate.of(2026, 8, 19),
                "https://example.test/privado.mp4",
                null,
                0,
                false));
        AppUser customer = users.save(new AppUser(email, passwords.encode("palavra123"), UserRole.CUSTOMER));
        String token = jwtService.createToken(customer);

        try {
            mockMvc.perform(get("/api/v1/favorites"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/favorites/{itemId}", publishedItem.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.portfolioItemId").value(publishedItem.getId()))
                    .andExpect(jsonPath("$.profileSlug").value(profile.getSlug()))
                    .andExpect(jsonPath("$.item.title").value("Fotografia favorita"));

            mockMvc.perform(post("/api/v1/favorites/{itemId}", publishedItem.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.portfolioItemId").value(publishedItem.getId()));

            mockMvc.perform(get("/api/v1/favorites").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].item.type").value("PHOTO"))
                    .andExpect(jsonPath("$[0].item.title").value("Fotografia favorita"));

            mockMvc.perform(post("/api/v1/favorites/{itemId}", draftItem.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Publicação não encontrada"));

            mockMvc.perform(delete("/api/v1/favorites/{itemId}", publishedItem.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/favorites").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        } finally {
            favorites.findByUserIdAndPortfolioItemId(customer.getId(), publishedItem.getId()).ifPresent(favorites::delete);
            portfolioItems.deleteById(draftItem.getId());
            portfolioItems.deleteById(publishedItem.getId());
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }
}
