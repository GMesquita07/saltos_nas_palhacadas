package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalMediaStorage {
    private final Path directory;
    public LocalMediaStorage(@Value("${app.media.local-directory}") String directory) { this.directory = Path.of(directory).toAbsolutePath().normalize(); }
    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(directory);
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase() : "";
        String filename = UUID.randomUUID() + extension;
        Files.copy(file.getInputStream(), directory.resolve(filename));
        return filename;
    }
    public Path getDirectory() { return directory; }
}
