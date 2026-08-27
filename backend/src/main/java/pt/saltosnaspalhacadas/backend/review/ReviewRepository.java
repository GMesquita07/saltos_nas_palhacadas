package pt.saltosnaspalhacadas.backend.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByProfileSlugAndPublishedTrueOrderByReviewDateDescIdDesc(String profileSlug);
    List<Review> findAllByPublishedTrueOrderByReviewDateDescIdDesc();
    List<Review> findAllByOrderByReviewDateDescIdDesc();
    List<Review> findAllByUserId(Long userId);
    @Transactional
    void deleteAllByUserId(Long userId);
}
