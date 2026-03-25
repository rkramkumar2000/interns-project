package com.ecommerce.notification.repository;

import com.ecommerce.notification.model.Notification;
import com.ecommerce.notification.model.NotificationStatus;
import com.ecommerce.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find by user
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndType(Long userId, NotificationType type, Pageable pageable);
    
    Page<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);
    
    // Find by status
    List<Notification> findByStatus(NotificationStatus status);
    
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    
    List<Notification> findByStatusAndScheduledTimeLessThanEqual(
            NotificationStatus status, LocalDateTime time);
    
    // Find failed notifications for retry
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' " +
           "AND n.retryCount < n.maxRetries " +
           "AND n.failedAt < :retryAfter")
    List<Notification> findFailedNotificationsForRetry(@Param("retryAfter") LocalDateTime retryAfter);
    
    // Count notifications
    Long countByUserId(Long userId);
    
    Long countByUserIdAndStatus(Long userId, NotificationStatus status);
    
    Long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
    
    Long countByUserIdAndStatusAndCreatedAtAfter(
            Long userId, NotificationStatus status, LocalDateTime after);
    
    Long countByStatus(NotificationStatus status);
    
    // Analytics queries
    @Query("SELECT n.type, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.type")
    List<Object[]> getNotificationCountByType(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT n.status, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.status")
    List<Object[]> getNotificationCountByStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // Clean up old notifications
    void deleteByCreatedAtBeforeAndStatus(LocalDateTime before, NotificationStatus status);
} 