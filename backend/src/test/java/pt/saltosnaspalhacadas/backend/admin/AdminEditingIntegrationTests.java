package pt.saltosnaspalhacadas.backend.admin;

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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.contact.Contact;
import pt.saltosnaspalhacadas.backend.contact.ContactRepository;
import pt.saltosnaspalhacadas.backend.contact.ContactType;
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
class AdminEditingIntegrationTests {

    private static final String ADMIN_EMAIL = "admin@example.test";

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
    private PortfolioItemRepository items;

    @Autowired
    private ContactRepository contacts;

    @BeforeEach
    void ensureAdmin() {
        if (users.findByEmailAndActiveTrue(ADMIN_EMAIL).isEmpty()) {
            users.save(new AppUser(ADMIN_EMAIL, passwords.encode("change-me-now"), UserRole.ADMIN));
        }
    }

    @Test
    void adminCanUpdateProfileContentAndContactWithoutLosingExistingValues() throws Exception {
        String slug = "dj-edicao-" + UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile(slug, "Nome antigo", "DJ", "Descrição antiga", null));
        PortfolioItem item = items.save(new PortfolioItem(
                profile,
                pt.saltosnaspalhacadas.backend.portfolio.MediaType.PHOTO,
                "Fotografia antiga",
                "Lisboa",
                LocalDate.of(2026, 8, 1),
                "https://example.test/old.jpg",
                null,
                0,
                true));
        Contact contact = contacts.save(new Contact("Email antigo", ContactType.EMAIL, "old@example.test", 0));
        String token = adminToken();

        try {
            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Nome atualizado","role":"DJ e animador","description":"Descrição atualizada","profileImageUrl":null,"profileImagePosition":null}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value(slug))
                    .andExpect(jsonPath("$.name").value("Nome atualizado"))
                    .andExpect(jsonPath("$.profileImagePosition").value("50% 50%"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}/portfolio/{itemId}", slug, item.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"VIDEO","title":"Vídeo atualizado","location":"Porto","eventDate":"2026-08-20","mediaUrl":"https://example.test/new.mp4","thumbnailUrl":"","published":true}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(item.getId()))
                    .andExpect(jsonPath("$.type").value("VIDEO"))
                    .andExpect(jsonPath("$.title").value("Vídeo atualizado"))
                    .andExpect(jsonPath("$.thumbnailUrl").doesNotExist());

            mockMvc.perform(put("/api/v1/admin/contacts/{id}", contact.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"Website oficial","type":"WEBSITE","value":"saltosnaspalhacadas.pt"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.label").value("Website oficial"))
                    .andExpect(jsonPath("$.type").value("WEBSITE"))
                    .andExpect(jsonPath("$.value").value("saltosnaspalhacadas.pt"));
        } finally {
            items.deleteById(item.getId());
            contacts.deleteById(contact.getId());
            profiles.deleteById(profile.getId());
        }
    }

    @Test
    void returnsFieldErrorsForAnInvalidSlugAndUsefulErrorsForInvalidContactValues() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/api/v1/admin/profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"DJ João","name":"DJ João","role":"DJ","description":"Perfil válido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.slug").value("O slug só pode usar minúsculas, números e hífen entre palavras"));

        mockMvc.perform(post("/api/v1/admin/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Email","type":"EMAIL","value":"email-inválido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indica um endereço de email válido"));
    }

    private String adminToken() {
        AppUser admin = users.findByEmailAndActiveTrue(ADMIN_EMAIL).orElseThrow();
        return jwtService.createToken(admin);
    }
}
