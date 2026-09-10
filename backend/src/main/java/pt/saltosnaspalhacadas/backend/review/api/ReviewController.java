package pt.saltosnaspalhacadas.backend.review.api;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileNotFoundException;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;
import pt.saltosnaspalhacadas.backend.security.ClientIpAddress;
import pt.saltosnaspalhacadas.backend.security.IpRateLimiter;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@RestController
@RequestMapping("/api/v1/profiles/{slug}/reviews")
public class ReviewController {

    private final ReviewRepository reviews;
    private final ProfileRepository profiles;
    private final AppUserRepository users;
    private final IpRateLimiter rateLimiter;
    private final ApiResponseLimits responseLimits;
    private final int reviewRateLimitPerMinute;

    public ReviewController(
            ReviewRepository reviews,
            ProfileRepository profiles,
            AppUserRepository users,
            IpRateLimiter rateLimiter,
            ApiResponseLimits responseLimits,
            @Value("${app.review.rate-limit-per-minute:6}") int reviewRateLimitPerMinute) {
        this.reviews = reviews;
        this.profiles = profiles;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.responseLimits = responseLimits;
        this.reviewRateLimitPerMinute = reviewRateLimitPerMinute;
    }

    @GetMapping
    List<ReviewResponse> findPublished(@PathVariable String slug) {
        profiles.findBySlugAndActiveTrue(slug).orElseThrow(() -> new ProfileNotFoundException(slug));
        return responseLimits.publicList(reviews.findAllByProfileSlugAndPublishedTrueOrderByReviewDateDescIdDesc(slug)
                .stream()
                .map(ReviewResponse::from));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReviewResponse submitReview(
            HttpServletRequest servletRequest,
            @PathVariable String slug,
            Authentication authentication,
            @Valid @RequestBody SubmitReviewRequest request) {
        assertReviewAllowed(servletRequest);
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para deixar uma avaliação");
        }

        Profile profile = profiles.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ProfileNotFoundException(slug));
        AppUser user = users.findByEmailAndActiveTrue(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tua sessão já não é válida"));

        Review review = new Review(
                profile,
                user,
                request.reviewerName().trim(),
                request.title().trim(),
                request.comment().trim(),
                request.rating(),
                LocalDate.now(),
                0,
                false);

        return ReviewResponse.from(reviews.save(review));
    }

    private void assertReviewAllowed(HttpServletRequest servletRequest) {
        if (!rateLimiter.tryAcquire("review-submit", ClientIpAddress.from(servletRequest), reviewRateLimitPerMinute, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas avaliações em pouco tempo. Tenta novamente dentro de instantes.");
        }
    }

    record SubmitReviewRequest(
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String reviewerName,
            @NotBlank(message = "O título da avaliação é obrigatório")
            @Size(max = 180, message = "O título da avaliação pode ter no máximo 180 caracteres")
            String title,
            @NotBlank(message = "O comentário é obrigatório")
            @Size(max = 1200, message = "O comentário pode ter no máximo 1200 caracteres")
            String comment,
            @NotNull(message = "Seleciona uma avaliação de 1 a 5 estrelas")
            @Min(value = 1, message = "A avaliação mínima é 1 estrela")
            @Max(value = 5, message = "A avaliação máxima é 5 estrelas")
            Integer rating) {
    }
}
