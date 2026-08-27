package pt.saltosnaspalhacadas.backend.clientcontent.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPublicIdentity;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentStatus;
import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileNotFoundException;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@RestController
@RequestMapping("/api/v1/client-posts")
public class ClientContentController {
    private static final String CONSENT_VERSION = "client-content-v1-2026-08-27";
    private static final Pattern PUBLIC_NAME_FORBIDDEN_DETAILS = Pattern.compile(".*(@|\\d{7,}).*");

    private final ClientContentPostRepository posts;
    private final ProfileRepository profiles;
    private final AppUserRepository users;
    private final ClientContentMediaService mediaService;

    public ClientContentController(ClientContentPostRepository posts, ProfileRepository profiles, AppUserRepository users, ClientContentMediaService mediaService) {
        this.posts = posts;
        this.profiles = profiles;
        this.users = users;
        this.mediaService = mediaService;
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
    @Transactional
    ClientContentPostResponse submit(Authentication authentication, @Valid @RequestBody SubmitClientContentRequest request) {
        AppUser user = findCurrentUser(authentication);
        if (request.eventDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data do evento não pode ser no futuro");
        }
        Profile profile = profiles.findBySlugAndActiveTrue(request.profileSlug().trim())
                .orElseThrow(() -> new ProfileNotFoundException(request.profileSlug()));

        ManagedMedia mainMedia = mediaService.attachOwnedPendingMedia(
                request.mediaId(),
                user,
                request.type(),
                "Envia o ficheiro através do upload do site antes de publicar");
        ManagedMedia thumbnailMedia = request.thumbnailId() == null ? null : mediaService.attachOwnedPendingMedia(
                request.thumbnailId(),
                user,
                MediaType.PHOTO,
                "Envia a miniatura através do upload do site");

        ClientContentPost post = new ClientContentPost(
                user,
                profile,
                request.type(),
                request.title().trim(),
                request.location().trim(),
                request.eventDate(),
                request.caption().trim(),
                mediaUrl(LocalMediaStorage.PRIVATE_MEDIA_PATH, mainMedia.getStorageKey()),
                thumbnailMedia == null ? null : mediaUrl(LocalMediaStorage.PRIVATE_MEDIA_PATH, thumbnailMedia.getStorageKey()),
                mainMedia,
                thumbnailMedia,
                publicDisplayName(user, request),
                Boolean.TRUE.equals(request.showLocation()),
                Boolean.TRUE.equals(request.showEventDate()),
                CONSENT_VERSION,
                Instant.now());

        return ClientContentPostResponse.mineFrom(posts.save(post));
    }

    private AppUser findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para publicar conteúdo");
        }

        return users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));
    }

    private static String mediaUrl(String prefix, String filename) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(prefix)
                .path(filename)
                .toUriString();
    }

    private static String publicDisplayName(AppUser user, SubmitClientContentRequest request) {
        ClientContentPublicIdentity identity = request.publicIdentity() == null
                ? ClientContentPublicIdentity.ANONYMOUS
                : request.publicIdentity();

        return switch (identity) {
            case ANONYMOUS -> "Cliente";
            case USERNAME -> usernameDisplayName(user);
            case CUSTOM -> customDisplayName(request.customDisplayName());
        };
    }

    private static String usernameDisplayName(AppUser user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Define um nome de utilizador na Conta antes de aparecer como @username");
        }
        return "@" + user.getUsername();
    }

    private static String customDisplayName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica o nome público a apresentar");
        }
        if (PUBLIC_NAME_FORBIDDEN_DETAILS.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome público não deve incluir email ou contacto telefónico");
        }
        return normalized;
    }

    record SubmitClientContentRequest(
            @NotBlank(message = "Escolhe o artista do evento")
            String profileSlug,
            @NotNull(message = "Envia uma fotografia ou vídeo")
            MediaType type,
            @NotNull(message = "Envia uma fotografia ou vídeo")
            UUID mediaId,
            UUID thumbnailId,
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
            ClientContentPublicIdentity publicIdentity,
            @Size(max = 80, message = "O nome público pode ter no máximo 80 caracteres")
            String customDisplayName,
            Boolean showLocation,
            Boolean showEventDate,
            @NotNull(message = "Confirma que tens autorização para publicar este conteúdo")
            @AssertTrue(message = "Confirma que tens autorização para publicar este conteúdo")
            Boolean consentToPublish,
            String mediaUrl,
            String thumbnailUrl) {
    }
}
