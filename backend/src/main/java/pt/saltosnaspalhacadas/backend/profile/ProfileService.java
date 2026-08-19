package pt.saltosnaspalhacadas.backend.profile;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public List<Profile> findActiveProfiles() {
        return profileRepository.findAllByActiveTrueOrderByNameAsc();
    }

    public Profile findActiveProfile(String slug) {
        return profileRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));
    }
}
