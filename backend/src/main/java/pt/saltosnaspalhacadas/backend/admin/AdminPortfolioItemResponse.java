package pt.saltosnaspalhacadas.backend.admin;

import java.time.LocalDate;

import pt.saltosnaspalhacadas.backend.portfolio.PortfolioItem;

public record AdminPortfolioItemResponse(
        Long id,
        String type,
        String title,
        String location,
        LocalDate eventDate,
        String mediaUrl,
        String thumbnailUrl,
        String thumbnailPosition,
        double thumbnailZoom,
        int displayOrder,
        boolean published) {

    public static AdminPortfolioItemResponse from(PortfolioItem item) {
        return new AdminPortfolioItemResponse(
                item.getId(),
                item.getMediaType().name(),
                item.getTitle(),
                item.getLocation(),
                item.getEventDate(),
                item.getMediaUrl(),
                item.getThumbnailUrl(),
                item.getThumbnailPosition(),
                item.getThumbnailZoom(),
                item.getDisplayOrder(),
                item.isPublished());
    }
}
