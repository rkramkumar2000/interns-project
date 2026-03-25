package com.ecommerce.notification.dto;

import com.ecommerce.notification.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    @NotBlank(message = "Recipient is required")
    private String recipient; // email or phone number
    
    private String subject;
    
    private String content;
    
    private String templateName;
    
    private Map<String, String> templateVariables;
    
    @Builder.Default
    private Integer priority = 5; // 1-10, 1 being highest
    
    private LocalDateTime scheduledTime;
    
    private String eventType; // order_confirmation, password_reset, etc.
    
    private Map<String, Object> metadata;
} 