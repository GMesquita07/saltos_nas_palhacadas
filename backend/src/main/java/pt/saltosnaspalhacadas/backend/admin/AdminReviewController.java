package pt.saltosnaspalhacadas.backend.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;
import pt.saltosnaspalhacadas.backend.review.api.ReviewResponse;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {

    private final ReviewRepository reviews;

    public AdminReviewController(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    List<ReviewResponse> findAll() {
        return reviews.findAllByOrderByReviewDateDescIdDesc()
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    ReviewResponse moderateReview(@PathVariable Long id, @Valid @RequestBody ModerateReviewRequest request) {
        Review review = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        review.moderate(request.published());

        return ReviewResponse.from(reviews.save(review));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteReview(@PathVariable Long id) {
        Review review = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
        reviews.delete(review);
    }

    record ModerateReviewRequest(
            @NotNull(message = "Escolhe se a avaliação fica publicada ou oculta")
            Boolean published) {
    }
}
