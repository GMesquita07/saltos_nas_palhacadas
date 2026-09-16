package pt.saltosnaspalhacadas.backend.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(name = "app.media.storage-provider", havingValue = "r2")
public class R2MediaStorage implements MediaStorage {
    private final S3Client s3;
    private final MediaFileValidator validator;
    private final String publicBucket;
    private final String privateBucket;

    public R2MediaStorage(
            S3Client s3,
            MediaFileValidator validator,
            @Value("${app.media.r2.public-bucket}") String publicBucket,
            @Value("${app.media.r2.private-bucket}") String privateBucket) {
        this.s3 = s3;
        this.validator = validator;
        this.publicBucket = required(publicBucket, "R2_PUBLIC_BUCKET é obrigatório quando MEDIA_STORAGE_PROVIDER=r2");
        this.privateBucket = required(privateBucket, "R2_PRIVATE_BUCKET é obrigatório quando MEDIA_STORAGE_PROVIDER=r2");
        if (this.publicBucket.equals(this.privateBucket)) {
            throw new IllegalStateException("R2_PUBLIC_BUCKET e R2_PRIVATE_BUCKET devem ser buckets diferentes");
        }
    }

    @Override
    public StoredMedia storePublic(MultipartFile file) throws IOException {
        return store(file, publicBucket);
    }

    @Override
    public StoredMedia storePrivate(MultipartFile file) throws IOException {
        return store(file, privateBucket);
    }

    @Override
    public boolean privateExists(String storageKey) {
        if (!MediaStorageKeys.isValid(storageKey)) {
            return false;
        }
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(privateBucket)
                    .key(storageKey)
                    .build());
            return true;
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public MediaDownload readPublic(String storageKey) throws IOException {
        return read(publicBucket, storageKey, "Ficheiro não encontrado");
    }

    @Override
    public MediaDownload readPrivate(String storageKey) throws IOException {
        return read(privateBucket, storageKey, "Ficheiro não encontrado");
    }

    @Override
    public void publishPrivate(String storageKey) throws IOException {
        requireValidStorageKey(storageKey);
        try {
            s3.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(privateBucket)
                    .sourceKey(storageKey)
                    .destinationBucket(publicBucket)
                    .destinationKey(storageKey)
                    .metadataDirective(MetadataDirective.COPY)
                    .build());
            deletePrivate(storageKey);
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                return;
            }
            throw exception;
        }
    }

    @Override
    public void deletePrivate(String storageKey) throws IOException {
        delete(privateBucket, storageKey);
    }

    @Override
    public void deletePublic(String storageKey) throws IOException {
        delete(publicBucket, storageKey);
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
        if (!privateExists(storageKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ficheiro enviado já não está disponível");
        }
        return storageKey;
    }

    private StoredMedia store(MultipartFile file, String bucket) throws IOException {
        StoredMedia media = validator.validate(file);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(media.storageKey())
                .contentType(media.contentType())
                .contentLength(file.getSize())
                .build();

        try {
            s3.putObject(request, RequestBody.fromContentProvider(contentStreamProvider(file), file.getSize(), media.contentType()));
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }

        return media;
    }

    private static ContentStreamProvider contentStreamProvider(MultipartFile file) {
        return () -> {
            try {
                return file.getInputStream();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        };
    }

    private MediaDownload read(String bucket, String storageKey, String notFoundMessage) {
        requireValidStorageKey(storageKey);
        try {
            ResponseInputStream<GetObjectResponse> object = s3.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
            GetObjectResponse response = object.response();
            return new MediaDownload(
                    storageKey,
                    new InputStreamResource(object),
                    response.contentType(),
                    response.contentLength());
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage, exception);
            }
            throw exception;
        }
    }

    private void delete(String bucket, String storageKey) {
        requireValidStorageKey(storageKey);
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
        } catch (S3Exception exception) {
            if (!isNotFound(exception)) {
                throw exception;
            }
        }
    }

    private static void requireValidStorageKey(String storageKey) {
        if (!MediaStorageKeys.isValid(storageKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ficheiro não encontrado");
        }
    }

    private static boolean isNotFound(S3Exception exception) {
        return exception.statusCode() == HttpStatus.NOT_FOUND.value();
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}
