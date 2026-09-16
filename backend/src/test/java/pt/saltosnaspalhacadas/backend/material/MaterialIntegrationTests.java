package pt.saltosnaspalhacadas.backend.material;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwords;
    @Autowired private AppUserRepository users;
    @Autowired private MaterialRepository materials;

    @Test
    void publicCanListMaterialsAndOnlyAdminCanManageThem() throws Exception {
        AppUser admin = users.findByEmailAndActiveTrue("admin@example.test")
                .orElseGet(() -> users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN)));
        String adminToken = jwtService.createToken(admin);
        String name = "Máquina de fumo " + UUID.randomUUID().toString().substring(0, 8);
        Long materialId = null;

        try {
            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/admin/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"%s","imageUrl":"https://example.test/fumo.jpg"}
                                    """.formatted(name)))
                    .andExpect(status().isForbidden());

            String createdMaterial = mockMvc.perform(post("/api/v1/admin/materials")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"%s","imageUrl":"https://example.test/fumo.jpg"}
                                    """.formatted(name)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(name))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Number createdMaterialId = com.jayway.jsonpath.JsonPath.read(createdMaterial, "$.id");
            materialId = createdMaterialId.longValue();

            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == %d)].name".formatted(materialId), contains(name)));

            mockMvc.perform(delete("/api/v1/admin/materials/{id}", materialId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
            materialId = null;

            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", not(hasItem(name))));
        } finally {
            if (materialId != null) {
                materials.findById(materialId).ifPresent(materials::delete);
            }
        }
    }

    @Test
    void adminCanChooseMaterialDisplayOrder() throws Exception {
        AppUser admin = users.findByEmailAndActiveTrue("admin@example.test")
                .orElseGet(() -> users.save(new AppUser("admin@example.test", passwords.encode("change-me-now"), UserRole.ADMIN)));
        String adminToken = jwtService.createToken(admin);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Material first = materials.save(new Material("Primeiro " + suffix, "https://example.test/primeiro.jpg", 0));
        Material second = materials.save(new Material("Segundo " + suffix, "https://example.test/segundo.jpg", 1));

        try {
            mockMvc.perform(put("/api/v1/admin/materials/order")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"materialIds":[%d,%d]}
                                    """.formatted(second.getId(), first.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(second.getId()))
                    .andExpect(jsonPath("$[1].id").value(first.getId()));

            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(second.getId()))
                    .andExpect(jsonPath("$[1].id").value(first.getId()));
        } finally {
            materials.findById(first.getId()).ifPresent(materials::delete);
            materials.findById(second.getId()).ifPresent(materials::delete);
        }
    }
}
