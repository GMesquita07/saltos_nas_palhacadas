package pt.saltosnaspalhacadas.backend.profile.api;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.profile.ProfileService;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    private static final CacheControl PUBLIC_PROFILE_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final ProfileService profileService;
    private final ApiResponseLimits responseLimits;

    public ProfileController(ProfileService profileService, ApiResponseLimits responseLimits) {
        this.profileService = profileService;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    ResponseEntity<List<ProfileResponse>> findAll() {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_PROFILE_CACHE)
                .body(responseLimits.publicList(profileService.findActiveProfiles().stream().map(ProfileResponse::from)));
    }

    @GetMapping("/{slug}")
    ResponseEntity<ProfileResponse> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_PROFILE_CACHE)
                .body(ProfileResponse.from(profileService.findActiveProfile(slug)));
    }
}
