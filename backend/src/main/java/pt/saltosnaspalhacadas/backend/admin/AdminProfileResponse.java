package pt.saltosnaspalhacadas.backend.admin;

import java.util.List;

import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.api.ProfileSocialLinkResponse;

public record AdminProfileResponse(
        Long id,
        String slug,
        String name,
        String role,
        String description,
        String profileImageUrl,
        String profileImagePosition,
        double profileImageZoom,
        String featuredVideoUrl,
        String heroBackgroundImageUrl,
        String notificationEmail,
        int displayOrder,
        List<ProfileSocialLinkResponse> socialLinks) {

    public static AdminProfileResponse from(Profile profile) {
        return new AdminProfileResponse(
                profile.getId(),
                profile.getSlug(),
                profile.getName(),
                profile.getRole(),
                profile.getDescription(),
                profile.getProfileImageUrl(),
                profile.getProfileImagePosition(),
                profile.getProfileImageZoom(),
                profile.getFeaturedVideoUrl(),
                profile.getHeroBackgroundImageUrl(),
                profile.getNotificationEmail(),
                profile.getDisplayOrder(),
                profile.getActiveSocialLinks()
                        .stream()
                        .map(ProfileSocialLinkResponse::from)
                        .toList());
    }
}
