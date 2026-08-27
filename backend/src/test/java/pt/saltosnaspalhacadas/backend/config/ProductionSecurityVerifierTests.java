package pt.saltosnaspalhacadas.backend.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityVerifierTests {

    @Test
    void ignoresNonProductionProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertThatCode(() -> new ProductionSecurityVerifier(environment).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionWithLocalhostCors() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.cors.allowed-origins", "http://localhost:5173");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("domínio real");
    }

    @Test
    void rejectsProductionWithWeakAdminPassword() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.bootstrap.admin.password", "11223344");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void acceptsProductionWithRequiredSecuritySettings() {
        assertThatCode(() -> new ProductionSecurityVerifier(validProductionEnvironment()).run(null))
                .doesNotThrowAnyException();
    }

    private static MockEnvironment validProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment
                .withProperty("app.security.jwt.secret", base64Secret())
                .withProperty("app.bootstrap.admin.email", "admin@saltosnaspalhacadas.pt")
                .withProperty("app.bootstrap.admin.password", "uma-password-forte-2026")
                .withProperty("app.cors.allowed-origins", "https://saltosnaspalhacadas.pt")
                .withProperty("app.support.ai.enabled", "false")
                .withProperty("app.booking.email.enabled", "false");
        return environment;
    }

    private static String base64Secret() {
        return Base64.getEncoder().encodeToString(
                "saltos-prod-secret-with-more-than-thirty-two-bytes".getBytes(StandardCharsets.UTF_8));
    }
}
