package pt.saltosnaspalhacadas.backend.clientcontent.api;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.security.ClientIpAddress;
import pt.saltosnaspalhacadas.backend.security.IpRateLimiter;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/client-posts/media")
public class ClientContentMediaController {

    private final ClientContentMediaService mediaService;
    private final AppUserRepository users;
    private final IpRateLimiter rateLimiter;
    private final int uploadRateLimitPerMinute;

    public ClientContentMediaController(
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
    MediaUploadResponse upload(Authentication authentication, HttpServletRequest request, @RequestParam MultipartFile file) throws IOException {
        AppUser user = findCurrentUser(authentication);
        assertUploadAllowed(request);
        ManagedMedia media = mediaService.uploadPrivate(user, file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(LocalMediaStorage.PRIVATE_MEDIA_PATH)
                .path(media.getStorageKey())
                .toUriString();
        return new MediaUploadResponse(media.getId(), url, media.getContentType());
    }

    private void assertUploadAllowed(HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("client-content-media-upload", ClientIpAddress.from(request), uploadRateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados uploads em pouco tempo. Tenta novamente dentro de instantes.");
        }
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para carregar ficheiros");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }

    record MediaUploadResponse(UUID id, String url, String contentType) {
    }
}
