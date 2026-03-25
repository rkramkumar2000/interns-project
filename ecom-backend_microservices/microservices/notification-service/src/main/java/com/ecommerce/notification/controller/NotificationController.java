package com.ecommerce.notification.controller;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.model.NotificationStatus;
import com.ecommerce.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Endpoints for managing notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping
    @Operation(summary = "Send a notification")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/async")
    @Operation(summary = "Send a notification asynchronously")
    public ResponseEntity<NotificationResponse> sendNotificationAsync(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotificationAsync(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/bulk")
    @Operation(summary = "Send bulk notifications")
    public ResponseEntity<List<NotificationResponse>> sendBulkNotifications(
            @Valid @RequestBody List<NotificationRequest> requests) {
        List<NotificationResponse> responses = notificationService.sendBulkNotifications(requests);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable Long notificationId) {
        NotificationResponse response = notificationService.getNotification(notificationId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("ASC") 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get notifications by status")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByStatus(
            @PathVariable NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> notifications = notificationService.getNotificationsByStatus(status, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    @PatchMapping("/{notificationId}/status")
    @Operation(summary = "Update notification status")
    public ResponseEntity<Void> updateNotificationStatus(
            @PathVariable Long notificationId,
            @RequestParam NotificationStatus status) {
        notificationService.updateNotificationStatus(notificationId, status);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{notificationId}/delivered")
    @Operation(summary = "Mark notification as delivered")
    public ResponseEntity<Void> markAsDelivered(@PathVariable Long notificationId) {
        notificationService.markAsDelivered(notificationId);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{notificationId}/failed")
    @Operation(summary = "Mark notification as failed")
    public ResponseEntity<Void> markAsFailed(
            @PathVariable Long notificationId,
            @RequestParam String reason) {
        notificationService.markAsFailed(notificationId, reason);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/retry-failed")
    @Operation(summary = "Retry failed notifications")
    public ResponseEntity<Void> retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/process-scheduled")
    @Operation(summary = "Process scheduled notifications")
    public ResponseEntity<Void> processScheduledNotifications() {
        notificationService.processScheduledNotifications();
        return ResponseEntity.accepted().build();
    }
    
    @GetMapping("/statistics/user/{userId}")
    @Operation(summary = "Get user notification statistics")
    public ResponseEntity<Map<String, Long>> getUserStatistics(@PathVariable Long userId) {
        Map<String, Long> stats = notificationService.getNotificationStatistics(userId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/statistics/system")
    @Operation(summary = "Get system notification statistics")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        Map<String, Object> stats = notificationService.getSystemStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "Clean up old notifications")
    public ResponseEntity<Void> cleanupOldNotifications(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        notificationService.cleanupOldNotifications(daysToKeep);
        return ResponseEntity.accepted().build();
    }
} 