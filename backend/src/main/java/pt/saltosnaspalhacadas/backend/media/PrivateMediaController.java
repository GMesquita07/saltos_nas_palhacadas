package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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

    private final ManagedMediaService mediaService;
    private final AppUserRepository users;

    public PrivateMediaController(ManagedMediaService mediaService, AppUserRepository users) {
        this.mediaService = mediaService;
        this.users = users;
    }

    @GetMapping("/{filename:.+}")
    ResponseEntity<Resource> show(@PathVariable String filename, Authentication authentication) throws IOException {
        AppUser user = findCurrentUser(authentication);
        return MediaHttpResponses.ok(mediaService.requirePrivateDownload(filename, user), true);
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para ver este ficheiro");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }
}
