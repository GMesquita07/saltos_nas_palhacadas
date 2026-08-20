package pt.saltosnaspalhacadas.backend.admin;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItem;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItemRepository;
import pt.saltosnaspalhacadas.backend.portfolio.api.PortfolioItemResponse;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileNotFoundException;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.profile.api.ProfileResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPortfolioController {

    private final ProfileRepository profiles;
    private final PortfolioItemRepository items;

    public AdminPortfolioController(ProfileRepository profiles, PortfolioItemRepository items) {
        this.profiles = profiles;
        this.items = items;
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    ProfileResponse createProfile(@Valid @RequestBody CreateProfileRequest request) {
        if (profiles.findBySlugAndActiveTrue(request.slug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um perfil com este slug");
        }

        Profile profile = new Profile(
                request.slug(),
                request.name(),
                request.role(),
                request.description(),
                emptyToNull(request.profileImageUrl()),
                defaultImagePosition(request.profileImagePosition()));

        return ProfileResponse.from(profiles.save(profile));
    }

    @PutMapping("/profiles/{slug}")
    ProfileResponse updateProfile(@PathVariable String slug, @Valid @RequestBody UpdateProfileRequest request) {
        Profile profile = profiles.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));

        profile.update(
                request.name(),
                request.role(),
                request.description(),
                emptyToNull(request.profileImageUrl()),
                defaultImagePosition(request.profileImagePosition()));

        return ProfileResponse.from(profiles.save(profile));
    }

    @PostMapping("/profiles/{slug}/portfolio")
    @ResponseStatus(HttpStatus.CREATED)
    PortfolioItemResponse createPortfolioItem(
            @PathVariable String slug,
            @Valid @RequestBody CreatePortfolioItemRequest request) {
        Profile profile = profiles.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));

        PortfolioItem item = new PortfolioItem(
                profile,
                request.type(),
                request.title(),
                request.location(),
                request.eventDate(),
                request.mediaUrl(),
                emptyToNull(request.thumbnailUrl()),
                0,
                isPublishedByDefault(request.published()));

        return PortfolioItemResponse.from(items.save(item));
    }

    @PutMapping("/profiles/{slug}/portfolio/{itemId}")
    PortfolioItemResponse updatePortfolioItem(
            @PathVariable String slug,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdatePortfolioItemRequest request) {
        PortfolioItem item = items.findByIdAndProfileSlug(itemId, slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));

        item.update(
                request.type(),
                request.title(),
                request.location(),
                request.eventDate(),
                request.mediaUrl(),
                emptyToNull(request.thumbnailUrl()),
                request.published() == null ? item.isPublished() : request.published());

        return PortfolioItemResponse.from(items.save(item));
    }

    @DeleteMapping("/profiles/{slug}/portfolio/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePortfolioItem(@PathVariable String slug, @PathVariable Long itemId) {
        PortfolioItem item = items.findByIdAndProfileSlug(itemId, slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));
        items.delete(item);
    }

    @DeleteMapping("/profiles/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProfile(@PathVariable String slug) {
        Profile profile = profiles.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));
        profiles.delete(profile);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultImagePosition(String value) {
        return value == null || value.isBlank() ? "50% 50%" : value;
    }

    private static boolean isPublishedByDefault(Boolean published) {
        return published == null || published;
    }

    record CreateProfileRequest(
            @NotBlank(message = "O slug é obrigatório")
            @Size(max = 100, message = "O slug pode ter no máximo 100 caracteres")
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "O slug só pode usar minúsculas, números e hífen entre palavras")
            String slug,
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String name,
            @NotBlank(message = "A função é obrigatória")
            @Size(max = 120, message = "A função pode ter no máximo 120 caracteres")
            String role,
            @NotBlank(message = "A descrição é obrigatória")
            @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres")
            String description,
            @Size(max = 2048, message = "O URL da imagem é demasiado longo")
            String profileImageUrl,
            @Pattern(regexp = "(?:100|[0-9]{1,2})% (?:100|[0-9]{1,2})%", message = "A posição da imagem é inválida")
            String profileImagePosition) {
    }

    record UpdateProfileRequest(
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String name,
            @NotBlank(message = "A função é obrigatória")
            @Size(max = 120, message = "A função pode ter no máximo 120 caracteres")
            String role,
            @NotBlank(message = "A descrição é obrigatória")
            @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres")
            String description,
            @Size(max = 2048, message = "O URL da imagem é demasiado longo")
            String profileImageUrl,
            @Pattern(regexp = "(?:100|[0-9]{1,2})% (?:100|[0-9]{1,2})%", message = "A posição da imagem é inválida")
            String profileImagePosition) {
    }

    record CreatePortfolioItemRequest(
            @NotNull(message = "Envia uma fotografia ou vídeo")
            MediaType type,
            @NotBlank(message = "O título é obrigatório")
            @Size(max = 180, message = "O título pode ter no máximo 180 caracteres")
            String title,
            @NotBlank(message = "O local é obrigatório")
            @Size(max = 180, message = "O local pode ter no máximo 180 caracteres")
            String location,
            @NotNull(message = "A data é obrigatória")
            LocalDate eventDate,
            @NotBlank(message = "Envia uma fotografia ou vídeo")
            @Size(max = 2048, message = "O URL do ficheiro é demasiado longo")
            String mediaUrl,
            @Size(max = 2048, message = "O URL da miniatura é demasiado longo")
            String thumbnailUrl,
            Boolean published) {
    }

    record UpdatePortfolioItemRequest(
            @NotNull(message = "O tipo de ficheiro é obrigatório")
            MediaType type,
            @NotBlank(message = "O título é obrigatório")
            @Size(max = 180, message = "O título pode ter no máximo 180 caracteres")
            String title,
            @NotBlank(message = "O local é obrigatório")
            @Size(max = 180, message = "O local pode ter no máximo 180 caracteres")
            String location,
            @NotNull(message = "A data é obrigatória")
            LocalDate eventDate,
            @NotBlank(message = "O ficheiro é obrigatório")
            @Size(max = 2048, message = "O URL do ficheiro é demasiado longo")
            String mediaUrl,
            @Size(max = 2048, message = "O URL da miniatura é demasiado longo")
            String thumbnailUrl,
            Boolean published) {
    }
}
