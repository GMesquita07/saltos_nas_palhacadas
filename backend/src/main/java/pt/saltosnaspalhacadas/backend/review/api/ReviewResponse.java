package pt.saltosnaspalhacadas.backend.review.api;

import java.time.LocalDate;

import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.review.Review;

public record ReviewResponse(Long id, String profileSlug, String profileName, String reviewerName, String title, String comment, int rating, LocalDate reviewDate, boolean published) {
    public static ReviewResponse from(Review review) {
        Profile profile = review.getProfile();
        return new ReviewResponse(
                review.getId(),
                profile == null ? null : profile.getSlug(),
                profile == null ? null : profile.getName(),
                review.getReviewerName(),
                review.getTitle(),
                review.getComment(),
                review.getRating(),
                review.getReviewDate(),
                review.isPublished());
    }
}
