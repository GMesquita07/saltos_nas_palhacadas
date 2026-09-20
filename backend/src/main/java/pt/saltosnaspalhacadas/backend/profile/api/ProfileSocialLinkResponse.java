package pt.saltosnaspalhacadas.backend.profile.api;

import pt.saltosnaspalhacadas.backend.profile.ProfileSocialLink;

public record ProfileSocialLinkResponse(Long id, String platform, String label, String url, int displayOrder) {
    public static ProfileSocialLinkResponse from(ProfileSocialLink link) {
        return new ProfileSocialLinkResponse(
                link.getId(),
                link.getPlatform(),
                link.getLabel(),
                link.getUrl(),
                link.getDisplayOrder());
    }
}
