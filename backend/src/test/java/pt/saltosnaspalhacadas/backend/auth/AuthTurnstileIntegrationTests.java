package pt.saltosnaspalhacadas.backend.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.security.TurnstileService;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTurnstileIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder passwords;

    @MockitoBean
    private TurnstileService turnstileService;

    @BeforeEach
    void resetTurnstileMock() {
        reset(turnstileService);
    }

    @Test
    void loginValidatesTurnstileAfterRateLimitAndBeforeAuthentication() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser user = users.save(new AppUser(
                "turnstile-login-" + suffix + "@example.test",
                "turnstile.login." + suffix,
                "Cliente",
                "Turnstile",
                "912345678",
                null,
                passwords.encode("password-segura"),
                UserRole.CUSTOMER));

        try {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Turnstile-Token", "login-token")
                            .header("X-Forwarded-For", "203.0.113.44, 10.0.0.10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"password-segura"}
                                    """.formatted(user.getEmail())))
                    .andExpect(status().isOk());

            verify(turnstileService).verify(eq("login-token"), eq("login"), eq("203.0.113.44"));
        } finally {
            users.deleteById(user.getId());
        }
    }

    @Test
    void registerValidatesTurnstileWithRegisterAction() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "turnstile-register-" + suffix + "@example.test";

        try {
            mockMvc.perform(post("/api/v1/auth/register")
                            .header("X-Turnstile-Token", "register-token")
                            .header("X-Forwarded-For", "203.0.113.45")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","username":"ts%s","firstName":"Cliente","lastName":"Seguro","phone":"+351 912 345 678","password":"password-segura"}
                                    """.formatted(email, suffix)))
                    .andExpect(status().isCreated());

            verify(turnstileService).verify(eq("register-token"), eq("register"), eq("203.0.113.45"));
        } finally {
            users.findByEmailAndActiveTrue(email).ifPresent(user -> users.deleteById(user.getId()));
        }
    }

    @Test
    void forgotPasswordValidatesTurnstileWithForgotPasswordAction() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .header("X-Turnstile-Token", "forgot-token")
                        .header("X-Forwarded-For", "203.0.113.46")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.test"}
                                """))
                .andExpect(status().isNoContent());

        verify(turnstileService).verify(eq("forgot-token"), eq("forgot_password"), eq("203.0.113.46"));
    }

    @Test
    void resetPasswordDoesNotValidateTurnstile() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("X-Turnstile-Token", "ignored-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"invalid-token","newPassword":"password-nova"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(turnstileService);
    }
}
