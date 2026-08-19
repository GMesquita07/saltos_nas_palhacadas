package pt.saltosnaspalhacadas.backend.admin;

import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.saltosnaspalhacadas.backend.portfolio.*;
import pt.saltosnaspalhacadas.backend.profile.*;
import pt.saltosnaspalhacadas.backend.profile.api.ProfileResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPortfolioController {
    private final ProfileRepository profiles; private final PortfolioItemRepository items;
    public AdminPortfolioController(ProfileRepository profiles, PortfolioItemRepository items) { this.profiles = profiles; this.items = items; }
    @PostMapping("/profiles") @ResponseStatus(HttpStatus.CREATED)
    ProfileResponse createProfile(@Valid @RequestBody CreateProfileRequest request) {
        if (profiles.findBySlugAndActiveTrue(request.slug()).isPresent()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "Slug já existe");
        return ProfileResponse.from(profiles.save(new Profile(request.slug(), request.name(), request.role(), request.description(), request.profileImageUrl())));
    }
    @PostMapping("/profiles/{slug}/portfolio") @ResponseStatus(HttpStatus.CREATED)
    void createPortfolioItem(@PathVariable String slug, @Valid @RequestBody CreatePortfolioItemRequest request) {
        Profile profile = profiles.findBySlugAndActiveTrue(slug).orElseThrow(() -> new ProfileNotFoundException(slug));
        items.save(new PortfolioItem(profile, request.type(), request.title(), request.location(), request.eventDate(), request.mediaUrl(), request.thumbnailUrl(), request.displayOrder(), request.published()));
    }
    @DeleteMapping("/profiles/{slug}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProfile(@PathVariable String slug) {
        Profile profile = profiles.findBySlugAndActiveTrue(slug).orElseThrow(() -> new ProfileNotFoundException(slug));
        profiles.delete(profile);
    }
    record CreateProfileRequest(@NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug, @NotBlank @Size(max=120) String name, @NotBlank @Size(max=120) String role, @NotBlank @Size(max=500) String description, @Size(max=2048) String profileImageUrl) { }
    record CreatePortfolioItemRequest(@NotNull MediaType type, @NotBlank @Size(max=180) String title, @NotBlank @Size(max=180) String location, @NotNull LocalDate eventDate, @NotBlank @Size(max=2048) String mediaUrl, @Size(max=2048) String thumbnailUrl, @Min(0) int displayOrder, boolean published) { }
}
