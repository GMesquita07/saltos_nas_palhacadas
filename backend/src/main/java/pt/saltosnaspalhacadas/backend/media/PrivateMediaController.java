package pt.saltosnaspalhacadas.backend.media;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@RestController
@RequestMapping("/api/v1/private-media")
public class PrivateMediaController {

    private final LocalMediaStorage storage;
    private final ClientContentMediaService mediaService;
    private final AppUserRepository users;

    public PrivateMediaController(LocalMediaStorage storage, ClientContentMediaService mediaService, AppUserRepository users) {
        this.storage = storage;
        this.mediaService = mediaService;
        this.users = users;
    }

    @GetMapping("/{filename:.+}")
    ResponseEntity<FileSystemResource> show(@PathVariable String filename, Authentication authentication) {
        AppUser user = findCurrentUser(authentication);
        ClientContentMediaService.PrivateMediaDownload media = mediaService.requirePrivateDownload(filename, user);

        Path file = storage.privatePath(filename);
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }

        MediaType contentType = media.contentType() == null
                ? MediaTypeFactory.getMediaType(file.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM)
                : MediaType.parseMediaType(media.contentType());
        if (!contentType.isConcrete()) {
            contentType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para ver este ficheiro");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }
}
