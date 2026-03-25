package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    
    // Send notification
    NotificationResponse sendNotification(NotificationRequest request);
    
    NotificationResponse sendNotificationAsync(NotificationRequest request);
    
    List<NotificationResponse> sendBulkNotifications(List<NotificationRequest> requests);
    
    // Get notifications
    NotificationResponse getNotification(Long notificationId);
    
    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);
    
    Page<NotificationResponse> getNotificationsByStatus(NotificationStatus status, Pageable pageable);
    
    // Update notification status
    void updateNotificationStatus(Long notificationId, NotificationStatus status);
    
    void markAsDelivered(Long notificationId);
    
    void markAsFailed(Long notificationId, String reason);
    
    // Retry failed notifications
    void retryFailedNotifications();
    
    // Process scheduled notifications
    void processScheduledNotifications();
    
    // Analytics
    Map<String, Long> getNotificationStatistics(Long userId);
    
    Map<String, Object> getSystemStatistics();
    
    // Clean up
    void cleanupOldNotifications(int daysToKeep);
} 