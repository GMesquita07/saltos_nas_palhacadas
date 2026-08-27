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
import java.time.Instant;
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
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaRepository;
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
    @Autowired private ManagedMediaRepository managedMedia;
    @Autowired private LocalMediaStorage storage;

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
        AppUser otherCustomer = users.save(new AppUser(
                "outro-cliente-" + suffix + "@example.test",
                "outro." + suffix,
                "Outro",
                "Cliente",
                "919999999",
                null,
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String customerToken = jwtService.createToken(customer);
        String otherCustomerToken = jwtService.createToken(otherCustomer);
        String adminToken = jwtService.createToken(admin);
        Long postId = null;

        try {
            UploadedMedia media = uploadPrivateMedia(customerToken, "festa.png", "image/png", pngHeader());
            UploadedMedia thumbnail = uploadPrivateMedia(customerToken, "festa-thumb.png", "image/png", pngHeader());
            String privateMediaPath = URI.create(media.url()).getPath();
            String publicMediaPath = privateMediaPath.replace("/api/v1/private-media/", "/api/v1/media/");

            mockMvc.perform(get(privateMediaPath))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + otherCustomerToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(publicMediaPath))
                    .andExpect(status().isNotFound());

            mockMvc.perform(post("/api/v1/client-posts")
                            .header("Authorization", "Bearer " + otherCustomerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileSlug":"%s","type":"PHOTO","mediaId":"%s","title":"Festa de outro","location":"Viseu","eventDate":"2026-08-20","caption":"Tentativa inválida","publicIdentity":"ANONYMOUS","showLocation":false,"showEventDate":false,"consentToPublish":true}
                                    """.formatted(profile.getSlug(), media.id())))
                    .andExpect(status().isNotFound());

            String createdPost = mockMvc.perform(post("/api/v1/client-posts")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profileSlug":"%s","type":"PHOTO","mediaId":"%s","thumbnailId":"%s","title":"Festa da família","location":"Viseu","eventDate":"2026-08-20","caption":"Momento partilhado pelo cliente.","publicIdentity":"USERNAME","showLocation":false,"showEventDate":true,"consentToPublish":true}
                                    """.formatted(profile.getSlug(), media.id(), thumbnail.id())))
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

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/v1/client-posts").queryParam("type", "PHOTO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Festa da família"))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"))
                    .andExpect(jsonPath("$[0].submittedByName").value("@" + customer.getUsername()))
                    .andExpect(jsonPath("$[0].submittedByEmail").doesNotExist())
                    .andExpect(jsonPath("$[0].location").doesNotExist())
                    .andExpect(jsonPath("$[0].eventDate").doesNotExist())
                    .andExpect(jsonPath("$[0].eventMonth").value("2026-08"));

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
            users.deleteById(otherCustomer.getId());
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
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.contentType").value("video/mp4"))
                    .andExpect(jsonPath("$.url", containsString("/api/v1/private-media/")));
        } finally {
            users.deleteById(customer.getId());
        }
    }

    @Test
    void legacyPrivateMediaRequiresPostOwnerOrAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Profile profile = profiles.save(new Profile("legacy-conteudo-" + suffix, "DJ Legacy", "DJ", "Perfil para conteúdo antigo", null));
        AppUser customer = users.save(new AppUser(
                "legacy-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        AppUser otherCustomer = users.save(new AppUser(
                "legacy-outro-" + suffix + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        AppUser admin = users.findByEmailAndActiveTrue("admin@example.test")
                .orElseGet(() -> users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN)));
        String customerToken = jwtService.createToken(customer);
        String otherCustomerToken = jwtService.createToken(otherCustomer);
        String adminToken = jwtService.createToken(admin);
        Long postId = null;
        String filename = null;

        try {
            UploadedMedia upload = uploadPrivateMedia(customerToken, "legacy.png", "image/png", pngHeader());
            managedMedia.deleteById(UUID.fromString(upload.id()));

            String privateMediaPath = URI.create(upload.url()).getPath();
            filename = privateMediaPath.substring(privateMediaPath.lastIndexOf('/') + 1);

            ClientContentPost legacyPost = posts.save(new ClientContentPost(
                    customer,
                    profile,
                    pt.saltosnaspalhacadas.backend.portfolio.MediaType.PHOTO,
                    "Festa antiga",
                    "Viseu",
                    LocalDate.now().minusDays(1),
                    "Legenda antiga",
                    upload.url(),
                    null,
                    null,
                    null,
                    "Cliente",
                    false,
                    false,
                    "legacy",
                    Instant.now()));
            postId = legacyPost.getId();

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + otherCustomerToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get(privateMediaPath)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        } finally {
            if (postId != null) {
                posts.deleteById(postId);
            }
            if (filename != null) {
                storage.deletePrivate(filename);
            }
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
            users.deleteById(otherCustomer.getId());
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
                                    {"profileSlug":"%s","type":"PHOTO","title":"Festa","location":"Viseu","eventDate":"2026-08-20","caption":"   ","consentToPublish":true}
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
                                    {"profileSlug":"%s","type":"PHOTO","title":"Festa","location":"Viseu","eventDate":"2026-08-20","caption":"Legenda válida","mediaUrl":"https://evil.example/festa.jpg","publicIdentity":"ANONYMOUS","consentToPublish":true}
                                    """.formatted(profile.getSlug())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.mediaId").value("Envia uma fotografia ou vídeo"));
        } finally {
            profiles.deleteById(profile.getId());
            users.deleteById(customer.getId());
        }
    }

    private UploadedMedia uploadPrivateMedia(String token, String filename, String contentType, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);

        String response = mockMvc.perform(multipart("/api/v1/client-posts/media")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", containsString("/api/v1/private-media/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new UploadedMedia(
                com.jayway.jsonpath.JsonPath.read(response, "$.id"),
                com.jayway.jsonpath.JsonPath.read(response, "$.url"));
    }

    private record UploadedMedia(String id, String url) {
    }

    private static byte[] mp4Header() {
        return new byte[] {0, 0, 0, 24, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d, 0, 0, 0, 1};
    }

    private static byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }
}
