package pt.saltosnaspalhacadas.backend.portfolio.api;

import java.util.List;

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
    private final ProfileService profileService;
    private final PortfolioService portfolioService;
    private final ApiResponseLimits responseLimits;

    public PortfolioController(ProfileService profileService, PortfolioService portfolioService, ApiResponseLimits responseLimits) {
        this.profileService = profileService;
        this.portfolioService = portfolioService;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    List<PortfolioItemResponse> findPublishedItems(@PathVariable String slug, @RequestParam(required = false) MediaType type) {
        var profile = profileService.findActiveProfile(slug);
        return responseLimits.publicList(portfolioService.findPublishedItems(profile.getId(), type).stream().map(PortfolioItemResponse::from));
    }
}
