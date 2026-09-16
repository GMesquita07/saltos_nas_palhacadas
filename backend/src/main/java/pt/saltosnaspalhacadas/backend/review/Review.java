package pt.saltosnaspalhacadas.backend.review;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.user.AppUser;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "reviewer_name", nullable = false, length = 120)
    private String reviewerName;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1200)
    private String comment;

    @Column(nullable = false)
    private int rating;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() { }

    public Review(Profile profile, AppUser user, String reviewerName, String title, String comment, int rating, LocalDate reviewDate, int displayOrder, boolean published) {
        this.profile = profile;
        this.user = user;
        this.reviewerName = reviewerName;
        this.title = title;
        this.comment = comment;
        this.rating = rating;
        this.reviewDate = reviewDate == null ? LocalDate.now() : reviewDate;
        this.displayOrder = displayOrder;
        this.published = published;
    }

    public Review(String reviewerName, String title, String comment, int rating, LocalDate reviewDate, int displayOrder, boolean published) {
        this(null, null, reviewerName, title, comment, rating, reviewDate, displayOrder, published);
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

    public Long getId() { return id; }
    public Profile getProfile() { return profile; }
    public AppUser getUser() { return user; }
    public String getReviewerName() { return reviewerName; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public int getRating() { return rating; }
    public LocalDate getReviewDate() { return reviewDate; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isPublished() { return published; }

    public void update(String reviewerName, String title, String comment, int rating, LocalDate reviewDate, int displayOrder, boolean published) {
        this.reviewerName = reviewerName;
        this.title = title;
        this.comment = comment;
        this.rating = rating;
        this.reviewDate = reviewDate;
        this.displayOrder = displayOrder;
        this.published = published;
    }

    public void moderate(boolean published) {
        this.published = published;
    }
}
