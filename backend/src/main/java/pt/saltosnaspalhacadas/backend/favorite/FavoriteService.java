package pt.saltosnaspalhacadas.backend.favorite;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItem;
import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItemRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;

@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final AppUserRepository users;
    private final PortfolioItemRepository portfolioItems;

    public FavoriteService(FavoriteRepository favorites, AppUserRepository users, PortfolioItemRepository portfolioItems) {
        this.favorites = favorites;
        this.users = users;
        this.portfolioItems = portfolioItems;
    }

    @Transactional(readOnly = true)
    public List<Favorite> findVisibleFavorites(String email) {
        return favorites.findVisibleByUserIdOrderByCreatedAtDesc(findActiveUser(email).getId());
    }

    @Transactional
    public AddFavoriteResult addFavorite(String email, Long portfolioItemId) {
        AppUser user = findActiveUser(email);
        PortfolioItem item = findVisiblePortfolioItem(portfolioItemId);

        return favorites.findByUserIdAndPortfolioItemId(user.getId(), item.getId())
                .map(favorite -> new AddFavoriteResult(favorite, false))
                .orElseGet(() -> new AddFavoriteResult(favorites.save(new Favorite(user, item)), true));
    }

    @Transactional
    public void removeFavorite(String email, Long portfolioItemId) {
        AppUser user = findActiveUser(email);
        favorites.findByUserIdAndPortfolioItemId(user.getId(), portfolioItemId).ifPresent(favorites::delete);
    }

    private AppUser findActiveUser(String email) {
        return users.findByEmailAndActiveTrue(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));
    }

    private PortfolioItem findVisiblePortfolioItem(Long portfolioItemId) {
        return portfolioItems.findById(portfolioItemId)
                .filter(item -> item.isPublished() && item.getProfile().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));
    }

    public record AddFavoriteResult(Favorite favorite, boolean created) {
    }
}
