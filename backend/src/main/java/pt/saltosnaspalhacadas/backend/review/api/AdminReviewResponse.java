package pt.saltosnaspalhacadas.backend.review.api;

import java.time.LocalDate;

import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.user.AppUser;

public record AdminReviewResponse(Long id, String profileSlug, String profileName, String reviewerName, String submittedByEmail, String title, String comment, int rating, LocalDate reviewDate, int displayOrder, boolean published) {
    public static AdminReviewResponse from(Review review) {
        Profile profile = review.getProfile();
        AppUser user = review.getUser();
        return new AdminReviewResponse(
                review.getId(),
                profile == null ? null : profile.getSlug(),
                profile == null ? null : profile.getName(),
                review.getReviewerName(),
                user == null ? null : user.getEmail(),
                review.getTitle(),
                review.getComment(),
                review.getRating(),
                review.getReviewDate(),
                review.getDisplayOrder(),
                review.isPublished());
    }
}
