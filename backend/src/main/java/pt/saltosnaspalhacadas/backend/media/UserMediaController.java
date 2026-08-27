package pt.saltosnaspalhacadas.backend.media;

import java.time.Duration;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.saltosnaspalhacadas.backend.security.ClientIpAddress;
import pt.saltosnaspalhacadas.backend.security.IpRateLimiter;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@RestController
@RequestMapping("/api/v1/media")
public class UserMediaController {

    private final ClientContentMediaService mediaService;
    private final AppUserRepository users;
    private final IpRateLimiter rateLimiter;
    private final int uploadRateLimitPerMinute;

    public UserMediaController(
            ClientContentMediaService mediaService,
            AppUserRepository users,
            IpRateLimiter rateLimiter,
            @Value("${app.media.upload.rate-limit-per-minute:30}") int uploadRateLimitPerMinute) {
        this.mediaService = mediaService;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.uploadRateLimitPerMinute = uploadRateLimitPerMinute;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    MediaUploadResponse uploadProfileImage(HttpServletRequest request, Authentication authentication, @RequestParam MultipartFile file) throws IOException {
        assertUploadAllowed(request);
        AppUser user = findCurrentUser(authentication);
        ManagedMedia media = mediaService.uploadPrivateAvatar(user, file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(LocalMediaStorage.PRIVATE_MEDIA_PATH)
                .path(media.getStorageKey())
                .toUriString();
        return new MediaUploadResponse(media.getId(), url, media.getContentType());
    }

    private void assertUploadAllowed(HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("media-upload", ClientIpAddress.from(request), uploadRateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados uploads em pouco tempo. Tenta novamente dentro de instantes.");
        }
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para carregar a tua foto");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }

    record MediaUploadResponse(UUID id, String url, String contentType) { }
}
