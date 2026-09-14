package pt.saltosnaspalhacadas.backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class R2MediaStorageTests {
    private final S3Client s3 = mock(S3Client.class);
    private final R2MediaStorage storage = new R2MediaStorage(s3, new MediaFileValidator(), "public-bucket", "private-bucket");

    @Test
    void storesPrivateMediaInPrivateBucket() throws Exception {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().build());

        StoredMedia media = storage.storePrivate(new MockMultipartFile("file", "festa.png", "image/png", pngHeader()));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("private-bucket");
        assertThat(request.getValue().key()).isEqualTo(media.storageKey());
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
        assertThat(media.storageKey()).endsWith(".png");
    }

    @Test
    void uploadsR2WithFreshNonResettableStreamAfterMagicByteValidation() throws Exception {
        byte[] content = pngContent();
        NonResettableMultipartFile file = new NonResettableMultipartFile("file", "festa.png", "image/png", content);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            RequestBody requestBody = invocation.getArgument(1);

            assertThat(requestBody.optionalContentLength()).contains((long) content.length);
            assertThat(requestBody.contentType()).isEqualTo("image/png");
            assertThat(readFully(requestBody.contentStreamProvider().newStream())).isEqualTo(content);
            assertThat(readFully(requestBody.contentStreamProvider().newStream())).isEqualTo(content);

            return PutObjectResponse.builder().build();
        });

        storage.storePrivate(file);

        assertThat(file.streams()).hasSize(3);
        assertThat(file.streams().getFirst().bytesRead()).isEqualTo(16);
        assertThat(file.streams().getFirst().closed()).isTrue();
        assertThat(file.streams()).allSatisfy(stream -> {
            assertThat(stream.markCalls()).isZero();
            assertThat(stream.resetCalls()).isZero();
        });
    }

    @Test
    void readsPrivateMediaWithoutLoadingItAllIntoMemory() throws Exception {
        byte[] body = "media-body".getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder()
                        .contentType("image/png")
                        .contentLength((long) body.length)
                        .build(),
                AbortableInputStream.create(new ByteArrayInputStream(body)));
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(response);

        MediaDownload download = storage.readPrivate("123e4567-e89b-12d3-a456-426614174000.png");

        assertThat(download.resource()).isNotNull();
        assertThat(download.contentType()).isEqualTo("image/png");
        assertThat(download.contentLength()).isEqualTo((long) body.length);
    }

    @Test
    void publishesPrivateMediaByCopyingBeforeDeleting() throws Exception {
        when(s3.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        storage.publishPrivate("123e4567-e89b-12d3-a456-426614174000.png");

        ArgumentCaptor<CopyObjectRequest> copy = ArgumentCaptor.forClass(CopyObjectRequest.class);
        InOrder order = inOrder(s3);
        order.verify(s3).copyObject(copy.capture());
        order.verify(s3).deleteObject(any(DeleteObjectRequest.class));
        assertThat(copy.getValue().sourceBucket()).isEqualTo("private-bucket");
        assertThat(copy.getValue().sourceKey()).isEqualTo("123e4567-e89b-12d3-a456-426614174000.png");
        assertThat(copy.getValue().destinationBucket()).isEqualTo("public-bucket");
        assertThat(copy.getValue().destinationKey()).isEqualTo("123e4567-e89b-12d3-a456-426614174000.png");
        assertThat(copy.getValue().metadataDirective()).isEqualTo(MetadataDirective.COPY);
    }

    @Test
    void doesNotDeletePrivateMediaIfCopyFails() {
        when(s3.copyObject(any(CopyObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(500).message("copy failed").build());
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertThatThrownBy(() -> storage.publishPrivate("123e4567-e89b-12d3-a456-426614174000.png"))
                .isInstanceOf(S3Exception.class);

        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
        assertThat(storage.privateExists("123e4567-e89b-12d3-a456-426614174000.png")).isTrue();
    }

    @Test
    void privateExistsUsesHeadObjectAndTreats404AsMissing() {
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertThat(storage.privateExists("123e4567-e89b-12d3-a456-426614174000.png")).isTrue();

        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(404).message("not found").build());

        assertThat(storage.privateExists("123e4567-e89b-12d3-a456-426614174000.png")).isFalse();
    }

    @Test
    void rejectsInvalidStorageKeysBeforeCallingR2() {
        assertThatThrownBy(() -> storage.readPrivate("../escape.png"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(s3, never()).getObject(any(GetObjectRequest.class));
    }

    private static byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
    }

    private static byte[] pngContent() {
        byte[] header = pngHeader();
        byte[] content = new byte[32];
        System.arraycopy(header, 0, content, 0, header.length);
        for (int index = header.length; index < content.length; index++) {
            content[index] = (byte) index;
        }
        return content;
    }

    private static byte[] readFully(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static final class NonResettableMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;
        private final List<TrackingInputStream> streams = new ArrayList<>();

        private NonResettableMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            throw new AssertionError("Upload code must stream MultipartFile content");
        }

        @Override
        public InputStream getInputStream() {
            TrackingInputStream stream = new TrackingInputStream(content);
            streams.add(stream);
            return stream;
        }

        @Override
        public void transferTo(File dest) {
            throw new AssertionError("Upload code must stream MultipartFile content");
        }

        List<TrackingInputStream> streams() {
            return streams;
        }
    }

    private static final class TrackingInputStream extends InputStream {
        private final byte[] content;
        private int position;
        private int bytesRead;
        private boolean closed;
        private int markCalls;
        private int resetCalls;

        private TrackingInputStream(byte[] content) {
            this.content = content;
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            if (position >= content.length) {
                return -1;
            }
            bytesRead++;
            return content[position++] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            ensureOpen();
            if (position >= content.length) {
                return -1;
            }
            int count = Math.min(length, content.length - position);
            System.arraycopy(content, position, buffer, offset, count);
            position += count;
            bytesRead += count;
            return count;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
            markCalls++;
        }

        @Override
        public synchronized void reset() throws IOException {
            resetCalls++;
            throw new IOException("reset is not supported");
        }

        @Override
        public void close() {
            closed = true;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("stream is already closed");
            }
        }

        int bytesRead() {
            return bytesRead;
        }

        boolean closed() {
            return closed;
        }

        int markCalls() {
            return markCalls;
        }

        int resetCalls() {
            return resetCalls;
        }
    }
}
