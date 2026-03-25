package com.ecommerce.notification.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.model.Notification;
import com.ecommerce.notification.model.NotificationStatus;
import com.ecommerce.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final TemplateService templateService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Sending notification to user: {} via {}", request.getUserId(), request.getType());
        
        // Create notification entity
        Notification notification = createNotificationEntity(request);
        notification = notificationRepository.save(notification);
        
        // Send based on type
        try {
            switch (request.getType()) {
                case EMAIL:
                    sendEmailNotification(notification, request);
                    break;
                case SMS:
                    sendSmsNotification(notification, request);
                    break;
                case PUSH:
                    // TODO: Implement push notifications
                    log.warn("Push notifications not yet implemented");
                    break;
                case IN_APP:
                    // In-app notifications are just saved to database
                    break;
            }
            
            // Update status
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            
        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getNotificationId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedAt(LocalDateTime.now());
            notification.setFailureReason(e.getMessage());
            notification = notificationRepository.save(notification);
        }
        
        return convertToResponse(notification);
    }
    
    @Override
    @Async
    public NotificationResponse sendNotificationAsync(NotificationRequest request) {
        return sendNotification(request);
    }
    
    @Override
    public List<NotificationResponse> sendBulkNotifications(List<NotificationRequest> requests) {
        log.info("Sending {} bulk notifications", requests.size());
        
        List<CompletableFuture<NotificationResponse>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> sendNotification(request)))
                .collect(Collectors.toList());
        
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
    
    @Override
    public NotificationResponse getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        return convertToResponse(notification);
    }
    
    @Override
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    public Page<NotificationResponse> getNotificationsByStatus(NotificationStatus status, Pageable pageable) {
        return notificationRepository.findByStatus(status, pageable)
                .map(this::convertToResponse);
    }
    
    private Notification createNotificationEntity(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setRecipient(request.getRecipient());
        notification.setSubject(request.getSubject());
        notification.setContent(request.getContent());
        notification.setTemplateName(request.getTemplateName());
        notification.setTemplateVariables(request.getTemplateVariables());
        notification.setPriority(request.getPriority());
        notification.setScheduledTime(request.getScheduledTime());
        notification.setEventType(request.getEventType());
        
        // Convert metadata to JSON string
        if (request.getMetadata() != null) {
            try {
                notification.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (Exception e) {
                log.error("Failed to serialize metadata", e);
            }
        }
        
        return notification;
    }
    
    private void sendEmailNotification(Notification notification, NotificationRequest request) {
        String recipient = notification.getRecipient();
        String subject = notification.getSubject();
        
        if (notification.getTemplateName() != null) {
            // Use template
            Map<String, Object> variables = new HashMap<>();
            if (notification.getTemplateVariables() != null) {
                variables.putAll(notification.getTemplateVariables());
            }
            variables.put("userId", notification.getUserId());
            variables.put("notificationId", notification.getNotificationId());
            
            emailService.sendTemplateEmail(recipient, subject, notification.getTemplateName(), variables);
        } else if (notification.getHtmlContent() != null) {
            // Send HTML email
            emailService.sendHtmlEmail(recipient, subject, notification.getHtmlContent());
        } else {
            // Send simple text email
            emailService.sendSimpleEmail(recipient, subject, notification.getContent());
        }
    }
    
    private void sendSmsNotification(Notification notification, NotificationRequest request) {
        String phoneNumber = notification.getRecipient();
        String message = notification.getContent();
        
        if (notification.getTemplateName() != null) {
            // Process template for SMS
            Map<String, Object> variables = new HashMap<>();
            if (notification.getTemplateVariables() != null) {
                variables.putAll(notification.getTemplateVariables());
            }
            message = templateService.processTemplate(notification.getTemplateName() + "-sms", variables);
        }
        
        smsService.sendSms(phoneNumber, message);
    }
    
    private NotificationResponse convertToResponse(Notification notification) {
        return modelMapper.map(notification, NotificationResponse.class);
    }
    
    @Override
    public void updateNotificationStatus(Long notificationId, NotificationStatus status) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        notification.setStatus(status);
        notification.setUpdatedAt(LocalDateTime.now());
        
        if (status == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        } else if (status == NotificationStatus.DELIVERED) {
            notification.setDeliveredAt(LocalDateTime.now());
        } else if (status == NotificationStatus.FAILED) {
            notification.setFailedAt(LocalDateTime.now());
        }
        
        notificationRepository.save(notification);
    }
    
    @Override
    public void markAsDelivered(Long notificationId) {
        updateNotificationStatus(notificationId, NotificationStatus.DELIVERED);
    }
    
    @Override
    public void markAsFailed(Long notificationId, String reason) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailedAt(LocalDateTime.now());
        notification.setFailureReason(reason);
        notification.setRetryCount(notification.getRetryCount() + 1);
        
        notificationRepository.save(notification);
    }
    
    @Override
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void retryFailedNotifications() {
        log.info("Checking for failed notifications to retry...");
        
        LocalDateTime retryAfter = LocalDateTime.now().minusMinutes(15); // Wait 15 minutes before retry
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(retryAfter);
        
        log.info("Found {} failed notifications to retry", failedNotifications.size());
        
        for (Notification notification : failedNotifications) {
            try {
                // Create request from notification
                NotificationRequest request = NotificationRequest.builder()
                        .userId(notification.getUserId())
                        .type(notification.getType())
                        .recipient(notification.getRecipient())
                        .subject(notification.getSubject())
                        .content(notification.getContent())
                        .templateName(notification.getTemplateName())
                        .templateVariables(notification.getTemplateVariables())
                        .priority(notification.getPriority())
                        .eventType(notification.getEventType())
                        .build();
                
                // Retry sending
                switch (notification.getType()) {
                    case EMAIL:
                        sendEmailNotification(notification, request);
                        break;
                    case SMS:
                        sendSmsNotification(notification, request);
                        break;
                    case PUSH:
                        // TODO: Implement push notification retry
                        log.warn("Push notification retry not implemented yet");
                        break;
                    case IN_APP:
                        // In-app notifications are already stored, just update status
                        notification.setStatus(NotificationStatus.DELIVERED);
                        notification.setSentAt(LocalDateTime.now());
                        break;
                }
                
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setRetryCount(notification.getRetryCount() + 1);
                
            } catch (Exception e) {
                log.error("Failed to retry notification: {}", notification.getNotificationId(), e);
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setFailureReason(e.getMessage());
            }
            
            notificationRepository.save(notification);
        }
    }
    
    @Override
    @Scheduled(cron = "0 */5 * * * *") // Run every 5 minutes
    public void processScheduledNotifications() {
        log.debug("Processing scheduled notifications...");
        
        List<Notification> scheduledNotifications = notificationRepository
                .findByStatusAndScheduledTimeLessThanEqual(
                        NotificationStatus.PENDING, 
                        LocalDateTime.now()
                );
        
        if (!scheduledNotifications.isEmpty()) {
            log.info("Found {} scheduled notifications to process", scheduledNotifications.size());
            
            for (Notification notification : scheduledNotifications) {
                NotificationRequest request = NotificationRequest.builder()
                        .userId(notification.getUserId())
                        .type(notification.getType())
                        .recipient(notification.getRecipient())
                        .subject(notification.getSubject())
                        .content(notification.getContent())
                        .templateName(notification.getTemplateName())
                        .templateVariables(notification.getTemplateVariables())
                        .priority(notification.getPriority())
                        .eventType(notification.getEventType())
                        .build();
                
                sendNotification(request);
            }
        }
    }
    
    @Override
    public Map<String, Long> getNotificationStatistics(Long userId) {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("total", notificationRepository.countByUserId(userId));
        stats.put("sent", notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT));
        stats.put("delivered", notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.DELIVERED));
        stats.put("failed", notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.FAILED));
        stats.put("pending", notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.PENDING));
        
        // Last 24 hours
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        stats.put("last24Hours", notificationRepository.countByUserIdAndCreatedAtAfter(userId, last24Hours));
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // Today's statistics
        List<Object[]> typeStats = notificationRepository.getNotificationCountByType(startOfDay, endOfDay);
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeStats) {
            typeMap.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("todayByType", typeMap);
        
        List<Object[]> statusStats = notificationRepository.getNotificationCountByStatus(startOfDay, endOfDay);
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusStats) {
            statusMap.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("todayByStatus", statusMap);
        
        // Overall counts
        stats.put("totalNotifications", notificationRepository.count());
        stats.put("pendingCount", notificationRepository.countByStatus(NotificationStatus.PENDING));
        stats.put("failedCount", notificationRepository.countByStatus(NotificationStatus.FAILED));
        
        return stats;
    }
    
    @Override
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void cleanupOldNotifications(int daysToKeep) {
        log.info("Cleaning up notifications older than {} days", daysToKeep);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        // Delete delivered and opened notifications
        notificationRepository.deleteByCreatedAtBeforeAndStatus(cutoffDate, NotificationStatus.DELIVERED);
        notificationRepository.deleteByCreatedAtBeforeAndStatus(cutoffDate, NotificationStatus.OPENED);
        
        log.info("Cleanup completed");
    }
    
    // Default method without parameter (keeps 30 days)
    public void cleanupOldNotifications() {
        cleanupOldNotifications(30);
    }
} 