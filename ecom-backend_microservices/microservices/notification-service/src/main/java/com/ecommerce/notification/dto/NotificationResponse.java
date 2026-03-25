package com.ecommerce.notification.dto;

import com.ecommerce.notification.model.NotificationStatus;
import com.ecommerce.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private Long notificationId;
    private Long userId;
    private NotificationType type;
    private NotificationStatus status;
    private String recipient;
    private String subject;
    private String eventType;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private String externalId;
    private LocalDateTime createdAt;
} 