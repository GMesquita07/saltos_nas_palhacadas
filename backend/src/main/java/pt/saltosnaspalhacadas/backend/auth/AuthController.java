package pt.saltosnaspalhacadas.backend.auth;

import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
        validatePhone(request.phone());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este email");
        }
        String username = normalizeUsername(request.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este nome de utilizador");
        }

        AppUser user = users.save(new AppUser(
                email,
                username,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                null,
                "50% 50%",
                1.0,
                passwords.encode(request.password()),
                UserRole.CUSTOMER));
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

    @PutMapping("/me")
    AuthenticatedUserResponse updateCurrentUser(Authentication authentication, @Valid @RequestBody UpdateUserRequest request) {
        AppUser user = findCurrentUser(authentication);
        validatePhone(request.phone());
        String username = normalizeUsername(request.username());

        users.findByEmailAndActiveTrue(user.getEmail())
                .filter(current -> current.getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));

        if (!username.equalsIgnoreCase(nullToEmpty(user.getUsername())) && users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este nome de utilizador");
        }

        user.updateProfile(
                username,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                emptyToNull(request.profileImageUrl()),
                defaultImagePosition(request.profileImagePosition()),
                defaultImageZoom(request.profileImageZoom()));

        return AuthenticatedUserResponse.from(users.save(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }

        return users.findByEmailAndActiveTrue(normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultImagePosition(String value) {
        return value == null || value.isBlank() ? "50% 50%" : value.trim();
    }

    private static double defaultImageZoom(Double value) {
        return value == null ? 1.0 : value;
    }

    private static void validatePhone(String phone) {
        String value = phone.trim();
        int digitCount = value.replaceAll("\\D", "").length();
        if (digitCount < 9 || digitCount > 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica um contacto telefónico válido");
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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
            @NotBlank(message = "O nome de utilizador é obrigatório")
            @Pattern(regexp = "(?!.*\\.\\.)(?!\\.)(?!.*\\.$)[a-z0-9._]{3,30}", message = "O nome de utilizador deve ter 3 a 30 caracteres, em minúsculas, e usar letras, números, ponto ou underscore")
            String username,
            @NotBlank(message = "O primeiro nome é obrigatório")
            @Size(max = 80, message = "O primeiro nome pode ter no máximo 80 caracteres")
            String firstName,
            @NotBlank(message = "O último nome é obrigatório")
            @Size(max = 80, message = "O último nome pode ter no máximo 80 caracteres")
            String lastName,
            @NotBlank(message = "O contacto telefónico é obrigatório")
            @Pattern(regexp = "\\+?[0-9][0-9()\\s.-]{7,24}", message = "Indica um contacto telefónico válido")
            String phone,
            @NotBlank(message = "A palavra-passe é obrigatória")
            @Size(min = 8, max = 128, message = "A palavra-passe deve ter entre 8 e 128 caracteres")
            String password) {
    }

    record UpdateUserRequest(
            @NotBlank(message = "O nome de utilizador é obrigatório")
            @Pattern(regexp = "(?!.*\\.\\.)(?!\\.)(?!.*\\.$)[a-z0-9._]{3,30}", message = "O nome de utilizador deve ter 3 a 30 caracteres, em minúsculas, e usar letras, números, ponto ou underscore")
            String username,
            @NotBlank(message = "O primeiro nome é obrigatório")
            @Size(max = 80, message = "O primeiro nome pode ter no máximo 80 caracteres")
            String firstName,
            @NotBlank(message = "O último nome é obrigatório")
            @Size(max = 80, message = "O último nome pode ter no máximo 80 caracteres")
            String lastName,
            @NotBlank(message = "O contacto telefónico é obrigatório")
            @Pattern(regexp = "\\+?[0-9][0-9()\\s.-]{7,24}", message = "Indica um contacto telefónico válido")
            String phone,
            @Size(max = 2048, message = "O URL da foto é demasiado longo")
            String profileImageUrl,
            @Pattern(regexp = "(?:100|[0-9]{1,2})% (?:100|[0-9]{1,2})%", message = "A posição da foto é inválida")
            String profileImagePosition,
            @DecimalMin(value = "1.0", message = "O zoom mínimo da foto é 1")
            @DecimalMax(value = "3.0", message = "O zoom máximo da foto é 3")
            Double profileImageZoom) {
    }

    record TokenResponse(String accessToken, String tokenType, String email, String username, String firstName, String lastName, String phone, String profileImageUrl, String profileImagePosition, double profileImageZoom, String role) {
        static TokenResponse from(AppUser user, String token) {
            return new TokenResponse(token, "Bearer", user.getEmail(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getPhone(), user.getProfileImageUrl(), user.getProfileImagePosition(), user.getProfileImageZoom(), user.getRole().name());
        }
    }

    record AuthenticatedUserResponse(String email, String username, String firstName, String lastName, String phone, String profileImageUrl, String profileImagePosition, double profileImageZoom, String role) {
        static AuthenticatedUserResponse from(AppUser user) {
            return new AuthenticatedUserResponse(user.getEmail(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getPhone(), user.getProfileImageUrl(), user.getProfileImagePosition(), user.getProfileImageZoom(), user.getRole().name());
        }
    }
}
