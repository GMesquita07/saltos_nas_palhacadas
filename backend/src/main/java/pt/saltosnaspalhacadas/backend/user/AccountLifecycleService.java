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
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPost;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentPostRepository;
import pt.saltosnaspalhacadas.backend.clientcontent.ClientContentStatus;
import pt.saltosnaspalhacadas.backend.favorite.Favorite;
import pt.saltosnaspalhacadas.backend.favorite.FavoriteRepository;
import pt.saltosnaspalhacadas.backend.media.ClientContentMediaService;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaRepository;
import pt.saltosnaspalhacadas.backend.review.Review;
import pt.saltosnaspalhacadas.backend.review.ReviewRepository;

@Service
public class AccountLifecycleService {

    private final AppUserRepository users;
    private final BookingRepository bookings;
    private final ClientContentPostRepository clientPosts;
    private final FavoriteRepository favorites;
    private final ReviewRepository reviews;
    private final ManagedMediaRepository media;
    private final ClientContentMediaService mediaService;
    private final LocalMediaStorage storage;

    public AccountLifecycleService(
            AppUserRepository users,
            BookingRepository bookings,
            ClientContentPostRepository clientPosts,
            FavoriteRepository favorites,
            ReviewRepository reviews,
            ManagedMediaRepository media,
            ClientContentMediaService mediaService,
            LocalMediaStorage storage) {
        this.users = users;
        this.bookings = bookings;
        this.clientPosts = clientPosts;
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
                clientPosts.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(ClientContentExport::from).toList(),
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
        deleteClientPosts(userId);
        reviews.deleteAllByUserId(userId);
        favorites.deleteAllByUserId(userId);
        anonymizeBookings(userId);
        deleteOwnedMedia(userId);
    }

    private void deleteClientPosts(Long userId) throws IOException {
        List<ClientContentPost> posts = clientPosts.findAllByUserIdOrderByCreatedAtDescIdDesc(userId);
        for (ClientContentPost post : posts) {
            deleteLegacyMediaIfNeeded(post);
        }
        clientPosts.deleteAll(posts);
        clientPosts.flush();
    }

    private void deleteLegacyMediaIfNeeded(ClientContentPost post) throws IOException {
        if (post.getMediaObject() == null) {
            storage.deleteManagedUrl(post.getMediaUrl());
        }
        if (post.getThumbnailObject() == null) {
            storage.deleteManagedUrl(post.getThumbnailUrl());
        }
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
            List<ClientContentExport> clientContent,
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

    public record ClientContentExport(
            Long id,
            String profileName,
            String mediaType,
            String title,
            String location,
            java.time.LocalDate eventDate,
            String caption,
            ClientContentStatus status,
            String publicDisplayName,
            boolean showLocation,
            boolean showEventDate,
            String consentVersion,
            Instant consentedAt,
            Instant createdAt) {
        static ClientContentExport from(ClientContentPost post) {
            return new ClientContentExport(
                    post.getId(),
                    post.getProfile() == null ? null : post.getProfile().getName(),
                    post.getMediaType().name(),
                    post.getTitle(),
                    post.getLocation(),
                    post.getEventDate(),
                    post.getCaption(),
                    post.getStatus(),
                    post.getPublicDisplayName(),
                    post.isShowLocation(),
                    post.isShowEventDate(),
                    post.getConsentVersion(),
                    post.getConsentedAt(),
                    post.getCreatedAt());
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
