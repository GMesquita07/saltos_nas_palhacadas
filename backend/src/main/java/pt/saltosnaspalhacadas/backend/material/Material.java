package pt.saltosnaspalhacadas.backend.material;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "image_position", nullable = false, length = 20)
    private String imagePosition = "50% 50%";

    @Column(name = "image_zoom", nullable = false)
    private double imageZoom = 1.0;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Material() {
    }

    public Material(String name, String imageUrl, int displayOrder) {
        this(name, imageUrl, "50% 50%", 1.0, displayOrder);
    }

    public Material(String name, String imageUrl, String imagePosition, double imageZoom, int displayOrder) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.imagePosition = imagePosition == null ? "50% 50%" : imagePosition;
        this.imageZoom = normalizeZoom(imageZoom);
        this.displayOrder = displayOrder;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImagePosition() {
        return imagePosition;
    }

    public double getImageZoom() {
        return imageZoom;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void update(String name, String imageUrl) {
        update(name, imageUrl, imagePosition, imageZoom);
    }

    public void update(String name, String imageUrl, String imagePosition, double imageZoom) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.imagePosition = imagePosition == null ? "50% 50%" : imagePosition;
        this.imageZoom = normalizeZoom(imageZoom);
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private static double normalizeZoom(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0;
        }

        return Math.min(3.0, Math.max(1.0, value));
    }
}
