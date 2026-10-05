package pt.saltosnaspalhacadas.backend.usernotification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @EntityGraph(attributePaths = "booking")
    List<UserNotification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserNotification notification
            set notification.readAt = :readAt
            where notification.user.id = :userId
              and notification.readAt is null
            """)
    int markAllReadByUserId(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    @Modifying(flushAutomatically = true)
    @Query("""
            update UserNotification notification
            set notification.readAt = :readAt
            where notification.user.id = :userId
              and notification.booking.id = :bookingId
              and notification.type = :type
              and notification.readAt is null
            """)
    int markUnreadByUserIdAndBookingIdAndType(
            @Param("userId") Long userId,
            @Param("bookingId") Long bookingId,
            @Param("type") UserNotificationType type,
            @Param("readAt") Instant readAt);

    @Modifying(flushAutomatically = true)
    @Query("""
            update UserNotification notification
            set notification.readAt = :readAt
            where notification.booking.id = :bookingId
              and notification.type = :type
              and notification.readAt is null
            """)
    int markReadByBookingIdAndType(
            @Param("bookingId") Long bookingId,
            @Param("type") UserNotificationType type,
            @Param("readAt") Instant readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from UserNotification notification where notification.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
