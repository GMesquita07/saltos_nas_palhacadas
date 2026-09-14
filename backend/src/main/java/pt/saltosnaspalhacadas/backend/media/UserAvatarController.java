package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@RestController
@RequestMapping("/api/v1/auth/me/avatar")
public class UserAvatarController {

    private final AppUserRepository users;
    private final MediaStorage storage;

    public UserAvatarController(AppUserRepository users, MediaStorage storage) {
        this.users = users;
        this.storage = storage;
    }

    @GetMapping
    @Transactional(readOnly = true)
    ResponseEntity<Resource> show(Authentication authentication) throws IOException {
        AppUser user = findCurrentUser(authentication);
        ManagedMedia avatar = user.getProfileMedia();
        if (avatar == null
                || avatar.getPurpose() != ManagedMediaPurpose.PROFILE_AVATAR
                || avatar.getStatus() == ManagedMediaStatus.DELETED
                || avatar.getDeletedAt() != null
                || !avatar.isOwnedBy(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto de perfil não encontrada");
        }

        if (!storage.privateExists(avatar.getStorageKey())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto de perfil não encontrada");
        }

        return MediaHttpResponses.ok(storage.readPrivate(avatar.getStorageKey()).withContentType(avatar.getContentType()), true);
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para ver a tua foto");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }
}
