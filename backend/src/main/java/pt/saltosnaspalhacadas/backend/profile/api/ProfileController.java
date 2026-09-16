package pt.saltosnaspalhacadas.backend.profile.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.profile.ProfileService;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    private final ProfileService profileService;
    private final ApiResponseLimits responseLimits;

    public ProfileController(ProfileService profileService, ApiResponseLimits responseLimits) {
        this.profileService = profileService;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    List<ProfileResponse> findAll() {
        return responseLimits.publicList(profileService.findActiveProfiles().stream().map(ProfileResponse::from));
    }

    @GetMapping("/{slug}")
    ProfileResponse findBySlug(@PathVariable String slug) {
        return ProfileResponse.from(profileService.findActiveProfile(slug));
    }
}
