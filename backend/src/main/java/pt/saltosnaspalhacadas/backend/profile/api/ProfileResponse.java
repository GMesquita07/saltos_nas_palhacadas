package pt.saltosnaspalhacadas.backend.profile.api;

import java.util.List;

import pt.saltosnaspalhacadas.backend.profile.Profile;

public record ProfileResponse(Long id, String slug, String name, String role, String description, String profileImageUrl, String profileImagePosition, double profileImageZoom, String featuredVideoUrl, String heroBackgroundImageUrl, int displayOrder, List<ProfileSocialLinkResponse> socialLinks) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
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
                profile.getDisplayOrder(),
                profile.getActiveSocialLinks().stream().map(ProfileSocialLinkResponse::from).toList());
    }
}
