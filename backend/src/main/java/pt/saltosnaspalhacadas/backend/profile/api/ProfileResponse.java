package pt.saltosnaspalhacadas.backend.profile.api;

import pt.saltosnaspalhacadas.backend.profile.Profile;

public record ProfileResponse(Long id, String slug, String name, String role, String description, String profileImageUrl) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(profile.getId(), profile.getSlug(), profile.getName(), profile.getRole(), profile.getDescription(), profile.getProfileImageUrl());
    }
}
