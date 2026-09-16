package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "app.media.storage-provider", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorage implements MediaStorage {
    private final MediaFileValidator validator;
    private final Path publicDirectory;
    private final Path privateDirectory;

    public LocalMediaStorage(
            MediaFileValidator validator,
            @Value("${app.media.local-directory}") String directory,
            @Value("${app.media.private-local-directory:}") String privateDirectory) {
        this.validator = validator;
        this.publicDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.privateDirectory = privateDirectory == null || privateDirectory.isBlank()
                ? defaultPrivateDirectory(this.publicDirectory)
                : Path.of(privateDirectory).toAbsolutePath().normalize();
    }

    @Override
    public StoredMedia storePublic(MultipartFile file) throws IOException {
        return store(file, publicDirectory);
    }

    @Override
    public StoredMedia storePrivate(MultipartFile file) throws IOException {
        return store(file, privateDirectory);
    }

    @Override
    public boolean privateExists(String storageKey) {
        if (!MediaStorageKeys.isValid(storageKey)) {
            return false;
        }
        return Files.exists(resolveSafe(privateDirectory, storageKey, HttpStatus.BAD_REQUEST));
    }

    @Override
    public MediaDownload readPublic(String storageKey) throws IOException {
        return read(publicDirectory, storageKey, "Ficheiro não encontrado");
    }

    @Override
    public MediaDownload readPrivate(String storageKey) throws IOException {
        return read(privateDirectory, storageKey, "Ficheiro não encontrado");
    }

    @Override
    public void publishPrivate(String storageKey) throws IOException {
        Path source = resolveSafe(privateDirectory, storageKey, HttpStatus.BAD_REQUEST);
        if (!Files.exists(source)) {
            return;
        }
        Files.createDirectories(publicDirectory);
        Files.move(source, resolveSafe(publicDirectory, storageKey, HttpStatus.BAD_REQUEST), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void deletePrivate(String storageKey) throws IOException {
        Files.deleteIfExists(resolveSafe(privateDirectory, storageKey, HttpStatus.BAD_REQUEST));
    }

    @Override
    public void deletePublic(String storageKey) throws IOException {
        Files.deleteIfExists(resolveSafe(publicDirectory, storageKey, HttpStatus.BAD_REQUEST));
    }

    @Override
    public void deleteManagedUrl(String url) throws IOException {
        Optional<String> privateKey = privateKeyFromUrl(url);
        if (privateKey.isPresent()) {
            deletePrivate(privateKey.get());
            return;
        }

        Optional<String> publicKey = publicKeyFromUrl(url);
        if (publicKey.isPresent()) {
            deletePublic(publicKey.get());
        }
    }

    @Override
    public boolean isValidStorageKey(String storageKey) {
        return MediaStorageKeys.isValid(storageKey);
    }

    @Override
    public Optional<String> privateKeyFromUrl(String url) {
        return MediaStorageKeys.fromPathOrUrl(url, MediaPaths.PRIVATE_MEDIA_PATH);
    }

    @Override
    public Optional<String> publicKeyFromUrl(String url) {
        return MediaStorageKeys.fromPathOrUrl(url, MediaPaths.PUBLIC_MEDIA_PATH);
    }

    @Override
    public String requirePrivateKey(String url, String message) {
        String storageKey = privateKeyFromUrl(url)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
        if (!Files.exists(resolveSafe(privateDirectory, storageKey, HttpStatus.BAD_REQUEST))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ficheiro enviado já não está disponível");
        }
        return storageKey;
    }

    private StoredMedia store(MultipartFile file, Path directory) throws IOException {
        StoredMedia media = validator.validate(file);
        Files.createDirectories(directory);
        Path target = resolveSafe(directory, media.storageKey(), HttpStatus.BAD_REQUEST);
        try (var input = file.getInputStream()) {
            Files.copy(input, target);
        }
        return media;
    }

    private MediaDownload read(Path directory, String storageKey, String notFoundMessage) throws IOException {
        Path file = resolveSafe(directory, storageKey, HttpStatus.NOT_FOUND);
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage);
        }
        return new MediaDownload(
                storageKey,
                new FileSystemResource(file),
                MediaTypeFactory.getMediaType(file.getFileName().toString()).map(MediaType::toString).orElse(null),
                Files.size(file));
    }

    private static Path resolveSafe(Path directory, String storageKey, HttpStatus invalidStatus) {
        if (!MediaStorageKeys.isValid(storageKey)) {
            throw new ResponseStatusException(invalidStatus, "Nome de ficheiro inválido");
        }
        Path target = directory.resolve(storageKey).normalize();
        if (!target.startsWith(directory)) {
            throw new ResponseStatusException(invalidStatus, "Nome de ficheiro inválido");
        }
        return target;
    }

    private static Path defaultPrivateDirectory(Path publicDirectory) {
        Path filename = publicDirectory.getFileName();
        String privateName = (filename == null ? "uploads" : filename.toString()) + "_private";
        Path parent = publicDirectory.getParent();
        return (parent == null ? Path.of(privateName) : parent.resolve(privateName)).toAbsolutePath().normalize();
    }
}
