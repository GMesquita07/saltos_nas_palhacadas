package pt.saltosnaspalhacadas.backend.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByProfileSlugAndPublishedTrueOrderByReviewDateDescIdDesc(String profileSlug);
    List<Review> findAllByPublishedTrueOrderByReviewDateDescIdDesc();
    List<Review> findAllByOrderByReviewDateDescIdDesc();
}
