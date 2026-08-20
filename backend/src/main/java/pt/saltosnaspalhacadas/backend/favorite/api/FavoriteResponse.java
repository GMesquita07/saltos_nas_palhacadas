package pt.saltosnaspalhacadas.backend.favorite.api;

import pt.saltosnaspalhacadas.backend.favorite.Favorite;
import pt.saltosnaspalhacadas.backend.portfolio.api.PortfolioItemResponse;

public record FavoriteResponse(Long portfolioItemId, String profileSlug, PortfolioItemResponse item) {

    public static FavoriteResponse from(Favorite favorite) {
        var item = favorite.getPortfolioItem();
        return new FavoriteResponse(item.getId(), item.getProfile().getSlug(), PortfolioItemResponse.from(item));
    }
}
