package pt.saltosnaspalhacadas.backend.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.io.Decoders;

@Component
class ProductionSecurityVerifier implements ApplicationRunner {
    private static final Set<String> FORBIDDEN_ADMIN_PASSWORDS = Set.of(
            "11223344",
            "admin",
            "admin123456",
            "password",
            "password123",
            "change-me-now");
    private static final Set<String> FORBIDDEN_JWT_SECRETS = Set.of(
            "c2FsdG9zLWRldi1zZWNyZXQtY2hhbmdlLW1lLTIwMjYtMDE=",
            "c2FsdG9zLXRlc3Qtc2VjcmV0LWNoYW5nZS1tZS0yMDI2LTAx");

    private final Environment environment;

    ProductionSecurityVerifier(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        requireStrongJwtSecret();
        requireStrongAdminCredentials();
        requireProductionCors();
        requireHttpsHeaders();
        requireDatabaseSslWhenConfigured();
        requireKnownMediaStorageProvider();
        requireExternalServicesWhenEnabled();
    }

    private void requireStrongJwtSecret() {
        String secret = required("app.security.jwt.secret", "JWT_SECRET é obrigatório em produção");
        if (FORBIDDEN_JWT_SECRETS.contains(secret.trim())) {
            throw new IllegalStateException("JWT_SECRET não pode ser um segredo de desenvolvimento ou teste");
        }
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length < 32) {
                throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes depois de descodificado em Base64");
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET deve estar em Base64 e ter pelo menos 32 bytes", exception);
        }
    }

    private void requireStrongAdminCredentials() {
        String adminEmail = required("app.bootstrap.admin.email", "ADMIN_EMAIL é obrigatório em produção");
        if (!adminEmail.contains("@")) {
            throw new IllegalStateException("ADMIN_EMAIL deve ser um email válido");
        }

        String adminPassword = required("app.bootstrap.admin.password", "ADMIN_PASSWORD é obrigatório em produção");
        if (adminPassword.length() < 12 || FORBIDDEN_ADMIN_PASSWORDS.contains(adminPassword.toLowerCase())) {
            throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos 12 caracteres e não pode ser uma password de exemplo");
        }
    }

    private void requireProductionCors() {
        String allowedOrigins = required("app.cors.allowed-origins", "CORS_ALLOWED_ORIGINS é obrigatório em produção");
        for (String origin : allowedOrigins.split(",")) {
            String normalized = origin.trim().toLowerCase();
            if (normalized.isBlank() || "*".equals(normalized)) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS não pode conter origens vazias ou wildcard em produção");
            }
            URI uri = parseUri(origin.trim(), "CORS_ALLOWED_ORIGINS deve conter origens HTTPS válidas");
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve conter apenas origens HTTPS em produção");
            }
            if ((uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve conter só a origem, sem path, query ou fragment");
            }
            if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve apontar para o domínio real do frontend em produção");
            }
        }

        String frontendUrl = required("app.frontend.public-url", "APP_PUBLIC_URL é obrigatório em produção");
        URI frontendUri = parseUri(frontendUrl, "APP_PUBLIC_URL deve ser o URL HTTPS público do frontend em produção");
        String normalizedFrontendUrl = frontendUrl.toLowerCase();
        if (!"https".equalsIgnoreCase(frontendUri.getScheme())
                || frontendUri.getHost() == null
                || normalizedFrontendUrl.contains("localhost")
                || normalizedFrontendUrl.contains("127.0.0.1")) {
            throw new IllegalStateException("APP_PUBLIC_URL deve ser o URL HTTPS público do frontend em produção");
        }
    }

    private void requireHttpsHeaders() {
        if (!environment.getProperty("app.security.hsts.enabled", Boolean.class, false)) {
            throw new IllegalStateException("SECURITY_HSTS_ENABLED deve estar ativo em produção");
        }
    }

    private void requireDatabaseSslWhenConfigured() {
        if (!environment.getProperty("app.security.require-database-ssl", Boolean.class, false)) {
            return;
        }

        String datasourceUrl = required("spring.datasource.url", "DB_URL é obrigatório em produção");
        String normalized = datasourceUrl.toLowerCase();
        if (!normalized.startsWith("jdbc:postgresql:")) {
            return;
        }
        if (!normalized.contains("sslmode=require")
                && !normalized.contains("sslmode=verify-ca")
                && !normalized.contains("sslmode=verify-full")
                && !normalized.contains("ssl=true")) {
            throw new IllegalStateException("DB_URL em produção deve exigir SSL, por exemplo com sslmode=require");
        }
    }

    private void requireKnownMediaStorageProvider() {
        String storageProvider = environment.getProperty("app.media.storage-provider");
        if (storageProvider == null || !"r2".equals(storageProvider.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("MEDIA_STORAGE_PROVIDER deve ser r2 em produção; storage local não é persistente.");
        }
        requireR2Configuration();
    }

    private void requireR2Configuration() {
        String endpoint = required("app.media.r2.endpoint", "R2_ENDPOINT é obrigatório quando MEDIA_STORAGE_PROVIDER=r2 em produção");
        URI endpointUri = parseUri(endpoint, "R2_ENDPOINT deve ser um URL HTTPS válido");
        String normalizedEndpoint = endpoint.toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(endpointUri.getScheme())
                || endpointUri.getHost() == null
                || normalizedEndpoint.contains("localhost")
                || normalizedEndpoint.contains("127.0.0.1")) {
            throw new IllegalStateException("R2_ENDPOINT deve ser HTTPS e não pode apontar para localhost em produção");
        }

        required("app.media.r2.access-key-id", "R2_ACCESS_KEY_ID é obrigatório quando MEDIA_STORAGE_PROVIDER=r2 em produção");
        required("app.media.r2.secret-access-key", "R2_SECRET_ACCESS_KEY é obrigatório quando MEDIA_STORAGE_PROVIDER=r2 em produção");
        String publicBucket = required("app.media.r2.public-bucket", "R2_PUBLIC_BUCKET é obrigatório quando MEDIA_STORAGE_PROVIDER=r2 em produção");
        String privateBucket = required("app.media.r2.private-bucket", "R2_PRIVATE_BUCKET é obrigatório quando MEDIA_STORAGE_PROVIDER=r2 em produção");
        if (publicBucket.equals(privateBucket)) {
            throw new IllegalStateException("R2_PUBLIC_BUCKET e R2_PRIVATE_BUCKET devem ser buckets diferentes");
        }
    }

    private void requireExternalServicesWhenEnabled() {
        if (environment.getProperty("app.support.ai.enabled", Boolean.class, false)) {
            required("app.support.ai.api-key", "OPENAI_API_KEY é obrigatório quando SUPPORT_AI_ENABLED=true em produção");
            String endpoint = required("app.support.ai.endpoint", "OPENAI_API_ENDPOINT é obrigatório quando SUPPORT_AI_ENABLED=true em produção");
            URI endpointUri = parseUri(endpoint, "OPENAI_API_ENDPOINT deve ser HTTPS quando SUPPORT_AI_ENABLED=true em produção");
            if (!"https".equalsIgnoreCase(endpointUri.getScheme())) {
                throw new IllegalStateException("OPENAI_API_ENDPOINT deve ser HTTPS quando SUPPORT_AI_ENABLED=true em produção");
            }
            required("app.support.ai.model", "OPENAI_MODEL é obrigatório quando SUPPORT_AI_ENABLED=true em produção");
        }

        if (environment.getProperty("app.booking.email.enabled", Boolean.class, false)) {
            required("app.booking.email.smtp-host", "BOOKING_EMAIL_SMTP_HOST é obrigatório quando BOOKING_EMAIL_ENABLED=true em produção");
            required("app.booking.email.from", "BOOKING_EMAIL_FROM é obrigatório quando BOOKING_EMAIL_ENABLED=true em produção");
            boolean ssl = environment.getProperty("app.booking.email.smtp-ssl", Boolean.class, false);
            boolean startTls = environment.getProperty("app.booking.email.smtp-starttls", Boolean.class, false);
            if (!ssl && !startTls) {
                throw new IllegalStateException("SMTP em produção deve usar SSL ou STARTTLS");
            }
        }
    }

    private static URI parseUri(String value, String errorMessage) {
        try {
            return URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private String required(String property, String errorMessage) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }
        return value.trim();
    }
}
