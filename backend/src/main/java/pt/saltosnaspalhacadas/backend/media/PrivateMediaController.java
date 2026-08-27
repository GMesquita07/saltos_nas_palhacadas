package pt.saltosnaspalhacadas.backend.media;

import java.nio.file.Files;
import java.nio.file.Path;

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

@RestController
@RequestMapping("/api/v1/private-media")
public class PrivateMediaController {

    private final LocalMediaStorage storage;

    public PrivateMediaController(LocalMediaStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/{filename:.+}")
    ResponseEntity<FileSystemResource> show(@PathVariable String filename, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para ver este ficheiro");
        }
        if (!storage.isSafeFilename(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }

        Path file = storage.privatePath(filename);
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }

        MediaType contentType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }
}
