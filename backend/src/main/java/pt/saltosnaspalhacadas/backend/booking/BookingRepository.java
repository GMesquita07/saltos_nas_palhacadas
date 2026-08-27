package pt.saltosnaspalhacadas.backend.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select booking from Booking booking
            join fetch booking.profile profile
            where booking.user.id = :userId
            order by booking.createdAt desc
            """)
    List<Booking> findAllByUserIdWithProfileOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select booking from Booking booking
            join fetch booking.profile profile
            where (:status is null or booking.status = :status)
            order by booking.createdAt desc
            """)
    List<Booking> findAllForAdmin(@Param("status") BookingStatus status);

    @Query("""
            select booking from Booking booking
            join fetch booking.profile profile
            join fetch booking.user user
            where booking.id = :id
            """)
    Optional<Booking> findByIdWithProfile(@Param("id") Long id);

    boolean existsByProfileIdAndEventDateAndStatus(Long profileId, LocalDate eventDate, BookingStatus status);

    boolean existsByProfileId(Long profileId);

    @Query("""
            select booking from Booking booking
            join fetch booking.profile profile
            where booking.status = :status
              and booking.eventDate = :eventDate
              and booking.reminderSentAt is null
              and booking.contactEmail is not null
              and trim(booking.contactEmail) <> ''
            order by booking.startTime asc, booking.id asc
            """)
    List<Booking> findAcceptedBookingsDueForReminder(
            @Param("status") BookingStatus status,
            @Param("eventDate") LocalDate eventDate);

    @Query("""
            select booking from Booking booking
            where booking.user.id = :userId
              and booking.profile.id = :profileId
              and booking.eventDate = :eventDate
              and booking.status in :statuses
            """)
    List<Booking> findUserBookingsOnDateWithStatuses(
            @Param("userId") Long userId,
            @Param("profileId") Long profileId,
            @Param("eventDate") LocalDate eventDate,
            @Param("statuses") java.util.Collection<BookingStatus> statuses);

    @Query("""
            select booking from Booking booking
            where booking.profile.id = :profileId
              and booking.eventDate = :eventDate
              and booking.status in :statuses
            """)
    List<Booking> findProfileBookingsOnDateWithStatuses(
            @Param("profileId") Long profileId,
            @Param("eventDate") LocalDate eventDate,
            @Param("statuses") java.util.Collection<BookingStatus> statuses);

    @Query("""
            select booking from Booking booking
            where booking.profile.slug = :profileSlug
              and booking.status in :statuses
              and booking.eventDate between :from and :to
            order by booking.eventDate asc, booking.startTime asc, booking.createdAt asc
            """)
    List<Booking> findAvailabilitySlots(
            @Param("profileSlug") String profileSlug,
            @Param("statuses") java.util.Collection<BookingStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select distinct booking.eventDate from Booking booking
            where booking.profile.slug = :profileSlug
              and booking.status = :status
              and booking.eventDate between :from and :to
            order by booking.eventDate asc
            """)
    List<LocalDate> findBookedDates(
            @Param("profileSlug") String profileSlug,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
