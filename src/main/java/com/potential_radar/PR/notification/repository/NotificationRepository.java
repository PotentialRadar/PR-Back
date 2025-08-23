package com.potential_radar.PR.notification.repository;


import com.potential_radar.PR.notification.domain.Notification;
import com.potential_radar.PR.notification.domain.NotificationType;
import com.potential_radar.PR.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.receiver.userId = :userId
              AND (:lastId IS NULL OR n.id < :lastId)
            ORDER BY
              CASE WHEN n.notificationType = com.potential_radar.PR.notification.domain.NotificationType.INVITATION THEN 0 ELSE 1 END,
              n.actionCreatedAt DESC
            """)
    List<Notification> findNextPageByUserId(
            @Param("userId") Long userId,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    Long countByReceiverUserId(Long userId);

    @Query("SELECT count(n) FROM Notification n WHERE n.receiver = :receiver AND n.isRead = false")
    long countUnreadNotifications(@Param("receiver") User receiver);

    Slice<Notification> findByReceiverUserIdAndIdLessThanOrderByIdDesc(Long receiverId, Long lastId, Pageable pageable);

    Optional<Notification> findByInvitationId(Long invitationId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiver = :user AND n.notificationType = :type")
    int deleteAllNotificationsByType(@Param("user") User user, @Param("type") NotificationType type);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.receiver = :user")
    int deleteAllNotificationsForUser(@Param("user") User user);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.id = :notificationId AND n.receiver = :user")
    int deleteByIdAndUser(@Param("notificationId") Long notificationId, @Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);
}