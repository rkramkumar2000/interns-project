package com.ecommerce.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;
    
    @Column(nullable = false)
    private String recipient; // email address or phone number
    
    @Column(nullable = false)
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String htmlContent;
    
    private String templateName;
    
    @ElementCollection
    @CollectionTable(name = "notification_variables", 
                     joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "variable_key")
    @Column(name = "variable_value")
    private Map<String, String> templateVariables = new HashMap<>();
    
    private Integer priority = 5; // 1-10, 1 being highest
    
    private LocalDateTime scheduledTime;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime deliveredAt;
    
    private LocalDateTime failedAt;
    
    private String failureReason;
    
    private Integer retryCount = 0;
    
    private Integer maxRetries = 3;
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    private String externalId; // ID from external service (Twilio, SendGrid, etc.)
    
    private String eventType; // order_confirmation, password_reset, etc.
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 