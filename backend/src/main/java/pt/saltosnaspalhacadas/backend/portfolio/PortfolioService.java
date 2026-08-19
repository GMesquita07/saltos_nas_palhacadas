package pt.saltosnaspalhacadas.backend.portfolio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PortfolioService {
    private final PortfolioItemRepository portfolioItemRepository;

    public PortfolioService(PortfolioItemRepository portfolioItemRepository) {
        this.portfolioItemRepository = portfolioItemRepository;
    }

    public List<PortfolioItem> findPublishedItems(Long profileId, MediaType mediaType) {
        if (mediaType == null) {
            return portfolioItemRepository.findAllByProfileIdAndPublishedTrueOrderByDisplayOrderAscEventDateDesc(profileId);
        }
        return portfolioItemRepository.findAllByProfileIdAndPublishedTrueAndMediaTypeOrderByDisplayOrderAscEventDateDesc(profileId, mediaType);
    }
}
