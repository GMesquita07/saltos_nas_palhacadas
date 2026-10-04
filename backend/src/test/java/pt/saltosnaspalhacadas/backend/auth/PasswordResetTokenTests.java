package pt.saltosnaspalhacadas.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.UserRole;

class PasswordResetTokenTests {
    @Test
    void isUsableOnlyBeforeExpiryAndWhileUnused() {
        Instant issuedAt = Instant.parse("2026-09-30T11:30:00Z");
        Instant expiresAt = issuedAt.plusSeconds(30 * 60);
        PasswordResetToken token = new PasswordResetToken(user(), "hash", expiresAt);

        assertThat(token.isUsable(expiresAt.minusNanos(1))).isTrue();
        assertThat(token.isUsable(expiresAt)).isFalse();
        assertThat(token.isUsable(expiresAt.plusNanos(1))).isFalse();

        PasswordResetToken usedToken = new PasswordResetToken(user(), "used-hash", expiresAt.plusSeconds(60));
        usedToken.markUsed(expiresAt.minusSeconds(1));
        assertThat(usedToken.isUsable(expiresAt.minusNanos(1))).isFalse();
    }

    private static AppUser user() {
        return new AppUser("reset-boundary@example.test", new BCryptPasswordEncoder().encode("password-antiga"), UserRole.CUSTOMER);
    }
}
