package pt.saltosnaspalhacadas.backend.clientcontent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class ClientContentIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwords;
    @Autowired private AppUserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private ClientContentPostRepository posts;

    @Test
    void customerSubmissionsStayHiddenUntilAdminApprovesThem() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile("cliente-conteudo-" + suffix, "DJ Cliente", "DJ", "Perfil para conteúdo dos clientes", null));
        AppUser customer = users.save(new AppUser(
                "cliente-conteudo-" + suffix + "@example.test",
                "cliente." + suffix,
                "Cliente",
                "Teste",
                "912345678",
                null,
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        AppUser admin = users.findByEmailAndActiveTrue("admin@example.test")
                .orElseGet(() -> users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN)));
        String customerToken = jwtService.createToken(customer);
        String adminToken = jwtService.createToken(admin);
        Long postId = null;

        try {
            String createdPost = mockMvc.perform(post("/api/v1/client-posts")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileSlug":"%s","type":"PHOTO","title":"Festa da família","location":"Viseu","eventDate":"2026-08-20","caption":"Momento partilhado pelo cliente.","mediaUrl":"https://example.test/festa.jpg","thumbnailUrl":"https://example.test/festa-thumb.jpg"}
                                    """.formatted(profile.getSlug())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.profileSlug").value(profile.getSlug()))
                    .andExpect(jsonPath("$.submittedByName").value("Cliente Teste"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Number createdPostId = com.jayway.jsonpath.JsonPath.read(createdPost, "$.id");
            postId = createdPostId.longValue();

            mockMvc.perform(get("/api/v1/client-posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            mockMvc.perform(get("/api/v1/client-posts/mine")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            mockMvc.perform(put("/api/v1/admin/client-posts/{id}", postId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status":"APPROVED","adminMessage":"Aprovado para publicação."}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.submittedByEmail").value(customer.getEmail()));

            mockMvc.perform(get("/api/v1/client-posts").queryParam("type", "PHOTO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Festa da família"))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"));

            mockMvc.perform(delete("/api/v1/admin/client-posts/{id}", postId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
            postId = null;
        } finally {
            if (postId != null) {
                posts.findById(postId).ifPresent(posts::delete);
            }
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void authenticatedCustomersCanUploadVideosForClientContent() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "upload-video-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String customerToken = jwtService.createToken(customer);

        try {
            MockMultipartFile file = new MockMultipartFile("file", "evento.mp4", "video/mp4", new byte[] {1, 2, 3});

            mockMvc.perform(multipart("/api/v1/media")
                            .file(file)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contentType").value("video/mp4"))
                    .andExpect(jsonPath("$.url").exists());
        } finally {
            users.deleteById(customer.getId());
        }
    }

    @Test
    void customerSubmissionsRequireACaption() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile("legenda-obrigatoria-" + suffix, "DJ Legenda", "DJ", "Perfil para validar legenda", null));
        AppUser customer = users.save(new AppUser(
                "legenda-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String customerToken = jwtService.createToken(customer);

        try {
            mockMvc.perform(post("/api/v1/client-posts")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileSlug":"%s","type":"PHOTO","title":"Festa","location":"Viseu","eventDate":"2026-08-20","caption":"   ","mediaUrl":"https://example.test/festa.jpg"}
                                    """.formatted(profile.getSlug())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.caption").value("A legenda é obrigatória"));
        } finally {
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }
}
