package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MediaFileValidator {
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 30 * 1024 * 1024;
    private static final Map<String, AllowedMedia> ALLOWED_MEDIA = Map.of(
            "image/jpeg", new AllowedMedia(".jpg", MAX_IMAGE_SIZE, MediaFileValidator::isJpeg),
            "image/png", new AllowedMedia(".png", MAX_IMAGE_SIZE, MediaFileValidator::isPng),
            "image/webp", new AllowedMedia(".webp", MAX_IMAGE_SIZE, MediaFileValidator::isWebp),
            "image/gif", new AllowedMedia(".gif", MAX_IMAGE_SIZE, MediaFileValidator::isGif),
            "video/mp4", new AllowedMedia(".mp4", MAX_VIDEO_SIZE, MediaFileValidator::isMp4Like),
            "video/webm", new AllowedMedia(".webm", MAX_VIDEO_SIZE, MediaFileValidator::isWebm),
            "video/quicktime", new AllowedMedia(".mov", MAX_VIDEO_SIZE, MediaFileValidator::isMp4Like));

    public StoredMedia validate(MultipartFile file) throws IOException {
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

        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(16);
        }
        if (!media.signature().matches(header)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O conteúdo do ficheiro não corresponde ao tipo indicado");
        }

        return new StoredMedia(UUID.randomUUID() + media.extension(), contentType);
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

    private record AllowedMedia(String extension, long maxSize, MediaSignature signature) {
    }

    @FunctionalInterface
    private interface MediaSignature {
        boolean matches(byte[] header);
    }
}
