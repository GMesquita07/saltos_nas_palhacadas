package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalMediaStorage {
    public static final String PUBLIC_MEDIA_PATH = "/api/v1/media/";
    public static final String PRIVATE_MEDIA_PATH = "/api/v1/private-media/";
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;
    private static final Pattern SAFE_FILENAME = Pattern.compile("[0-9a-fA-F-]{36}\\.(jpg|png|webp|gif|mp4|webm|mov)");
    private static final Map<String, AllowedMedia> ALLOWED_MEDIA = Map.of(
            "image/jpeg", new AllowedMedia(".jpg", MAX_IMAGE_SIZE, LocalMediaStorage::isJpeg),
            "image/png", new AllowedMedia(".png", MAX_IMAGE_SIZE, LocalMediaStorage::isPng),
            "image/webp", new AllowedMedia(".webp", MAX_IMAGE_SIZE, LocalMediaStorage::isWebp),
            "image/gif", new AllowedMedia(".gif", MAX_IMAGE_SIZE, LocalMediaStorage::isGif),
            "video/mp4", new AllowedMedia(".mp4", MAX_VIDEO_SIZE, LocalMediaStorage::isMp4Like),
            "video/webm", new AllowedMedia(".webm", MAX_VIDEO_SIZE, LocalMediaStorage::isWebm),
            "video/quicktime", new AllowedMedia(".mov", MAX_VIDEO_SIZE, LocalMediaStorage::isMp4Like));

    private final Path publicDirectory;
    private final Path privateDirectory;

    public LocalMediaStorage(
            @Value("${app.media.local-directory}") String directory,
            @Value("${app.media.private-local-directory:}") String privateDirectory) {
        this.publicDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.privateDirectory = privateDirectory == null || privateDirectory.isBlank()
                ? defaultPrivateDirectory(this.publicDirectory)
                : Path.of(privateDirectory).toAbsolutePath().normalize();
    }

    public StoredMedia store(MultipartFile file) throws IOException {
        return store(file, publicDirectory);
    }

    public StoredMedia storePrivate(MultipartFile file) throws IOException {
        return store(file, privateDirectory);
    }

    public Path getDirectory() { return publicDirectory; }

    public Path privatePath(String filename) {
        return resolveSafe(privateDirectory, filename);
    }

    public Optional<String> privateFilenameFromUrl(String url) {
        return filenameFromPathOrUrl(url, PRIVATE_MEDIA_PATH);
    }

    public Optional<String> publicFilenameFromUrl(String url) {
        return filenameFromPathOrUrl(url, PUBLIC_MEDIA_PATH);
    }

    public String requirePrivateFilename(String url, String message) {
        String filename = privateFilenameFromUrl(url)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
        if (!Files.exists(privatePath(filename))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ficheiro enviado já não está disponível");
        }
        return url.trim();
    }

    public void publishPrivate(String filename) throws IOException {
        Path source = privatePath(filename);
        if (!Files.exists(source)) {
            return;
        }
        Files.createDirectories(publicDirectory);
        Files.move(source, resolveSafe(publicDirectory, filename), StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteManagedUrl(String url) throws IOException {
        Optional<String> privateFilename = privateFilenameFromUrl(url);
        if (privateFilename.isPresent()) {
            Files.deleteIfExists(privatePath(privateFilename.get()));
            return;
        }

        Optional<String> publicFilename = publicFilenameFromUrl(url);
        if (publicFilename.isPresent()) {
            Files.deleteIfExists(resolveSafe(publicDirectory, publicFilename.get()));
        }
    }

    public boolean isSafeFilename(String filename) {
        return filename != null && SAFE_FILENAME.matcher(filename).matches();
    }

    private StoredMedia store(MultipartFile file, Path directory) throws IOException {
        AllowedMedia media = validate(file);
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + media.extension();
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de ficheiro inválido");
        }
        Files.copy(file.getInputStream(), target);
        return new StoredMedia(filename, normalizedContentType(file.getContentType()));
    }

    private static AllowedMedia validate(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleciona uma imagem ou vídeo válido");
        }

        String contentType = normalizedContentType(file.getContentType());
        AllowedMedia media = ALLOWED_MEDIA.get(contentType);
        if (media == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só são permitidos ficheiros JPG, PNG, WebP, GIF, MP4, WebM ou MOV");
        }

        if (file.getSize() > media.maxSize()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ficheiro demasiado grande");
        }

        byte[] header = file.getInputStream().readNBytes(16);
        if (!media.signature().matches(header)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O conteúdo do ficheiro não corresponde ao tipo indicado");
        }

        return media;
    }

    private static String normalizedContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separator = contentType.indexOf(';');
        String value = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isJpeg(byte[] header) {
        return header.length >= 3
                && header[0] == (byte) 0xff
                && header[1] == (byte) 0xd8
                && header[2] == (byte) 0xff;
    }

    private static boolean isPng(byte[] header) {
        return startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    }

    private static boolean isWebp(byte[] header) {
        return header.length >= 12
                && startsWith(header, new byte[] {0x52, 0x49, 0x46, 0x46})
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
    }

    private static boolean isGif(byte[] header) {
        return startsWith(header, new byte[] {0x47, 0x49, 0x46, 0x38, 0x37, 0x61})
                || startsWith(header, new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61});
    }

    private static boolean isMp4Like(byte[] header) {
        return header.length >= 12
                && header[4] == 0x66
                && header[5] == 0x74
                && header[6] == 0x79
                && header[7] == 0x70;
    }

    private static boolean isWebm(byte[] header) {
        return startsWith(header, new byte[] {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3});
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && Arrays.equals(Arrays.copyOf(value, prefix.length), prefix);
    }

    private static Optional<String> filenameFromPathOrUrl(String value, String prefix) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String path;
        String trimmed = value.trim();
        try {
            path = trimmed.startsWith("/") ? trimmed : URI.create(trimmed).getPath();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        if (path == null || !path.startsWith(prefix)) {
            return Optional.empty();
        }

        String filename = URLDecoder.decode(path.substring(prefix.length()), StandardCharsets.UTF_8);
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return Optional.empty();
        }
        return Optional.of(filename);
    }

    private static Path resolveSafe(Path directory, String filename) {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de ficheiro inválido");
        }
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de ficheiro inválido");
        }
        return target;
    }

    private static Path defaultPrivateDirectory(Path publicDirectory) {
        Path filename = publicDirectory.getFileName();
        String privateName = (filename == null ? "uploads" : filename.toString()) + "_private";
        Path parent = publicDirectory.getParent();
        return (parent == null ? Path.of(privateName) : parent.resolve(privateName)).toAbsolutePath().normalize();
    }

    public record StoredMedia(String filename, String contentType) {
    }

    private record AllowedMedia(String extension, long maxSize, MediaSignature signature) {
    }

    @FunctionalInterface
    private interface MediaSignature {
        boolean matches(byte[] header);
    }
}
