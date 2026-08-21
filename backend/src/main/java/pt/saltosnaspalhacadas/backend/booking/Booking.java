package pt.saltosnaspalhacadas.backend.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.user.AppUser;

@Entity
@Table(name = "booking_requests")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private BookingEventType eventType;

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 40)
    private String contactPhone;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "admin_message", length = 1000)
    private String adminMessage;

    @Column(name = "counter_budget", precision = 10, scale = 2)
    private BigDecimal counterBudget;

    @Column(name = "counter_event_date")
    private LocalDate counterEventDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() {
    }

    public Booking(
            AppUser user,
            Profile profile,
            LocalDate eventDate,
            BookingEventType eventType,
            String contactName,
            String contactPhone,
            BigDecimal budget,
            String description,
            String notes) {
        this.user = user;
        this.profile = profile;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.contactName = contactName.trim();
        this.contactPhone = contactPhone.trim();
        this.budget = budget;
        this.description = description.trim();
        this.notes = emptyToNull(notes);
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

    public void decide(
            BookingStatus status,
            String adminMessage,
            BigDecimal counterBudget,
            LocalDate counterEventDate) {
        this.status = status;
        this.adminMessage = emptyToNull(adminMessage);
        this.counterBudget = status == BookingStatus.COUNTER_PROPOSED ? counterBudget : null;
        this.counterEventDate = status == BookingStatus.COUNTER_PROPOSED ? counterEventDate : null;
    }

    public void acceptCounterProposal(LocalDate acceptedEventDate, BigDecimal acceptedBudget) {
        this.eventDate = acceptedEventDate;
        this.budget = acceptedBudget;
        this.status = BookingStatus.ACCEPTED;
        this.counterBudget = null;
        this.counterEventDate = null;
    }

    public void declineCounterProposal() {
        this.status = BookingStatus.DECLINED;
        this.counterBudget = null;
        this.counterEventDate = null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Profile getProfile() { return profile; }
    public LocalDate getEventDate() { return eventDate; }
    public BookingEventType getEventType() { return eventType; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public BigDecimal getBudget() { return budget; }
    public String getDescription() { return description; }
    public String getNotes() { return notes; }
    public BookingStatus getStatus() { return status; }
    public String getAdminMessage() { return adminMessage; }
    public BigDecimal getCounterBudget() { return counterBudget; }
    public LocalDate getCounterEventDate() { return counterEventDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
