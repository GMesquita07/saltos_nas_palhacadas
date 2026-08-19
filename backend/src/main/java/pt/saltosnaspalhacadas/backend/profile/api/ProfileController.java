package pt.saltosnaspalhacadas.backend.profile.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.saltosnaspalhacadas.backend.profile.ProfileService;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    List<ProfileResponse> findAll() {
        return profileService.findActiveProfiles().stream().map(ProfileResponse::from).toList();
    }

    @GetMapping("/{slug}")
    ProfileResponse findBySlug(@PathVariable String slug) {
        return ProfileResponse.from(profileService.findActiveProfile(slug));
    }
}
