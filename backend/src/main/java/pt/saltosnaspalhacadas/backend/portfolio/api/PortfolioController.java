package pt.saltosnaspalhacadas.backend.portfolio.api;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioService;
import pt.saltosnaspalhacadas.backend.profile.ProfileService;

@RestController
@RequestMapping("/api/v1/profiles/{slug}/portfolio")
public class PortfolioController {
    private static final CacheControl PUBLIC_PORTFOLIO_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final ProfileService profileService;
    private final PortfolioService portfolioService;
    private final ApiResponseLimits responseLimits;

    public PortfolioController(ProfileService profileService, PortfolioService portfolioService, ApiResponseLimits responseLimits) {
        this.profileService = profileService;
        this.portfolioService = portfolioService;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    ResponseEntity<List<PortfolioItemResponse>> findPublishedItems(@PathVariable String slug, @RequestParam(required = false) MediaType type) {
        var profile = profileService.findActiveProfile(slug);
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_PORTFOLIO_CACHE)
                .body(responseLimits.publicList(portfolioService.findPublishedItems(profile.getId(), type).stream().map(PortfolioItemResponse::from)));
    }
}
