package pt.saltosnaspalhacadas.backend.auth;

import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.user.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthController(AppUserRepository users, PasswordEncoder passwords, JwtService jwt) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = users.findByEmailAndActiveTrue(normalizeEmail(request.email()))
                .filter(candidate -> passwords.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou palavra-passe inválidos"));

        return TokenResponse.from(user, jwt.createToken(user));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este email");
        }

        AppUser user = users.save(new AppUser(email, passwords.encode(request.password()), UserRole.CUSTOMER));
        return TokenResponse.from(user, jwt.createToken(user));
    }

    @GetMapping("/me")
    AuthenticatedUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }

        AppUser user = users.findByEmailAndActiveTrue(normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));
        return AuthenticatedUserResponse.from(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    record LoginRequest(
            @NotBlank(message = "O email é obrigatório")
            @Email(message = "Indica um endereço de email válido")
            @Size(max = 254, message = "O email pode ter no máximo 254 caracteres")
            String email,
            @NotBlank(message = "A palavra-passe é obrigatória")
            String password) {
    }

    record RegisterRequest(
            @NotBlank(message = "O email é obrigatório")
            @Email(message = "Indica um endereço de email válido")
            @Size(max = 254, message = "O email pode ter no máximo 254 caracteres")
            String email,
            @NotBlank(message = "A palavra-passe é obrigatória")
            @Size(min = 8, max = 128, message = "A palavra-passe deve ter entre 8 e 128 caracteres")
            String password) {
    }

    record TokenResponse(String accessToken, String tokenType, String role) {
        static TokenResponse from(AppUser user, String token) {
            return new TokenResponse(token, "Bearer", user.getRole().name());
        }
    }

    record AuthenticatedUserResponse(String email, String role) {
        static AuthenticatedUserResponse from(AppUser user) {
            return new AuthenticatedUserResponse(user.getEmail(), user.getRole().name());
        }
    }
}
