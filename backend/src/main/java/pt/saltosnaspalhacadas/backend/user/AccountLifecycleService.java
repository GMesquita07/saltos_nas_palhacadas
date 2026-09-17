package pt.saltosnaspalhacadas.backend.user;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingRepository;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;
import pt.saltosnaspalhacadas.backend.favorite.Favorite;
import pt.saltosnaspalhacadas.backend.favorite.FavoriteRepository;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaService;
import pt.saltosnaspalhacadas.backend.media.MediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaRepository;
import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;

@Service
public class AccountLifecycleService {

    private final AppUserRepository users;
    private final BookingRepository bookings;
    private final FavoriteRepository favorites;
    private final ReviewRepository reviews;
    private final ManagedMediaRepository media;
    private final ManagedMediaService mediaService;
    private final MediaStorage storage;

    public AccountLifecycleService(
            AppUserRepository users,
            BookingRepository bookings,
            FavoriteRepository favorites,
            ReviewRepository reviews,
            ManagedMediaRepository media,
            ManagedMediaService mediaService,
            MediaStorage storage) {
        this.users = users;
        this.bookings = bookings;
        this.favorites = favorites;
        this.reviews = reviews;
        this.media = media;
        this.mediaService = mediaService;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public AccountDataExport exportFor(Long userId) {
        AppUser user = users.findById(userId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));

        return new AccountDataExport(
                Instant.now(),
                AccountProfile.from(user),
                bookings.findAllByUserIdWithProfileOrderByCreatedAtDesc(userId).stream().map(BookingExport::from).toList(),
                favorites.findAllByUserId(userId).stream().map(FavoriteExport::from).toList(),
                reviews.findAllByUserId(userId).stream().map(ReviewExport::from).toList());
    }

    @Transactional(rollbackFor = IOException.class)
    public void deleteAccount(Long userId, String disabledPasswordHash) throws IOException {
        AppUser user = users.findById(userId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A conta de administrador deve ser removida manualmente por outro administrador");
        }

        String legacyProfileImageUrl = user.getProfileImageUrl();
        String anonymizedEmail = "deleted-user-%d-%d@deleted.local".formatted(user.getId(), System.currentTimeMillis());
        user.anonymizeForDeletion(anonymizedEmail, disabledPasswordHash);
        users.saveAndFlush(user);

        storage.deleteManagedUrl(legacyProfileImageUrl);
        reviews.deleteAllByUserId(userId);
        favorites.deleteAllByUserId(userId);
        anonymizeBookings(userId);
        deleteOwnedMedia(userId);
    }

    private void anonymizeBookings(Long userId) {
        List<Booking> userBookings = bookings.findAllByUserIdOrderByCreatedAtDesc(userId);
        for (Booking booking : userBookings) {
            booking.anonymizeForAccountDeletion();
        }
        bookings.saveAll(userBookings);
    }

    private void deleteOwnedMedia(Long userId) throws IOException {
        for (ManagedMedia ownedMedia : media.findAllByOwnerId(userId)) {
            mediaService.delete(ownedMedia);
            media.delete(ownedMedia);
        }
    }

    public record AccountDataExport(
            Instant exportedAt,
            AccountProfile profile,
            List<BookingExport> bookings,
            List<FavoriteExport> favorites,
            List<ReviewExport> reviews) {
    }

    public record AccountProfile(
            String email,
            String username,
            String firstName,
            String lastName,
            String phone,
            String role,
            Instant createdAt) {
        static AccountProfile from(AppUser user) {
            return new AccountProfile(
                    user.getEmail(),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhone(),
                    user.getRole().name(),
                    user.getCreatedAt());
        }
    }

    public record BookingExport(
            Long id,
            String profileName,
            java.time.LocalDate eventDate,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            String eventType,
            String customEventType,
            String weddingCoupleNames,
            String location,
            String contactName,
            String contactEmail,
            String contactPhone,
            String description,
            String notes,
            BookingStatus status,
            String adminMessage,
            java.math.BigDecimal counterBudget,
            java.time.LocalDate counterEventDate,
            Instant createdAt,
            Instant updatedAt) {
        static BookingExport from(Booking booking) {
            return new BookingExport(
                    booking.getId(),
                    booking.getProfile().getName(),
                    booking.getEventDate(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getEventType().name(),
                    booking.getCustomEventType(),
                    booking.getWeddingCoupleNames(),
                    booking.getLocation(),
                    booking.getContactName(),
                    booking.getContactEmail(),
                    booking.getContactPhone(),
                    booking.getDescription(),
                    booking.getNotes(),
                    booking.getStatus(),
                    booking.getAdminMessage(),
                    booking.getCounterBudget(),
                    booking.getCounterEventDate(),
                    booking.getCreatedAt(),
                    booking.getUpdatedAt());
        }
    }

    public record FavoriteExport(
            Long id,
            String portfolioTitle,
            String profileName,
            Instant createdAt) {
        static FavoriteExport from(Favorite favorite) {
            return new FavoriteExport(
                    favorite.getId(),
                    favorite.getPortfolioItem().getTitle(),
                    favorite.getPortfolioItem().getProfile().getName(),
                    favorite.getCreatedAt());
        }
    }

    public record ReviewExport(
            Long id,
            String profileName,
            String reviewerName,
            String title,
            String comment,
            int rating,
            java.time.LocalDate reviewDate,
            boolean published) {
        static ReviewExport from(Review review) {
            return new ReviewExport(
                    review.getId(),
                    review.getProfile() == null ? null : review.getProfile().getName(),
                    review.getReviewerName(),
                    review.getTitle(),
                    review.getComment(),
                    review.getRating(),
                    review.getReviewDate(),
                    review.isPublished());
        }
    }
}
