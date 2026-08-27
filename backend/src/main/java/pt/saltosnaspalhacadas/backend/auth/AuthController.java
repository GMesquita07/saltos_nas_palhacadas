package pt.saltosnaspalhacadas.backend.auth;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaPurpose;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaStatus;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.security.ClientIpAddress;
import pt.saltosnaspalhacadas.backend.security.IpRateLimiter;
import pt.saltosnaspalhacadas.backend.user.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final IpRateLimiter rateLimiter;
    private final ClientContentMediaService mediaService;
    private final LocalMediaStorage storage;
    private final int authRateLimitPerMinute;

    public AuthController(
            AppUserRepository users,
            PasswordEncoder passwords,
            JwtService jwt,
            IpRateLimiter rateLimiter,
            ClientContentMediaService mediaService,
            LocalMediaStorage storage,
            @Value("${app.auth.rate-limit-per-minute:12}") int authRateLimitPerMinute) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.mediaService = mediaService;
        this.storage = storage;
        this.authRateLimitPerMinute = authRateLimitPerMinute;
    }

    @PostMapping("/login")
    TokenResponse login(HttpServletRequest servletRequest, @Valid @RequestBody LoginRequest request) {
        assertAuthAllowed(servletRequest);
        AppUser user = users.findByEmailAndActiveTrue(normalizeEmail(request.email()))
                .filter(candidate -> passwords.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou palavra-passe inválidos"));

        return TokenResponse.from(user, jwt.createToken(user), profileImageUrl(user));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse register(HttpServletRequest servletRequest, @Valid @RequestBody RegisterRequest request) {
        assertAuthAllowed(servletRequest);
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
        return TokenResponse.from(user, jwt.createToken(user), profileImageUrl(user));
    }

    @GetMapping("/me")
    AuthenticatedUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }

        AppUser user = users.findByEmailAndActiveTrue(normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));
        return AuthenticatedUserResponse.from(user, profileImageUrl(user));
    }

    @PutMapping("/me")
    @Transactional(rollbackFor = IOException.class)
    AuthenticatedUserResponse updateCurrentUser(Authentication authentication, @Valid @RequestBody UpdateUserRequest request) throws IOException {
        AppUser user = findCurrentUser(authentication);
        validatePhone(request.phone());
        String username = normalizeUsername(request.username());
        ManagedMedia previousAvatar = user.getProfileMedia();
        String previousLegacyUrl = user.getProfileImageUrl();
        ProfileImageSelection profileImage = resolveProfileImage(user, request);

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
                profileImage.legacyUrl(),
                profileImage.media(),
                defaultImagePosition(request.profileImagePosition()),
                defaultImageZoom(request.profileImageZoom()));

        AppUser savedUser = users.save(user);
        cleanupPreviousProfileImage(previousAvatar, previousLegacyUrl, profileImage);

        return AuthenticatedUserResponse.from(savedUser, profileImageUrl(savedUser));
    }

    private void assertAuthAllowed(HttpServletRequest servletRequest) {
        if (!rateLimiter.tryAcquire("auth", ClientIpAddress.from(servletRequest), authRateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas tentativas em pouco tempo. Tenta novamente dentro de instantes.");
        }
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

    private static String defaultImagePosition(String value) {
        return value == null || value.isBlank() ? "50% 50%" : value.trim();
    }

    private static double defaultImageZoom(Double value) {
        return value == null ? 1.0 : value;
    }

    private ProfileImageSelection resolveProfileImage(AppUser user, UpdateUserRequest request) {
        if (request.profileImageMediaId() != null) {
            ManagedMedia media = mediaService.attachOwnedPendingMedia(
                    request.profileImageMediaId(),
                    user,
                    MediaType.PHOTO,
                    ManagedMediaPurpose.PROFILE_AVATAR,
                    "Carrega a foto através do upload da conta antes de guardar");
            return new ProfileImageSelection(null, media);
        }

        String requestedUrl = blankToNull(request.profileImageUrl());
        if (requestedUrl == null) {
            return new ProfileImageSelection(null, null);
        }

        if (user.getProfileMedia() != null && isCurrentAvatarUrl(requestedUrl)) {
            return new ProfileImageSelection(null, user.getProfileMedia());
        }

        if (user.getProfileImageUrl() != null && user.getProfileImageUrl().equals(requestedUrl)) {
            return new ProfileImageSelection(user.getProfileImageUrl(), null);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrega a foto através do upload da conta antes de guardar");
    }

    private void cleanupPreviousProfileImage(ManagedMedia previousAvatar, String previousLegacyUrl, ProfileImageSelection nextProfileImage) throws IOException {
        if (previousAvatar != null && !sameMedia(previousAvatar, nextProfileImage.media())) {
            mediaService.delete(previousAvatar);
        }

        if (previousLegacyUrl != null && !previousLegacyUrl.equals(nextProfileImage.legacyUrl())) {
            storage.deleteManagedUrl(previousLegacyUrl);
        }
    }

    private static String profileImageUrl(AppUser user) {
        ManagedMedia media = user.getProfileMedia();
        if (media != null
                && media.getPurpose() == ManagedMediaPurpose.PROFILE_AVATAR
                && media.getStatus() != ManagedMediaStatus.DELETED
                && media.getDeletedAt() == null) {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/auth/me/avatar")
                    .toUriString();
        }
        return user.getProfileImageUrl();
    }

    private static boolean sameMedia(ManagedMedia first, ManagedMedia second) {
        return first != null
                && second != null
                && first.getId() != null
                && first.getId().equals(second.getId());
    }

    private static boolean isCurrentAvatarUrl(String value) {
        return value.equals("/api/v1/auth/me/avatar") || value.endsWith("/api/v1/auth/me/avatar");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
            UUID profileImageMediaId,
            @Pattern(regexp = "(?:100|[0-9]{1,2})% (?:100|[0-9]{1,2})%", message = "A posição da foto é inválida")
            String profileImagePosition,
            @DecimalMin(value = "1.0", message = "O zoom mínimo da foto é 1")
            @DecimalMax(value = "3.0", message = "O zoom máximo da foto é 3")
            Double profileImageZoom) {
    }

    record ProfileImageSelection(String legacyUrl, ManagedMedia media) {
    }

    record TokenResponse(String accessToken, String tokenType, String email, String username, String firstName, String lastName, String phone, String profileImageUrl, String profileImagePosition, double profileImageZoom, String role) {
        static TokenResponse from(AppUser user, String token, String profileImageUrl) {
            return new TokenResponse(token, "Bearer", user.getEmail(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getPhone(), profileImageUrl, user.getProfileImagePosition(), user.getProfileImageZoom(), user.getRole().name());
        }
    }

    record AuthenticatedUserResponse(String email, String username, String firstName, String lastName, String phone, String profileImageUrl, String profileImagePosition, double profileImageZoom, String role) {
        static AuthenticatedUserResponse from(AppUser user, String profileImageUrl) {
            return new AuthenticatedUserResponse(user.getEmail(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getPhone(), profileImageUrl, user.getProfileImagePosition(), user.getProfileImageZoom(), user.getRole().name());
        }
    }
}
