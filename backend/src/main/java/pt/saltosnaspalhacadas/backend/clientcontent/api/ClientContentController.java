package pt.saltosnaspalhacadas.backend.clientcontent.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentStatus;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileNotFoundException;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@RestController
@RequestMapping("/api/v1/client-posts")
public class ClientContentController {

    private final ClientContentPostRepository posts;
    private final ProfileRepository profiles;
    private final AppUserRepository users;
    private final LocalMediaStorage storage;

    public ClientContentController(ClientContentPostRepository posts, ProfileRepository profiles, AppUserRepository users, LocalMediaStorage storage) {
        this.posts = posts;
        this.profiles = profiles;
        this.users = users;
        this.storage = storage;
    }

    @GetMapping
    List<ClientContentPostResponse> findPublished(@RequestParam(required = false) MediaType type) {
        List<ClientContentPost> result = type == null
                ? posts.findAllByStatusOrderByEventDateDescIdDesc(ClientContentStatus.APPROVED)
                : posts.findAllByStatusAndMediaTypeOrderByEventDateDescIdDesc(ClientContentStatus.APPROVED, type);

        return result.stream()
                .map(ClientContentPostResponse::publicFrom)
                .toList();
    }

    @GetMapping("/mine")
    List<ClientContentPostResponse> findMine(Authentication authentication) {
        AppUser user = findCurrentUser(authentication);

        return posts.findAllByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .stream()
                .map(ClientContentPostResponse::mineFrom)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClientContentPostResponse submit(Authentication authentication, @Valid @RequestBody SubmitClientContentRequest request) {
        AppUser user = findCurrentUser(authentication);
        if (request.eventDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data do evento não pode ser no futuro");
        }
        Profile profile = profiles.findBySlugAndActiveTrue(request.profileSlug().trim())
                .orElseThrow(() -> new ProfileNotFoundException(request.profileSlug()));

        ClientContentPost post = new ClientContentPost(
                user,
                profile,
                request.type(),
                request.title().trim(),
                request.location().trim(),
                request.eventDate(),
                request.caption().trim(),
                storage.requirePrivateFilename(request.mediaUrl(), "Envia o ficheiro através do upload do site antes de publicar"),
                request.thumbnailUrl() == null || request.thumbnailUrl().isBlank()
                        ? null
                        : storage.requirePrivateFilename(request.thumbnailUrl(), "Envia a miniatura através do upload do site"));

        return ClientContentPostResponse.mineFrom(posts.save(post));
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para publicar conteúdo");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }

    record SubmitClientContentRequest(
            @NotBlank(message = "Escolhe o artista do evento")
            String profileSlug,
            @NotNull(message = "Envia uma fotografia ou vídeo")
            MediaType type,
            @NotBlank(message = "O título é obrigatório")
            @Size(max = 180, message = "O título pode ter no máximo 180 caracteres")
            String title,
            @NotBlank(message = "O local é obrigatório")
            @Size(max = 180, message = "O local pode ter no máximo 180 caracteres")
            String location,
            @NotNull(message = "A data do evento é obrigatória")
            LocalDate eventDate,
            @NotBlank(message = "A legenda é obrigatória")
            @Size(max = 800, message = "A legenda pode ter no máximo 800 caracteres")
            String caption,
            @NotBlank(message = "Envia uma fotografia ou vídeo")
            @Size(max = 2048, message = "O URL do ficheiro é demasiado longo")
            String mediaUrl,
            @Size(max = 2048, message = "O URL da miniatura é demasiado longo")
            String thumbnailUrl) {
    }
}
