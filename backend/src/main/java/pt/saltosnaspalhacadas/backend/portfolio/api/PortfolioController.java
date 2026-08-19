package pt.saltosnaspalhacadas.backend.portfolio.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.portfolio.MediaType;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioService;
import pt.saltosnaspalhacadas.backend.profile.ProfileService;

@RestController
@RequestMapping("/api/v1/profiles/{slug}/portfolio")
public class PortfolioController {
    private final ProfileService profileService;
    private final PortfolioService portfolioService;

    public PortfolioController(ProfileService profileService, PortfolioService portfolioService) {
        this.profileService = profileService;
        this.portfolioService = portfolioService;
    }

    @GetMapping
    List<PortfolioItemResponse> findPublishedItems(@PathVariable String slug, @RequestParam(required = false) MediaType type) {
        var profile = profileService.findActiveProfile(slug);
        return portfolioService.findPublishedItems(profile.getId(), type).stream().map(PortfolioItemResponse::from).toList();
    }
}
