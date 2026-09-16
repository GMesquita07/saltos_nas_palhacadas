package pt.saltosnaspalhacadas.backend.clientcontent.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonInclude;

import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.user.AppUser;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientContentPostResponse(
        Long id,
        String type,
        String title,
        String location,
        LocalDate eventDate,
        String eventMonth,
        String caption,
        String mediaUrl,
        String thumbnailUrl,
        String status,
        Instant createdAt,
        Instant moderatedAt,
        String adminMessage,
        Long profileId,
        String profileSlug,
        String profileName,
        String submittedByName,
        String submittedByEmail,
        String publicDisplayName,
        boolean showLocation,
        boolean showEventDate,
        String consentVersion,
        Instant consentedAt) {

    public static ClientContentPostResponse publicFrom(ClientContentPost post) {
        return from(post, false, false, true);
    }

    public static ClientContentPostResponse mineFrom(ClientContentPost post) {
        return from(post, false, true, false);
    }

    public static ClientContentPostResponse adminFrom(ClientContentPost post) {
        return from(post, true, true, false);
    }

    private static ClientContentPostResponse from(ClientContentPost post, boolean includeSubmittedByEmail, boolean includeAdminMessage, boolean publicView) {
        Profile profile = post.getProfile();
        AppUser user = post.getUser();
        LocalDate eventDate = publicView ? null : post.getEventDate();
        String eventMonth = publicView && post.isShowEventDate() && post.getEventDate() != null
                ? YearMonth.from(post.getEventDate()).toString()
                : null;
        String location = publicView && !post.isShowLocation() ? null : post.getLocation();
        String submittedByName = publicView ? post.getPublicDisplayName() : displayName(user);

        return new ClientContentPostResponse(
                post.getId(),
                post.getMediaType().name(),
                post.getTitle(),
                location,
                eventDate,
                eventMonth,
                post.getCaption(),
                post.getMediaUrl(),
                post.getThumbnailUrl(),
                post.getStatus().name(),
                post.getCreatedAt(),
                post.getModeratedAt(),
                includeAdminMessage ? post.getAdminMessage() : null,
                profile == null ? null : profile.getId(),
                profile == null ? null : profile.getSlug(),
                profile == null ? null : profile.getName(),
                submittedByName,
                includeSubmittedByEmail && user != null ? user.getEmail() : null,
                post.getPublicDisplayName(),
                post.isShowLocation(),
                post.isShowEventDate(),
                includeAdminMessage ? post.getConsentVersion() : null,
                includeAdminMessage ? post.getConsentedAt() : null);
    }

    private static String displayName(AppUser user) {
        if (user == null) return "Cliente";

        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (!fullName.isBlank()) return fullName;
        if (user.getUsername() != null && !user.getUsername().isBlank()) return "@" + user.getUsername();
        return "Cliente";
    }
}
