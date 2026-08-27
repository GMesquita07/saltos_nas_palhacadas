package pt.saltosnaspalhacadas.backend.admin;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentStatus;
import pt.saltosnaspalhacadas.backend.clientcontent.api.ClientContentPostResponse;
import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;

@RestController
@RequestMapping("/api/v1/admin/client-posts")
public class AdminClientContentController {

    private final ClientContentPostRepository posts;
    private final LocalMediaStorage storage;
    private final ClientContentMediaService mediaService;

    public AdminClientContentController(ClientContentPostRepository posts, LocalMediaStorage storage, ClientContentMediaService mediaService) {
        this.posts = posts;
        this.storage = storage;
        this.mediaService = mediaService;
    }

    @GetMapping
    List<ClientContentPostResponse> findAll() {
        return posts.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(ClientContentPostResponse::adminFrom)
                .toList();
    }

    @PutMapping("/{id}")
    @Transactional(rollbackFor = IOException.class)
    ResponseEntity<ClientContentPostResponse> moderate(@PathVariable Long id, @Valid @RequestBody ModerateClientContentRequest request) throws IOException {
        ClientContentPost post = posts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));

        if (request.status() == ClientContentStatus.REJECTED) {
            deleteMedia(post);
            posts.delete(post);
            return ResponseEntity.noContent().build();
        }

        if (request.status() == ClientContentStatus.APPROVED) {
            post.updateMediaUrls(
                    publishMedia(post.getMediaObject(), post.getMediaUrl()),
                    publishMedia(post.getThumbnailObject(), post.getThumbnailUrl()));
        }

        post.moderate(request.status(), request.adminMessage());

        return ResponseEntity.ok(ClientContentPostResponse.adminFrom(posts.save(post)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional(rollbackFor = IOException.class)
    void delete(@PathVariable Long id) throws IOException {
        ClientContentPost post = posts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));
        deleteMedia(post);
        posts.delete(post);
    }

    private String publishMedia(ManagedMedia media, String fallbackUrl) throws IOException {
        if (media == null) {
            return promoteIfPrivate(fallbackUrl);
        }
        return publicUrl(mediaService.publish(media));
    }

    private String promoteIfPrivate(String url) throws IOException {
        if (url == null || url.isBlank()) {
            return null;
        }

        String filename = storage.privateFilenameFromUrl(url).orElse(null);
        if (filename == null) {
            return url;
        }

        storage.publishPrivate(filename);
        return publicUrl(LocalMediaStorage.PUBLIC_MEDIA_PATH + filename);
    }

    private String publicUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path)
                .toUriString();
    }

    private void deleteMedia(ClientContentPost post) throws IOException {
        if (post.getMediaObject() != null) {
            mediaService.delete(post.getMediaObject());
        } else {
            storage.deleteManagedUrl(post.getMediaUrl());
        }

        if (post.getThumbnailObject() != null) {
            mediaService.delete(post.getThumbnailObject());
        } else {
            storage.deleteManagedUrl(post.getThumbnailUrl());
        }
    }

    record ModerateClientContentRequest(
            @NotNull(message = "Escolhe o estado da publicação")
            ClientContentStatus status,
            @Size(max = 600, message = "A mensagem do admin pode ter no máximo 600 caracteres")
            String adminMessage) {
    }
}
