package pt.saltosnaspalhacadas.backend.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private BookingEventType eventType;

    @Column(name = "custom_event_type", length = 120)
    private String customEventType;

    @Column(name = "wedding_couple_names", length = 180)
    private String weddingCoupleNames;

    @Column(length = 180)
    private String location;

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 40)
    private String contactPhone;

    @Column(precision = 10, scale = 2)
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

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

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
            LocalTime startTime,
            LocalTime endTime,
            BookingEventType eventType,
            String customEventType,
            String weddingCoupleNames,
            String location,
            String contactName,
            String contactEmail,
            String contactPhone,
            String description,
            String notes) {
        this.user = user;
        this.profile = profile;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventType = eventType;
        this.customEventType = emptyToNull(customEventType);
        this.weddingCoupleNames = emptyToNull(weddingCoupleNames);
        this.location = emptyToNull(location);
        this.contactName = contactName.trim();
        this.contactEmail = emptyToNull(contactEmail);
        this.contactPhone = contactPhone.trim();
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

    public void accept(LocalDate eventDate, LocalTime startTime, LocalTime endTime, BigDecimal budget, String adminMessage) {
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.budget = budget;
        this.status = BookingStatus.ACCEPTED;
        this.adminMessage = emptyToNull(adminMessage);
        this.counterBudget = null;
        this.counterEventDate = null;
        this.cancelledAt = null;
        this.reminderSentAt = null;
    }

    public void decline(String adminMessage) {
        this.status = BookingStatus.DECLINED;
        this.adminMessage = emptyToNull(adminMessage);
        this.counterBudget = null;
        this.counterEventDate = null;
    }

    public void cancel(String adminMessage) {
        this.status = BookingStatus.CANCELLED;
        this.adminMessage = emptyToNull(adminMessage);
        this.counterBudget = null;
        this.counterEventDate = null;
        this.cancelledAt = Instant.now();
    }

    public void counterPropose(String adminMessage, BigDecimal counterBudget, LocalDate counterEventDate) {
        this.status = BookingStatus.COUNTER_PROPOSED;
        this.adminMessage = emptyToNull(adminMessage);
        this.counterBudget = counterBudget;
        this.counterEventDate = counterEventDate;
    }

    public void acceptCounterProposal(LocalDate acceptedEventDate, BigDecimal acceptedBudget) {
        this.eventDate = acceptedEventDate;
        this.budget = acceptedBudget;
        this.status = BookingStatus.ACCEPTED;
        this.counterBudget = null;
        this.counterEventDate = null;
        this.cancelledAt = null;
        this.reminderSentAt = null;
    }

    public void declineCounterProposal() {
        this.status = BookingStatus.DECLINED;
        this.counterBudget = null;
        this.counterEventDate = null;
    }

    public void markReminderSent(Instant sentAt) {
        this.reminderSentAt = sentAt == null ? Instant.now() : sentAt;
    }

    public void anonymizeForAccountDeletion() {
        this.customEventType = null;
        this.weddingCoupleNames = null;
        this.location = null;
        this.contactName = "Cliente eliminado";
        this.contactEmail = null;
        this.contactPhone = "removido";
        this.description = "Dados removidos por pedido de eliminação da conta.";
        this.notes = null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Profile getProfile() { return profile; }
    public LocalDate getEventDate() { return eventDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public BookingEventType getEventType() { return eventType; }
    public String getCustomEventType() { return customEventType; }
    public String getWeddingCoupleNames() { return weddingCoupleNames; }
    public String getLocation() { return location; }
    public String getContactName() { return contactName; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public BigDecimal getBudget() { return budget; }
    public String getDescription() { return description; }
    public String getNotes() { return notes; }
    public BookingStatus getStatus() { return status; }
    public String getAdminMessage() { return adminMessage; }
    public BigDecimal getCounterBudget() { return counterBudget; }
    public LocalDate getCounterEventDate() { return counterEventDate; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getReminderSentAt() { return reminderSentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
