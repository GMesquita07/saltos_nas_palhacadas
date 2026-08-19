package pt.saltosnaspalhacadas.backend.portfolio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findAllByProfileIdAndPublishedTrueOrderByDisplayOrderAscEventDateDesc(Long profileId);
    List<PortfolioItem> findAllByProfileIdAndPublishedTrueAndMediaTypeOrderByDisplayOrderAscEventDateDesc(Long profileId, MediaType mediaType);
}
