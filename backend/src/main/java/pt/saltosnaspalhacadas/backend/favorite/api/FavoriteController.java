package pt.saltosnaspalhacadas.backend.favorite.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.saltosnaspalhacadas.backend.favorite.FavoriteService;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favorites;

    public FavoriteController(FavoriteService favorites) {
        this.favorites = favorites;
    }

    @GetMapping
    List<FavoriteResponse> findFavorites(Authentication authentication) {
        return favorites.findVisibleFavorites(currentEmail(authentication)).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    @PostMapping("/{portfolioItemId}")
    ResponseEntity<FavoriteResponse> addFavorite(
            Authentication authentication,
            @PathVariable Long portfolioItemId) {
        FavoriteService.AddFavoriteResult result = favorites.addFavorite(currentEmail(authentication), portfolioItemId);
        FavoriteResponse response = FavoriteResponse.from(result.favorite());
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.ok(response);
    }

    @DeleteMapping("/{portfolioItemId}")
    ResponseEntity<Void> removeFavorite(Authentication authentication, @PathVariable Long portfolioItemId) {
        favorites.removeFavorite(currentEmail(authentication), portfolioItemId);
        return ResponseEntity.noContent().build();
    }

    private static String currentEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }
        return authentication.getName();
    }
}
