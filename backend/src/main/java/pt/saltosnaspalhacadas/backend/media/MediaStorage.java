package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorage {
    StoredMedia storePublic(MultipartFile file) throws IOException;

    StoredMedia storePrivate(MultipartFile file) throws IOException;

    boolean privateExists(String storageKey);

    MediaDownload readPublic(String storageKey) throws IOException;

    MediaDownload readPrivate(String storageKey) throws IOException;

    void publishPrivate(String storageKey) throws IOException;

    void deletePrivate(String storageKey) throws IOException;

    void deletePublic(String storageKey) throws IOException;

    void deleteManagedUrl(String url) throws IOException;

    boolean isValidStorageKey(String storageKey);

    Optional<String> privateKeyFromUrl(String url);

    Optional<String> publicKeyFromUrl(String url);

    String requirePrivateKey(String url, String message);
}
