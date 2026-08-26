package pt.saltosnaspalhacadas.backend.material.api;

import pt.saltosnaspalhacadas.backend.material.Material;

public record MaterialResponse(Long id, String name, String imageUrl, int displayOrder) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getImageUrl(),
                material.getDisplayOrder());
    }
}
