package pt.saltosnaspalhacadas.backend.admin;

import pt.saltosnaspalhacadas.backend.profile.Profile;

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
        String notificationEmail,
        int displayOrder) {

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
                profile.getNotificationEmail(),
                profile.getDisplayOrder());
    }
}
