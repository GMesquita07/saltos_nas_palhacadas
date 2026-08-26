package pt.saltosnaspalhacadas.backend.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentStatus;
import pt.saltosnaspalhacadas.backend.clientcontent.api.ClientContentPostResponse;

@RestController
@RequestMapping("/api/v1/admin/client-posts")
public class AdminClientContentController {

    private final ClientContentPostRepository posts;

    public AdminClientContentController(ClientContentPostRepository posts) {
        this.posts = posts;
    }

    @GetMapping
    List<ClientContentPostResponse> findAll() {
        return posts.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(ClientContentPostResponse::adminFrom)
                .toList();
    }

    @PutMapping("/{id}")
    ClientContentPostResponse moderate(@PathVariable Long id, @Valid @RequestBody ModerateClientContentRequest request) {
        ClientContentPost post = posts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));

        post.moderate(request.status(), request.adminMessage());

        return ClientContentPostResponse.adminFrom(posts.save(post));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        ClientContentPost post = posts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));
        posts.delete(post);
    }

    record ModerateClientContentRequest(
            @NotNull(message = "Escolhe o estado da publicação")
            ClientContentStatus status,
            @Size(max = 600, message = "A mensagem do admin pode ter no máximo 600 caracteres")
            String adminMessage) {
    }
}
