package pt.saltosnaspalhacadas.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaRepository;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndAdminIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ManagedMediaRepository managedMedia;
    @Autowired private ClientContentMediaService mediaService;

    @BeforeEach
    void ensureAdmin() {
        if (users.findByEmailAndActiveTrue("admin@example.test").isEmpty()) {
            users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN));
        }
    }

    @Test
    void adminCanLoginAndCreateProfile() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"admin@example.test\",\"password\":\"change-me-now\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));

        String token = jwtService.createToken(users.findByEmailAndActiveTrue("admin@example.test").orElseThrow());
        mockMvc.perform(post("/api/v1/admin/profiles").header("Authorization", "Bearer " + token).contentType("application/json").content("{\"slug\":\"dj-teste\",\"name\":\"DJ Teste\",\"role\":\"DJ\",\"description\":\"Perfil de teste\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("dj-teste"));
    }

    @Test
    void anonymousUserCannotCreateProfile() throws Exception {
        mockMvc.perform(post("/api/v1/admin/profiles").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAccessAdminEndpoints() throws Exception {
        AppUser customer = users.save(new AppUser(
                "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test",
                passwords.encode("palavra123"),
                UserRole.CUSTOMER));

        try {
            String token = jwtService.createToken(customer);
            mockMvc.perform(post("/api/v1/admin/profiles")
                            .header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content("{\"slug\":\"perfil-nao-autorizado\",\"name\":\"Sem permissão\",\"role\":\"DJ\",\"description\":\"Este perfil não deve ser criado\"}"))
                    .andExpect(status().isForbidden());
        } finally {
            users.deleteById(customer.getId());
        }
    }

    @Test
    void customerAvatarUploadIsPrivateAndReplacesPreviousAvatar() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "avatar-" + suffix + "@example.test",
                "avatar." + suffix,
                "Cliente",
                "Avatar",
                "912345678",
                null,
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        AppUser otherCustomer = users.save(new AppUser(
                "outro-avatar-" + suffix + "@example.test",
                "outro." + suffix,
                "Outro",
                "Cliente",
                "919999999",
                null,
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String customerToken = jwtService.createToken(customer);
        String otherToken = jwtService.createToken(otherCustomer);

        try {
            UploadedAvatar firstAvatar = uploadAvatar(customerToken, "avatar.png", "image/png");
            String firstPrivatePath = URI.create(firstAvatar.url()).getPath();
            String firstPublicPath = firstPrivatePath.replace("/api/v1/private-media/", "/api/v1/media/");

            mockMvc.perform(get(firstPublicPath))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(firstPrivatePath))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(firstPrivatePath)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(firstPrivatePath)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"avatar.%s","firstName":"Cliente","lastName":"Avatar","phone":"912345678","profileImageUrl":"%s","profileImageMediaId":"%s","profileImagePosition":"42%% 58%%","profileImageZoom":1.4}
                                    """.formatted(suffix, firstAvatar.url(), firstAvatar.id())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profileImageUrl", containsString("/api/v1/auth/me/avatar")))
                    .andExpect(jsonPath("$.profileImagePosition").value("42% 58%"))
                    .andExpect(jsonPath("$.profileImageZoom").value(1.4));

            mockMvc.perform(get("/api/v1/auth/me/avatar"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/auth/me/avatar")
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/v1/auth/me/avatar")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());

            UploadedAvatar secondAvatar = uploadAvatar(customerToken, "novo-avatar.png", "image/png");

            mockMvc.perform(put("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"avatar.%s","firstName":"Cliente","lastName":"Avatar","phone":"912345678","profileImageUrl":"%s","profileImageMediaId":"%s","profileImagePosition":"50%% 50%%","profileImageZoom":1.0}
                                    """.formatted(suffix, secondAvatar.url(), secondAvatar.id())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profileImageUrl", containsString("/api/v1/auth/me/avatar")));

            mockMvc.perform(get(firstPrivatePath)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/v1/auth/me/avatar")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());
        } finally {
            deleteManagedMediaFor(customer);
            deleteManagedMediaFor(otherCustomer);
            users.deleteById(customer.getId());
            users.deleteById(otherCustomer.getId());
        }
    }

    @Test
    void adminCanPublishAContactAndItIsPubliclyListed() throws Exception {
        String token = jwtService.createToken(users.findByEmailAndActiveTrue("admin@example.test").orElseThrow());

        mockMvc.perform(post("/api/v1/admin/contacts").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"label\":\"Email geral\",\"type\":\"EMAIL\",\"value\":\"ola@example.test\",\"displayOrder\":0}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.label").value("Email geral"));

        mockMvc.perform(get("/api/v1/contacts"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].value").value("ola@example.test"));
    }

    private UploadedAvatar uploadAvatar(String token, String filename, String contentType) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, pngHeader());

        String response = mockMvc.perform(multipart("/api/v1/media")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.contentType").value(contentType))
                .andExpect(jsonPath("$.url", containsString("/api/v1/private-media/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new UploadedAvatar(
                com.jayway.jsonpath.JsonPath.read(response, "$.id"),
                com.jayway.jsonpath.JsonPath.read(response, "$.url"));
    }

    private void deleteManagedMediaFor(AppUser user) throws Exception {
        for (ManagedMedia ownedMedia : managedMedia.findAllByOwnerId(user.getId())) {
            mediaService.delete(ownedMedia);
            managedMedia.delete(ownedMedia);
        }
    }

    private static byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }

    private record UploadedAvatar(String id, String url) {
    }
}
