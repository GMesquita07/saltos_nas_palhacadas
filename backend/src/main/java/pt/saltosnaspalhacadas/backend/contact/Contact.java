package pt.saltosnaspalhacadas.backend.contact;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contacts")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 20)
    private ContactType type;

    @Column(name = "contact_value", nullable = false, length = 500)
    private String value;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Contact() { }

    public Contact(String label, ContactType type, String value, int displayOrder) {
        this.label = label;
        this.type = type;
        this.value = value;
        this.displayOrder = displayOrder;
    }

    @jakarta.persistence.PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @jakarta.persistence.PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public ContactType getType() { return type; }
    public String getValue() { return value; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isVisible() { return visible; }
    public void update(String label, ContactType type, String value) { this.label = label; this.type = type; this.value = value; }
}
