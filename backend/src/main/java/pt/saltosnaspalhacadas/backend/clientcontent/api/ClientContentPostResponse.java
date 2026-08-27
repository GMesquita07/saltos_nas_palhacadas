package pt.saltosnaspalhacadas.backend.clientcontent.api;

import java.time.Instant;
import java.time.LocalDate;

import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.user.AppUser;

public record ClientContentPostResponse(
        Long id,
        String type,
        String title,
        String location,
        LocalDate eventDate,
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
        String submittedByEmail) {

    public static ClientContentPostResponse publicFrom(ClientContentPost post) {
        return from(post, false, false);
    }

    public static ClientContentPostResponse mineFrom(ClientContentPost post) {
        return from(post, false, true);
    }

    public static ClientContentPostResponse adminFrom(ClientContentPost post) {
        return from(post, true, true);
    }

    private static ClientContentPostResponse from(ClientContentPost post, boolean includeSubmittedByEmail, boolean includeAdminMessage) {
        Profile profile = post.getProfile();
        AppUser user = post.getUser();

        return new ClientContentPostResponse(
                post.getId(),
                post.getMediaType().name(),
                post.getTitle(),
                post.getLocation(),
                post.getEventDate(),
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
                displayName(user),
                includeSubmittedByEmail && user != null ? user.getEmail() : null);
    }

    private static String displayName(AppUser user) {
        if (user == null) return "Cliente";

        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (!fullName.isBlank()) return fullName;
        if (user.getUsername() != null && !user.getUsername().isBlank()) return "@" + user.getUsername();
        return "Cliente";
    }
}
