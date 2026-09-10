package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class PublicMediaController {
    private final MediaStorage storage;

    public PublicMediaController(MediaStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/{filename:.+}")
    ResponseEntity<Resource> show(@PathVariable String filename) throws IOException {
        return MediaHttpResponses.ok(storage.readPublic(filename), false);
    }
}
