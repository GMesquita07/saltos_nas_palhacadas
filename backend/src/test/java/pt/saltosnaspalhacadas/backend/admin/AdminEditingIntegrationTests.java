package pt.saltosnaspalhacadas.backend.admin;

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
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;
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

    @Autowired
    private ReviewRepository reviews;

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
                                    {"name":"Nome atualizado","role":"DJ e animador","description":"Descrição atualizada","profileImageUrl":null,"profileImagePosition":"50% 24%","profileImageZoom":1.35,"featuredVideoUrl":"https://example.test/destaque.mp4"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value(slug))
                    .andExpect(jsonPath("$.name").value("Nome atualizado"))
                    .andExpect(jsonPath("$.profileImagePosition").value("50% 24%"))
                    .andExpect(jsonPath("$.profileImageZoom").value(1.35))
                    .andExpect(jsonPath("$.featuredVideoUrl").value("https://example.test/destaque.mp4"));

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
    void adminPortfolioListsHiddenItemsAndPublicPortfolioStaysChronological() throws Exception {
        String slug = "portfolio-admin-" + UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile(slug, "Portfolio Admin", "DJ", "Descrição do portfolio", null));
        PortfolioItem oldest = items.save(new PortfolioItem(
                profile,
                pt.saltosnaspalhacadas.backend.portfolio.MediaType.PHOTO,
                "Evento antigo",
                "Lisboa",
                LocalDate.of(2026, 4, 1),
                "https://example.test/old.jpg",
                null,
                0,
                true));
        PortfolioItem sameDayFirst = items.save(new PortfolioItem(
                profile,
                pt.saltosnaspalhacadas.backend.portfolio.MediaType.PHOTO,
                "Primeiro do mesmo dia",
                "Porto",
                LocalDate.of(2026, 5, 10),
                "https://example.test/first.jpg",
                null,
                1,
                true));
        PortfolioItem sameDaySecond = items.save(new PortfolioItem(
                profile,
                pt.saltosnaspalhacadas.backend.portfolio.MediaType.VIDEO,
                "Segundo do mesmo dia",
                "Braga",
                LocalDate.of(2026, 5, 10),
                "https://example.test/second.mp4",
                "https://example.test/second.jpg",
                2,
                true));
        PortfolioItem hidden = items.save(new PortfolioItem(
                profile,
                pt.saltosnaspalhacadas.backend.portfolio.MediaType.PHOTO,
                "Conteúdo oculto",
                "Coimbra",
                LocalDate.of(2026, 6, 1),
                "https://example.test/hidden.jpg",
                null,
                3,
                false));
        AppUser customer = users.save(new AppUser(
                "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String adminToken = adminToken();
        String customerToken = jwtService.createToken(customer);

        try {
            mockMvc.perform(get("/api/v1/admin/profiles/{slug}/portfolio", slug)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/admin/profiles/{slug}/portfolio", slug)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].id").value(hidden.getId()))
                    .andExpect(jsonPath("$[0].published").value(false))
                    .andExpect(jsonPath("$[1].id").value(sameDaySecond.getId()))
                    .andExpect(jsonPath("$[2].id").value(sameDayFirst.getId()))
                    .andExpect(jsonPath("$[3].id").value(oldest.getId()));

            mockMvc.perform(get("/api/v1/profiles/{slug}/portfolio", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(sameDaySecond.getId()))
                    .andExpect(jsonPath("$[1].id").value(sameDayFirst.getId()))
                    .andExpect(jsonPath("$[2].id").value(oldest.getId()))
                    .andExpect(jsonPath("$[0].published").doesNotExist());

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}/portfolio/{itemId}", slug, sameDaySecond.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"VIDEO","title":"Segundo oculto","location":"Braga","eventDate":"2026-05-10","mediaUrl":"https://example.test/second.mp4","thumbnailUrl":"https://example.test/second.jpg","published":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.published").value(false));

            mockMvc.perform(get("/api/v1/profiles/{slug}/portfolio", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(sameDayFirst.getId()));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}/portfolio/{itemId}", slug, sameDaySecond.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"VIDEO","title":"Segundo visível","location":"Braga","eventDate":"2026-05-10","mediaUrl":"https://example.test/second.mp4","thumbnailUrl":"https://example.test/second.jpg","published":true}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.published").value(true));
        } finally {
            items.findById(hidden.getId()).ifPresent(items::delete);
            items.findById(sameDaySecond.getId()).ifPresent(items::delete);
            items.findById(sameDayFirst.getId()).ifPresent(items::delete);
            items.findById(oldest.getId()).ifPresent(items::delete);
            profiles.findById(profile.getId()).ifPresent(profiles::delete);
            users.findById(customer.getId()).ifPresent(users::delete);
        }
    }

    @Test
    void adminCanCreateUpdateAndToggleContactVisibility() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Contact visible = contacts.save(new Contact("Email visível " + suffix, ContactType.EMAIL, "visible-" + suffix + "@example.test", 0));
        Contact hidden = contacts.save(new Contact("Telefone oculto " + suffix, ContactType.PHONE, "+351 912 345 678", 1));
        hidden.update(hidden.getLabel(), hidden.getType(), hidden.getValue(), false);
        contacts.save(hidden);
        AppUser customer = users.save(new AppUser(
                "cliente-contactos-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String adminToken = adminToken();
        String customerToken = jwtService.createToken(customer);

        try {
            mockMvc.perform(get("/api/v1/admin/contacts")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/admin/contacts")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == %d && @.visible == true)]".formatted(visible.getId())).isNotEmpty())
                    .andExpect(jsonPath("$[?(@.id == %d && @.visible == false)]".formatted(hidden.getId())).isNotEmpty());

            mockMvc.perform(get("/api/v1/contacts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == %d)]".formatted(hidden.getId())).isEmpty());

            mockMvc.perform(put("/api/v1/admin/contacts/{id}", visible.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"Email editado","type":"EMAIL","value":"edited@example.test"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visible").value(true));

            mockMvc.perform(put("/api/v1/admin/contacts/{id}", visible.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"Email editado","type":"EMAIL","value":"edited@example.test","visible":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visible").value(false));

            mockMvc.perform(put("/api/v1/admin/contacts/{id}", hidden.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"Telefone público","type":"PHONE","value":"+351 912 345 678","visible":true}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visible").value(true));

            String contactIds = java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(hidden.getId(), visible.getId()),
                            contacts.findAllByOrderByDisplayOrderAscIdAsc()
                                    .stream()
                                    .map(Contact::getId)
                                    .filter(id -> !id.equals(hidden.getId()) && !id.equals(visible.getId())))
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));

            mockMvc.perform(put("/api/v1/admin/contacts/order")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"contactIds":[%s]}
                                    """.formatted(contactIds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(hidden.getId()))
                    .andExpect(jsonPath("$[1].id").value(visible.getId()));

            String createdContact = mockMvc.perform(post("/api/v1/admin/contacts")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"Novo contacto","type":"EMAIL","value":"novo-%s@example.test"}
                                    """.formatted(suffix)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.visible").value(true))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            Number createdId = com.jayway.jsonpath.JsonPath.read(createdContact, "$.id");
            contacts.findById(createdId.longValue()).ifPresent(contacts::delete);
        } finally {
            contacts.findById(visible.getId()).ifPresent(contacts::delete);
            contacts.findById(hidden.getId()).ifPresent(contacts::delete);
            users.findById(customer.getId()).ifPresent(users::delete);
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

    @Test
    void adminCanCreateUpdateAndValidateProfileSocialLinks() throws Exception {
        String slug = "social-admin-" + UUID.randomUUID().toString().substring(0, 8);
        String token = adminToken();

        try {
            mockMvc.perform(post("/api/v1/admin/profiles")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "slug":"%s",
                                      "name":"Perfil social",
                                      "role":"DJ",
                                      "description":"Perfil com links sociais",
                                      "socialLinks":[
                                        {"platform":"INSTAGRAM","label":"Instagram","url":"https://instagram.com/saltos"},
                                        {"platform":"EMAIL","label":"Email","url":"artista@example.test"}
                                      ]
                                    }
                                    """.formatted(slug)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.socialLinks.length()").value(2))
                    .andExpect(jsonPath("$.socialLinks[0].platform").value("INSTAGRAM"))
                    .andExpect(jsonPath("$.socialLinks[1].platform").value("EMAIL"))
                    .andExpect(jsonPath("$.socialLinks[1].url").value("artista@example.test"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"Perfil social atualizado",
                                      "role":"DJ",
                                      "description":"Perfil com links sociais atualizados",
                                      "profileImageUrl":null,
                                      "profileImagePosition":"50% 50%",
                                      "profileImageZoom":1,
                                      "featuredVideoUrl":null,
                                      "notificationEmail":null,
                                      "socialLinks":[
                                        {"platform":"YOUTUBE","label":"Canal","url":"https://youtube.com/@saltos"}
                                      ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.socialLinks.length()").value(1))
                    .andExpect(jsonPath("$.socialLinks[0].platform").value("YOUTUBE"))
                    .andExpect(jsonPath("$.socialLinks[0].label").value("Canal"));

            mockMvc.perform(get("/api/v1/profiles/{slug}", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.socialLinks.length()").value(1))
                    .andExpect(jsonPath("$.socialLinks[0].url").value("https://youtube.com/@saltos"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"Perfil social atualizado",
                                      "role":"DJ",
                                      "description":"Perfil com link inválido",
                                      "profileImageUrl":null,
                                      "profileImagePosition":"50% 50%",
                                      "profileImageZoom":1,
                                      "featuredVideoUrl":null,
                                      "notificationEmail":null,
                                      "socialLinks":[
                                        {"platform":"WEBSITE","label":"Site","url":"javascript:alert(1)"}
                                      ]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica um URL http/https válido para o link social"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"Perfil social atualizado",
                                      "role":"DJ",
                                      "description":"Perfil com data URI inválido",
                                      "profileImageUrl":null,
                                      "profileImagePosition":"50% 50%",
                                      "profileImageZoom":1,
                                      "featuredVideoUrl":null,
                                      "notificationEmail":null,
                                      "socialLinks":[
                                        {"platform":"WEBSITE","label":"Site","url":"data:text/html,test"}
                                      ]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica um URL http/https válido para o link social"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"Perfil social atualizado",
                                      "role":"DJ",
                                      "description":"Perfil com link protocol-relative",
                                      "profileImageUrl":null,
                                      "profileImagePosition":"50% 50%",
                                      "profileImageZoom":1,
                                      "featuredVideoUrl":null,
                                      "notificationEmail":null,
                                      "socialLinks":[
                                        {"platform":"WEBSITE","label":"Site","url":"//evil.example/perfil"}
                                      ]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica um URL http/https válido para o link social"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"Perfil social atualizado",
                                      "role":"DJ",
                                      "description":"Perfil com email inválido",
                                      "profileImageUrl":null,
                                      "profileImagePosition":"50% 50%",
                                      "profileImageZoom":1,
                                      "featuredVideoUrl":null,
                                      "notificationEmail":null,
                                      "socialLinks":[
                                        {"platform":"EMAIL","label":"Email","url":"email-invalido"}
                                      ]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Indica um email válido para o link social"));

            mockMvc.perform(put("/api/v1/admin/profiles/{slug}", slug)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name":"Perfil social atualizado",
                                      "role":"DJ",
                                      "description":"Perfil com links sociais a mais",
                                      "profileImageUrl":null,
                                      "profileImagePosition":"50%% 50%%",
                                      "profileImageZoom":1,
                                      "featuredVideoUrl":null,
                                      "notificationEmail":null,
                                      "socialLinks":[%s]
                                    }
                                    """.formatted(socialLinksJson(13))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.socialLinks").value("Um perfil pode ter no máximo 12 links sociais"));
        } finally {
            profiles.findBySlugAndActiveTrue(slug).ifPresent((profile) -> profiles.deleteById(profile.getId()));
        }
    }

    @Test
    void usersSubmitReviewsAndAdminControlsVisibility() throws Exception {
        reviews.deleteAll();
        String slug = "avaliacoes-" + UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile(slug, "Artista avaliado", "Animador", "Perfil para avaliações", null));
        AppUser customer = users.save(new AppUser(
                "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String adminToken = adminToken();
        String customerToken = jwtService.createToken(customer);
        Long reviewId = null;

        try {
            String createdReview = mockMvc.perform(post("/api/v1/profiles/{slug}/reviews", slug)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reviewerName":"Cliente teste","title":"Excelente festa","comment":"A animação correu muito bem do início ao fim.","rating":5}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.profileSlug").value(slug))
                    .andExpect(jsonPath("$.profileName").value("Artista avaliado"))
                    .andExpect(jsonPath("$.submittedByEmail").doesNotExist())
                    .andExpect(jsonPath("$.reviewerName").value("Cliente teste"))
                    .andExpect(jsonPath("$.rating").value(5))
                    .andExpect(jsonPath("$.published").value(false))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Number createdReviewId = com.jayway.jsonpath.JsonPath.read(createdReview, "$.id");
            reviewId = createdReviewId.longValue();

            mockMvc.perform(get("/api/v1/profiles/{slug}/reviews", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            mockMvc.perform(put("/api/v1/admin/reviews/{id}", reviewId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"published":true}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submittedByEmail").value(customer.getEmail()))
                    .andExpect(jsonPath("$.published").value(true));

            mockMvc.perform(get("/api/v1/profiles/{slug}/reviews", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].submittedByEmail").doesNotExist())
                    .andExpect(jsonPath("$[0].title").value("Excelente festa"));

            mockMvc.perform(put("/api/v1/admin/reviews/{id}", reviewId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"published":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.published").value(false));

            mockMvc.perform(get("/api/v1/profiles/{slug}/reviews", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            mockMvc.perform(delete("/api/v1/admin/reviews/{id}", reviewId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
            reviewId = null;
        } finally {
            if (reviewId != null) {
                reviews.findById(reviewId).ifPresent(reviews::delete);
            }
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    private String adminToken() {
        AppUser admin = users.findByEmailAndActiveTrue(ADMIN_EMAIL).orElseThrow();
        return jwtService.createToken(admin);
    }

    private static String socialLinksJson(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> """
                        {"platform":"WEBSITE","label":"Link %d","url":"https://example.test/%d"}"""
                        .formatted(index, index))
                .collect(java.util.stream.Collectors.joining(","));
    }
}
