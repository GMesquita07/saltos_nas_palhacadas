package pt.saltosnaspalhacadas.backend.clientcontent.api;

import java.io.IOException;
import java.time.Duration;

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
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.security.ClientIpAddress;
import pt.saltosnaspalhacadas.backend.security.IpRateLimiter;

@RestController
@RequestMapping("/api/v1/client-posts/media")
public class ClientContentMediaController {

    private final LocalMediaStorage storage;
    private final IpRateLimiter rateLimiter;
    private final int uploadRateLimitPerMinute;

    public ClientContentMediaController(
            LocalMediaStorage storage,
            IpRateLimiter rateLimiter,
            @Value("${app.media.upload.rate-limit-per-minute:30}") int uploadRateLimitPerMinute) {
        this.storage = storage;
        this.rateLimiter = rateLimiter;
        this.uploadRateLimitPerMinute = uploadRateLimitPerMinute;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    MediaUploadResponse upload(HttpServletRequest request, @RequestParam MultipartFile file) throws IOException {
        assertUploadAllowed(request);
        LocalMediaStorage.StoredMedia media = storage.storePrivate(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(LocalMediaStorage.PRIVATE_MEDIA_PATH)
                .path(media.filename())
                .toUriString();
        return new MediaUploadResponse(url, media.contentType());
    }

    private void assertUploadAllowed(HttpServletRequest request) {
        if (!rateLimiter.tryAcquire("client-content-media-upload", ClientIpAddress.from(request), uploadRateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados uploads em pouco tempo. Tenta novamente dentro de instantes.");
        }
    }

    record MediaUploadResponse(String url, String contentType) {
    }
}
