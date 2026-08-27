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
    private final LocalMediaStorage storage;

    public UserAvatarController(AppUserRepository users, LocalMediaStorage storage) {
        this.users = users;
        this.storage = storage;
    }

    @GetMapping
    @Transactional(readOnly = true)
    ResponseEntity<FileSystemResource> show(Authentication authentication) {
        AppUser user = findCurrentUser(authentication);
        ManagedMedia avatar = user.getProfileMedia();
        if (avatar == null
                || avatar.getPurpose() != ManagedMediaPurpose.PROFILE_AVATAR
                || avatar.getStatus() == ManagedMediaStatus.DELETED
                || avatar.getDeletedAt() != null
                || !avatar.isOwnedBy(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto de perfil não encontrada");
        }

        Path file = storage.privatePath(avatar.getStorageKey());
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto de perfil não encontrada");
        }

        MediaType contentType = MediaType.parseMediaType(avatar.getContentType());
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para ver a tua foto");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }
}
