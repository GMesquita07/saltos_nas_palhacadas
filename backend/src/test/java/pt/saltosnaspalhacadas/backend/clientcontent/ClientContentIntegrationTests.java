package pt.saltosnaspalhacadas.backend.clientcontent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

import java.net.URI;
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
            String mediaUrl = uploadPrivateMedia(customerToken, "festa.png", "image/png", pngHeader());
            String thumbnailUrl = uploadPrivateMedia(customerToken, "festa-thumb.png", "image/png", pngHeader());
            String privateMediaPath = URI.create(mediaUrl).getPath();
            String publicMediaPath = privateMediaPath.replace("/api/v1/private-media/", "/api/v1/media/");

            mockMvc.perform(get(privateMediaPath))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(publicMediaPath))
                    .andExpect(status().isNotFound());

            String createdPost = mockMvc.perform(post("/api/v1/client-posts")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileSlug":"%s","type":"PHOTO","title":"Festa da família","location":"Viseu","eventDate":"2026-08-20","caption":"Momento partilhado pelo cliente.","mediaUrl":"%s","thumbnailUrl":"%s"}
                                    """.formatted(profile.getSlug(), mediaUrl, thumbnailUrl)))
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
                    .andExpect(jsonPath("$.mediaUrl", containsString("/api/v1/media/")))
                    .andExpect(jsonPath("$.submittedByEmail").value(customer.getEmail()));

            mockMvc.perform(get(publicMediaPath))
                    .andExpect(status().isOk());

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
            MockMultipartFile file = new MockMultipartFile("file", "evento.mp4", "video/mp4", mp4Header());

            mockMvc.perform(multipart("/api/v1/client-posts/media")
                            .file(file)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contentType").value("video/mp4"))
                    .andExpect(jsonPath("$.url", containsString("/api/v1/private-media/")));
        } finally {
            users.deleteById(customer.getId());
        }
    }

    @Test
    void uploadedMediaMustUseAllowedContentAndServerGeneratedExtension() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "upload-image-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String customerToken = jwtService.createToken(customer);

        try {
            MockMultipartFile disguisedImage = new MockMultipartFile("file", "shell.php", "image/png", pngHeader());

            mockMvc.perform(multipart("/api/v1/media")
                            .file(disguisedImage)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contentType").value("image/png"))
                    .andExpect(jsonPath("$.url", endsWith(".png")));

            MockMultipartFile svg = new MockMultipartFile(
                    "file",
                    "script.svg",
                    "image/svg+xml",
                    "<svg><script>alert(1)</script></svg>".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/media")
                            .file(svg)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isBadRequest());
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

    @Test
    void customerSubmissionsRejectExternalMediaUrls() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile("url-externo-" + suffix, "DJ URL", "DJ", "Perfil para validar media", null));
        AppUser customer = users.save(new AppUser(
                "url-externo-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String customerToken = jwtService.createToken(customer);

        try {
            mockMvc.perform(post("/api/v1/client-posts")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileSlug":"%s","type":"PHOTO","title":"Festa","location":"Viseu","eventDate":"2026-08-20","caption":"Legenda válida","mediaUrl":"https://evil.example/festa.jpg"}
                                    """.formatted(profile.getSlug())))
                    .andExpect(status().isBadRequest());
        } finally {
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    private String uploadPrivateMedia(String token, String filename, String contentType, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        String response = mockMvc.perform(multipart("/api/v1/client-posts/media")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", containsString("/api/v1/private-media/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.url");
    }

    private static byte[] mp4Header() {
        return new byte[] {0, 0, 0, 24, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d, 0, 0, 0, 1};
    }

    private static byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }
}
