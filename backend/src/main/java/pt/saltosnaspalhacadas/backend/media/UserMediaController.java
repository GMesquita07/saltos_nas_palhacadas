package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/media")
public class UserMediaController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;

    private final LocalMediaStorage storage;

    public UserMediaController(LocalMediaStorage storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    MediaUploadResponse uploadProfileImage(@RequestParam MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (file.isEmpty() || contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleciona uma imagem ou vídeo válido");
        }
        long limit = contentType.startsWith("image/") ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
        if (file.getSize() > limit) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ficheiro demasiado grande");
        }

        String filename = storage.store(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/media/").path(filename).toUriString();
        return new MediaUploadResponse(url, contentType);
    }

    record MediaUploadResponse(String url, String contentType) { }
}
