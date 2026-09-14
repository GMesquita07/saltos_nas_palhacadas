package pt.saltosnaspalhacadas.backend.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityVerifierTests {

    @Test
    void allowsDevWithoutMediaStorageProvider() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertThatCode(() -> new ProductionSecurityVerifier(environment).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionWithLocalhostCors() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.cors.allowed-origins", "https://localhost:5173");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("domínio real");
    }

    @Test
    void rejectsProductionWithHttpCors() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.cors.allowed-origins", "http://saltosnaspalhacadas.pt");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void rejectsProductionWithoutHsts() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.security.hsts.enabled", "false");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECURITY_HSTS_ENABLED");
    }

    @Test
    void rejectsProductionDatabaseWithoutSslWhenRequired() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.example.test/saltos")
                .withProperty("app.security.require-database-ssl", "true");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sslmode=require");
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
    void rejectsProductionWithoutMaintenanceApiKey() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.maintenance.api-key", "");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAINTENANCE_API_KEY");
    }

    @Test
    void rejectsProductionWithWeakMaintenanceApiKey() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.maintenance.api-key", "change-me-maintenance-secret-key-2026");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAINTENANCE_API_KEY");
    }

    @Test
    void rejectsProductionWithoutMediaStorageProvider() {
        assertThatThrownBy(() -> new ProductionSecurityVerifier(validProductionEnvironment()).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEDIA_STORAGE_PROVIDER deve ser r2 em produção");
    }

    @Test
    void rejectsProductionWithLocalMediaStorageProvider() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.media.storage-provider", "local");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage local não é persistente");
    }

    @Test
    void acceptsProductionWithValidR2Configuration() {
        assertThatCode(() -> new ProductionSecurityVerifier(validProductionEnvironmentWithR2()).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsProductionWithStrongMaintenanceApiKey() {
        MockEnvironment environment = validProductionEnvironmentWithR2()
                .withProperty("app.maintenance.api-key", "8Rq4Vz7Lm2Np5Qx9Tb3Yw6Gc1Hd4Ks2P");

        assertThatCode(() -> new ProductionSecurityVerifier(environment).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionWithUnknownMediaStorageProvider() {
        MockEnvironment environment = validProductionEnvironment()
                .withProperty("app.media.storage-provider", "ftp");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEDIA_STORAGE_PROVIDER");
    }

    @Test
    void rejectsProductionR2WithoutCredentials() {
        MockEnvironment environment = validProductionEnvironmentWithR2()
                .withProperty("app.media.r2.secret-access-key", "");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_SECRET_ACCESS_KEY");
    }

    @Test
    void rejectsProductionR2WithHttpEndpoint() {
        MockEnvironment environment = validProductionEnvironmentWithR2()
                .withProperty("app.media.r2.endpoint", "http://account.eu.r2.cloudflarestorage.com");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_ENDPOINT");
    }

    @Test
    void rejectsProductionR2WithSameBucketForPublicAndPrivateMedia() {
        MockEnvironment environment = validProductionEnvironmentWithR2()
                .withProperty("app.media.r2.private-bucket", "saltos-public");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("buckets diferentes");
    }

    @Test
    void rejectsProductionEmailWithoutTls() {
        MockEnvironment environment = validProductionEnvironmentWithR2()
                .withProperty("app.booking.email.enabled", "true")
                .withProperty("app.booking.email.smtp-host", "smtp.example.test")
                .withProperty("app.booking.email.from", "no-reply@saltosnaspalhacadas.pt")
                .withProperty("app.booking.email.smtp-ssl", "false")
                .withProperty("app.booking.email.smtp-starttls", "false");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP");
    }

    @Test
    void rejectsProductionAiWithoutModel() {
        MockEnvironment environment = validProductionEnvironmentWithR2()
                .withProperty("app.support.ai.enabled", "true")
                .withProperty("app.support.ai.api-key", "sk-test")
                .withProperty("app.support.ai.endpoint", "https://api.openai.com/v1/responses")
                .withProperty("app.support.ai.model", "");

        assertThatThrownBy(() -> new ProductionSecurityVerifier(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_MODEL");
    }

    private static MockEnvironment validProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment
                .withProperty("app.security.jwt.secret", base64Secret())
                .withProperty("app.bootstrap.admin.email", "admin@saltosnaspalhacadas.pt")
                .withProperty("app.bootstrap.admin.password", "uma-password-forte-2026")
                .withProperty("app.maintenance.api-key", "9xVf7Qr2Lm8Np4Ts6Yb3Wd5Gh7Jk2MzQ")
                .withProperty("app.cors.allowed-origins", "https://saltosnaspalhacadas.pt")
                .withProperty("app.frontend.public-url", "https://saltosnaspalhacadas.pt")
                .withProperty("app.security.hsts.enabled", "true")
                .withProperty("app.security.require-database-ssl", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.example.test/saltos?sslmode=require")
                .withProperty("app.support.ai.enabled", "false")
                .withProperty("app.booking.email.enabled", "false");
        return environment;
    }

    private static MockEnvironment validProductionEnvironmentWithR2() {
        return validProductionEnvironment()
                .withProperty("app.media.storage-provider", "r2")
                .withProperty("app.media.r2.endpoint", "https://account.eu.r2.cloudflarestorage.com")
                .withProperty("app.media.r2.access-key-id", "access-key-id")
                .withProperty("app.media.r2.secret-access-key", "secret-access-key")
                .withProperty("app.media.r2.public-bucket", "saltos-public")
                .withProperty("app.media.r2.private-bucket", "saltos-private");
    }

    private static String base64Secret() {
        return Base64.getEncoder().encodeToString(
                "saltos-prod-secret-with-more-than-thirty-two-bytes".getBytes(StandardCharsets.UTF_8));
    }
}
