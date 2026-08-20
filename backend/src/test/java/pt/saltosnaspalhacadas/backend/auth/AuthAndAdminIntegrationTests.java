package pt.saltosnaspalhacadas.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    void adminCanPublishAContactAndItIsPubliclyListed() throws Exception {
        String token = jwtService.createToken(users.findByEmailAndActiveTrue("admin@example.test").orElseThrow());

        mockMvc.perform(post("/api/v1/admin/contacts").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"label\":\"Email geral\",\"type\":\"EMAIL\",\"value\":\"ola@example.test\",\"displayOrder\":0}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.label").value("Email geral"));

        mockMvc.perform(get("/api/v1/contacts"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].value").value("ola@example.test"));
    }
}
