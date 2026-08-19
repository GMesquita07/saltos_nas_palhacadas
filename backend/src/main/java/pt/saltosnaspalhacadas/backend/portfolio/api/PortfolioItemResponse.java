package pt.saltosnaspalhacadas.backend.portfolio.api;

import java.time.LocalDate;

import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItem;

public record PortfolioItemResponse(Long id, String type, String title, String location, LocalDate eventDate, String mediaUrl, String thumbnailUrl, int displayOrder) {
    public static PortfolioItemResponse from(PortfolioItem item) {
        return new PortfolioItemResponse(item.getId(), item.getMediaType().name(), item.getTitle(), item.getLocation(), item.getEventDate(), item.getMediaUrl(), item.getThumbnailUrl(), item.getDisplayOrder());
    }
}
