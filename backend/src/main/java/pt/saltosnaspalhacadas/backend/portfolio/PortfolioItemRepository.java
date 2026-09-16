package pt.saltosnaspalhacadas.backend.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findAllByProfileIdAndPublishedTrueOrderByEventDateDescIdDesc(Long profileId);
    List<PortfolioItem> findAllByProfileIdAndPublishedTrueAndMediaTypeOrderByEventDateDescIdDesc(Long profileId, MediaType mediaType);
    Optional<PortfolioItem> findByIdAndProfileSlug(Long id, String slug);
}
