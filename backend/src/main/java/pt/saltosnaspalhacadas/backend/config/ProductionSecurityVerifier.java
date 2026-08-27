package pt.saltosnaspalhacadas.backend.config;

import java.util.Arrays;
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
            "password",
            "change-me-now");

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
        requireExternalServicesWhenEnabled();
    }

    private void requireStrongJwtSecret() {
        String secret = required("app.security.jwt.secret", "JWT_SECRET é obrigatório em produção");
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
            if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve apontar para o domínio real do frontend em produção");
            }
        }
    }

    private void requireExternalServicesWhenEnabled() {
        if (environment.getProperty("app.support.ai.enabled", Boolean.class, false)) {
            required("app.support.ai.api-key", "OPENAI_API_KEY é obrigatório quando SUPPORT_AI_ENABLED=true em produção");
            required("app.support.ai.endpoint", "OPENAI_API_ENDPOINT é obrigatório quando SUPPORT_AI_ENABLED=true em produção");
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

    private String required(String property, String errorMessage) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }
        return value.trim();
    }
}
