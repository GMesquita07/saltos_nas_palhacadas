package pt.saltosnaspalhacadas.backend.profile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.saltosnaspalhacadas.backend.booking.BookingRepository;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final BookingRepository bookingRepository;

    public ProfileService(
            ProfileRepository profileRepository,
            BookingRepository bookingRepository) {
        this.profileRepository = profileRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Profile> findActiveProfiles() {
        return profileRepository.findAllActiveWithSocialLinks();
    }

    public List<ProfileWithCompletedEvents> findActiveProfilesWithCompletedEvents() {
        List<Profile> profiles = findActiveProfiles();
        Map<Long, Long> completedEventsByProfileId = completedEventsByProfileId(profiles);

        return profiles.stream()
                .map((profile) -> new ProfileWithCompletedEvents(
                        profile,
                        completedEventsByProfileId.getOrDefault(profile.getId(), 0L)))
                .toList();
    }

    public Profile findActiveProfile(String slug) {
        return profileRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));
    }

    public Profile findActiveProfileWithSocialLinks(String slug) {
        return profileRepository.findActiveBySlugWithSocialLinks(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));
    }

    public ProfileWithCompletedEvents findActiveProfileWithSocialLinksAndCompletedEvents(String slug) {
        Profile profile = findActiveProfileWithSocialLinks(slug);
        long completedEventsCount = bookingRepository.countByProfileIdAndStatusAndEventDateLessThanEqual(
                profile.getId(),
                BookingStatus.ACCEPTED,
                LocalDate.now());

        return new ProfileWithCompletedEvents(profile, completedEventsCount);
    }

    private Map<Long, Long> completedEventsByProfileId(List<Profile> profiles) {
        if (profiles.isEmpty()) return Map.of();

        List<Long> profileIds = profiles.stream().map(Profile::getId).toList();
        return bookingRepository.countCompletedEventsByProfileIds(
                        profileIds,
                        BookingStatus.ACCEPTED,
                        LocalDate.now())
                .stream()
                .collect(Collectors.toMap(
                        BookingRepository.ProfileCompletedEventCount::getProfileId,
                        BookingRepository.ProfileCompletedEventCount::getCompletedEventsCount,
                        Long::sum));
    }

    public record ProfileWithCompletedEvents(Profile profile, long completedEventsCount) {
    }
}
