package pt.saltosnaspalhacadas.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaService;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaRepository;
import pt.saltosnaspalhacadas.backend.notification.EmailService;
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
    @Autowired private ManagedMediaService mediaService;
    @Autowired private PasswordResetTokenRepository passwordResetTokens;
    @MockitoBean private EmailService emailService;
    @Value("${app.auth.rate-limit-per-minute}")
    private int authRateLimit;

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
    void registrationRejectsUnexpectedRoleField() throws Exception {
        String email = "mass-assignment-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","username":"mass%s","firstName":"Cliente","lastName":"Teste","phone":"+351 912 345 678","password":"palavra123","role":"ADMIN"}
                """.formatted(email, UUID.randomUUID().toString().substring(0, 6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O pedido contém campos não permitidos."));
    }

    @Test
    void customerCanChangePasswordInsideAccount() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "password-" + suffix + "@example.test",
                "password." + suffix,
                "Cliente",
                "Seguro",
                "912345678",
                null,
                passwords.encode("password-antiga"),
                UserRole.CUSTOMER));
        String token = jwtService.createToken(customer);

        try {
            mockMvc.perform(put("/api/v1/auth/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"errada\",\"newPassword\":\"password-nova\"}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(put("/api/v1/auth/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"password-antiga\",\"newPassword\":\"password-nova\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"password-%s@example.test\",\"password\":\"password-antiga\"}".formatted(suffix)))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"password-%s@example.test\",\"password\":\"password-nova\"}".formatted(suffix)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        } finally {
            passwordResetTokens.deleteAllByUserId(customer.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void customerCanResetPasswordWithTemporaryToken() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "reset-" + suffix + "@example.test",
                "reset." + suffix,
                "Cliente",
                "Reset",
                "912345678",
                null,
                passwords.encode("password-antiga"),
                UserRole.CUSTOMER));
        String rawToken = "reset-token-" + suffix;
        passwordResetTokens.save(new PasswordResetToken(customer, tokenHash(rawToken), Instant.now().plusSeconds(900)));

        try {
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"%s\",\"newPassword\":\"password-nova\"}".formatted(rawToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"reset-%s@example.test\",\"password\":\"password-antiga\"}".formatted(suffix)))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"reset-%s@example.test\",\"password\":\"password-nova\"}".formatted(suffix)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"%s\",\"newPassword\":\"outra-password\"}".formatted(rawToken)))
                    .andExpect(status().isBadRequest());
        } finally {
            passwordResetTokens.deleteAllByUserId(customer.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void resetPasswordPrevalidationAcceptsValidTokenWithoutConsumingIt() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = createCustomer("prevalid-" + suffix);
        String rawToken = "prevalidation-token-" + suffix;
        passwordResetTokens.save(new PasswordResetToken(customer, tokenHash(rawToken), Instant.now().plusSeconds(900)));

        try {
            validateResetToken(rawToken, "198.51.100.11")
                    .andExpect(status().isNoContent());

            PasswordResetToken persistedToken = passwordResetTokens.findByTokenHash(tokenHash(rawToken)).orElseThrow();
            assertThat(persistedToken.getUsedAt()).isNull();

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .header("X-Forwarded-For", "198.51.100.12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"%s\",\"newPassword\":\"password-nova\"}".formatted(rawToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "198.51.100.13")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"password\":\"password-nova\"}".formatted(customer.getEmail())))
                    .andExpect(status().isOk());

            validateResetToken(rawToken, "198.51.100.14")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
        } finally {
            passwordResetTokens.deleteAllByUserId(customer.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void resetPasswordPrevalidationRejectsUnusableTokensWithGenericBadRequest() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser expiredUser = createCustomer("expired-" + suffix);
        AppUser usedUser = createCustomer("used-" + suffix);
        AppUser inactiveUser = createCustomer("inactive-" + suffix);
        String expiredToken = "expired-token-" + suffix;
        String usedToken = "used-token-" + suffix;
        String inactiveToken = "inactive-token-" + suffix;

        PasswordResetToken usedResetToken = new PasswordResetToken(usedUser, tokenHash(usedToken), Instant.now().plusSeconds(900));
        usedResetToken.markUsed(Instant.now());
        passwordResetTokens.save(new PasswordResetToken(expiredUser, tokenHash(expiredToken), Instant.now().minusSeconds(1)));
        passwordResetTokens.save(usedResetToken);
        passwordResetTokens.save(new PasswordResetToken(inactiveUser, tokenHash(inactiveToken), Instant.now().plusSeconds(900)));
        inactiveUser.anonymizeForDeletion("deleted-" + suffix + "@example.test", passwords.encode("disabled-password"));
        users.save(inactiveUser);

        try {
            validateResetToken("missing-token-" + suffix, "198.51.100.21")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            validateResetToken(expiredToken, "198.51.100.22")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            validateResetToken(usedToken, "198.51.100.23")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            validateResetToken(inactiveToken, "198.51.100.24")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            validateResetToken("   ", "198.51.100.25")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            validateResetToken("x".repeat(201), "198.51.100.26")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            for (String rejectedToken : new String[] {expiredToken, usedToken, inactiveToken}) {
                mockMvc.perform(post("/api/v1/auth/reset-password")
                                .header("X-Forwarded-For", "198.51.100.27")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"token":"%s","newPassword":"password-nova"}
                                        """.formatted(rejectedToken)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
            }
        } finally {
            passwordResetTokens.deleteAllByUserId(expiredUser.getId());
            passwordResetTokens.deleteAllByUserId(usedUser.getId());
            passwordResetTokens.deleteAllByUserId(inactiveUser.getId());
            users.deleteById(expiredUser.getId());
            users.deleteById(usedUser.getId());
            users.deleteById(inactiveUser.getId());
        }
    }

    @Test
    void forgotPasswordInvalidatesPreviousResetToken() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = createCustomer("replace-" + suffix);
        String oldToken = "old-token-" + suffix;
        passwordResetTokens.save(new PasswordResetToken(customer, tokenHash(oldToken), Instant.now().plusSeconds(900)));

        try {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .header("X-Forwarded-For", "198.51.100.31")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\"}".formatted(customer.getEmail())))
                    .andExpect(status().isNoContent());

            assertThat(passwordResetTokens.findByTokenHash(tokenHash(oldToken))).isEmpty();
            validateResetToken(oldToken, "198.51.100.32")
                    .andExpect(status().isBadRequest());
        } finally {
            passwordResetTokens.deleteAllByUserId(customer.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void resetPasswordFinalStillRejectsInvalidTokenWithoutPrevalidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("X-Forwarded-For", "198.51.100.41")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-token\",\"newPassword\":\"password-nova\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O link de recuperação é inválido ou já expirou"));
    }

    @Test
    void resetPrevalidationUsesAuthRateLimit() throws Exception {
        assertThat(authRateLimit).isPositive();
        for (int request = 0; request < authRateLimit; request++) {
            validateResetToken("missing-rate-limit-token", "198.51.100.71")
                    .andExpect(status().isBadRequest());
        }
        validateResetToken("missing-rate-limit-token", "198.51.100.71")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void resetPrevalidationRejectsMissingAndMalformedInput() throws Exception {
        for (String body : new String[] {"{}", "{\"token\":null}", "{\"token\":{}}", "{", "null"}) {
            mockMvc.perform(post("/api/v1/auth/reset-password/validate")
                            .header("X-Forwarded-For", "198.51.100.51")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void finalResetRechecksTokenUsedAfterPrevalidation() throws Exception {
        AppUser customer = createCustomer("race-" + UUID.randomUUID().toString().substring(0, 8));
        String rawToken = "race-token-" + UUID.randomUUID();
        PasswordResetToken token = passwordResetTokens.save(new PasswordResetToken(customer, tokenHash(rawToken), Instant.now().plusSeconds(900)));
        try {
            validateResetToken(rawToken, "198.51.100.61").andExpect(status().isNoContent());
            token.markUsed(Instant.now());
            passwordResetTokens.save(token);
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .header("X-Forwarded-For", "198.51.100.62")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token":"%s","newPassword":"password-nova"}
                                    """.formatted(rawToken)))
                    .andExpect(status().isBadRequest());
            assertThat(passwords.matches("password-antiga", users.findById(customer.getId()).orElseThrow().getPasswordHash())).isTrue();
        } finally {
            passwordResetTokens.deleteAllByUserId(customer.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void forgotPasswordEmailUsesResetPasswordRoute() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "forgot-" + suffix + "@example.test",
                "forgot." + suffix,
                "Cliente",
                "Reset",
                "912345678",
                null,
                passwords.encode("password-antiga"),
                UserRole.CUSTOMER));

        try {
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"forgot-%s@example.test\"}".formatted(suffix)))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
            verify(emailService).send(eq(customer.getEmail()), eq("Recuperar palavra-passe"), body.capture());
            assertThat(body.getValue())
                    .contains("/reset-password?resetToken=")
                    .doesNotContain("/?resetToken=");
        } finally {
            passwordResetTokens.deleteAllByUserId(customer.getId());
            users.deleteById(customer.getId());
        }
    }

    @Test
    void customerCanExportAndDeleteAccountWithPasswordConfirmation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser customer = users.save(new AppUser(
                "rgpd-" + suffix + "@example.test",
                "rgpd." + suffix,
                "Cliente",
                "RGPD",
                "912345678",
                null,
                passwords.encode("password-segura"),
                UserRole.CUSTOMER));
        String token = jwtService.createToken(customer);

        try {
            mockMvc.perform(get("/api/v1/auth/me/export")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profile.email").value("rgpd-" + suffix + "@example.test"))
                    .andExpect(jsonPath("$.profile.firstName").value("Cliente"));

            mockMvc.perform(delete("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password-errada\"}"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(delete("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"password-segura\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"rgpd-%s@example.test\",\"password\":\"password-segura\"}".formatted(suffix)))
                    .andExpect(status().isUnauthorized());

            AppUser deletedUser = users.findById(customer.getId()).orElseThrow();
            assertThat(deletedUser.isActive()).isFalse();
            assertThat(deletedUser.getDeletedAt()).isNotNull();
            assertThat(deletedUser.getEmail()).startsWith("deleted-user-");
        } finally {
            if (users.existsById(customer.getId())) {
                users.deleteById(customer.getId());
            }
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O pedido contém campos não permitidos."));

        mockMvc.perform(post("/api/v1/admin/contacts").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"label\":\"Email geral\",\"type\":\"EMAIL\",\"value\":\"ola@example.test\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.label").value("Email geral"));

        mockMvc.perform(get("/api/v1/contacts"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(jsonPath("$[0].value").value("ola@example.test"));
    }

    private AppUser createCustomer(String prefix) {
        return users.save(new AppUser(
                prefix + "@example.test",
                prefix.replace('-', '.'),
                "Cliente",
                "Reset",
                "912345678",
                null,
                passwords.encode("password-antiga"),
                UserRole.CUSTOMER));
    }

    private ResultActions validateResetToken(String rawToken, String clientIp) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password/validate")
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(rawToken)));
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

    private static String tokenHash(String token) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private record UploadedAvatar(String id, String url) {
    }
}
