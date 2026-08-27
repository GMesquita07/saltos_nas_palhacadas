package pt.saltosnaspalhacadas.backend.favorite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndPortfolioItemId(Long userId, Long portfolioItemId);

    @Query("""
            select favorite from Favorite favorite
            join fetch favorite.portfolioItem item
            join fetch item.profile profile
            where favorite.user.id = :userId
              and item.published = true
              and profile.active = true
            order by favorite.createdAt desc
            """)
    List<Favorite> findVisibleByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<Favorite> findAllByUserId(Long userId);

    @Transactional
    void deleteAllByUserId(Long userId);
}
