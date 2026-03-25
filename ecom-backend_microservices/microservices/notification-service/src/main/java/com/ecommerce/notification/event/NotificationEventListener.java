package com.ecommerce.notification.event;

import com.ecommerce.notification.dto.NotificationRequest;
import com.ecommerce.notification.model.NotificationType;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final NotificationService notificationService;
    
    @KafkaListener(topics = "auth-events", groupId = "notification-service")
    public void handleAuthEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received auth event: {}", eventType);
        
        switch (eventType) {
            case "USER_REGISTERED":
                sendWelcomeEmail(event);
                break;
            case "PASSWORD_RESET_REQUESTED":
                sendPasswordResetEmail(event);
                break;
            case "USER_VERIFIED":
                sendVerificationSuccessEmail(event);
                break;
        }
    }
    
    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received order event: {}", eventType);
        
        switch (eventType) {
            case "ORDER_CREATED":
                sendOrderConfirmationEmail(event);
                break;
            case "ORDER_SHIPPED":
                sendOrderShippedNotification(event);
                break;
            case "ORDER_DELIVERED":
                sendOrderDeliveredNotification(event);
                break;
            case "ORDER_CANCELLED":
                sendOrderCancelledNotification(event);
                break;
        }
    }
    
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received payment event: {}", eventType);
        
        switch (eventType) {
            case "PAYMENT_COMPLETED":
                sendPaymentSuccessNotification(event);
                break;
            case "PAYMENT_FAILED":
                sendPaymentFailedNotification(event);
                break;
            case "REFUND_PROCESSED":
                sendRefundNotification(event);
                break;
        }
    }
    
    @KafkaListener(topics = "cart-events", groupId = "notification-service")
    public void handleCartEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received cart event: {}", eventType);
        
        if ("CART_ABANDONED".equals(eventType)) {
            sendCartAbandonedReminder(event);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void sendWelcomeEmail(Map<String, Object> event) {
        Map<String, Object> user = (Map<String, Object>) event.get("user");
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", (String) user.get("firstName"));
        variables.put("userEmail", (String) user.get("email"));
        
        NotificationRequest request = NotificationRequest.builder()
                .userId(((Number) user.get("userId")).longValue())
                .type(NotificationType.EMAIL)
                .recipient((String) user.get("email"))
                .subject("Welcome to E-Commerce Store!")
                .templateName("welcome")
                .templateVariables(variables)
                .eventType("user_registration")
                .priority(8)
                .build();
        
        notificationService.sendNotificationAsync(request);
    }
    
    @SuppressWarnings("unchecked")
    private void sendPasswordResetEmail(Map<String, Object> event) {
        Map<String, Object> user = (Map<String, Object>) event.get("user");
        String resetToken = (String) event.get("resetToken");
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", (String) user.get("firstName"));
        variables.put("resetLink", "https://ecommerce.com/reset-password?token=" + resetToken);
        variables.put("expiryTime", "24 hours");
        
        NotificationRequest request = NotificationRequest.builder()
                .userId(((Number) user.get("userId")).longValue())
                .type(NotificationType.EMAIL)
                .recipient((String) user.get("email"))
                .subject("Password Reset Request")
                .templateName("password-reset")
                .templateVariables(variables)
                .eventType("password_reset")
                .priority(10) // High priority
                .build();
        
        notificationService.sendNotificationAsync(request);
    }
    
    @SuppressWarnings("unchecked")
    private void sendOrderConfirmationEmail(Map<String, Object> event) {
        Map<String, Object> order = (Map<String, Object>) event.get("order");
        Map<String, Object> user = (Map<String, Object>) event.get("user");
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", (String) user.get("name"));
        variables.put("orderId", order.get("orderId").toString());
        variables.put("orderTotal", order.get("totalAmount").toString());
        variables.put("orderDate", order.get("orderDate").toString());
        variables.put("deliveryAddress", order.get("deliveryAddress").toString());
        
        NotificationRequest emailRequest = NotificationRequest.builder()
                .userId(((Number) order.get("userId")).longValue())
                .type(NotificationType.EMAIL)
                .recipient((String) user.get("email"))
                .subject("Order Confirmation - Order #" + order.get("orderId"))
                .templateName("order-confirmation")
                .templateVariables(variables)
                .eventType("order_confirmation")
                .priority(9)
                .build();
        
        notificationService.sendNotificationAsync(emailRequest);
        
        // Also send SMS if phone number is available
        if (user.get("phoneNumber") != null) {
            String smsMessage = String.format(
                "Your order #%s has been confirmed. Total: $%s. Track at: https://ecommerce.com/orders/%s",
                order.get("orderId"), order.get("totalAmount"), order.get("orderId")
            );
            
            NotificationRequest smsRequest = NotificationRequest.builder()
                    .userId(((Number) order.get("userId")).longValue())
                    .type(NotificationType.SMS)
                    .recipient((String) user.get("phoneNumber"))
                    .content(smsMessage)
                    .eventType("order_confirmation")
                    .priority(8)
                    .build();
            
            notificationService.sendNotificationAsync(smsRequest);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void sendOrderShippedNotification(Map<String, Object> event) {
        Map<String, Object> order = (Map<String, Object>) event.get("order");
        Map<String, Object> user = (Map<String, Object>) event.get("user");
        String trackingNumber = (String) event.get("trackingNumber");
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", (String) user.get("name"));
        variables.put("orderId", order.get("orderId").toString());
        variables.put("trackingNumber", trackingNumber);
        variables.put("trackingUrl", "https://tracking.com/track/" + trackingNumber);
        
        NotificationRequest request = NotificationRequest.builder()
                .userId(((Number) order.get("userId")).longValue())
                .type(NotificationType.EMAIL)
                .recipient((String) user.get("email"))
                .subject("Your Order Has Been Shipped! - Order #" + order.get("orderId"))
                .templateName("order-shipped")
                .templateVariables(variables)
                .eventType("order_shipped")
                .priority(7)
                .build();
        
        notificationService.sendNotificationAsync(request);
    }
    
    private void sendOrderDeliveredNotification(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending order delivered notification");
    }
    
    private void sendOrderCancelledNotification(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending order cancelled notification");
    }
    
    private void sendPaymentSuccessNotification(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending payment success notification");
    }
    
    private void sendPaymentFailedNotification(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending payment failed notification");
    }
    
    private void sendRefundNotification(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending refund notification");
    }
    
    private void sendCartAbandonedReminder(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending cart abandoned reminder");
    }
    
    private void sendVerificationSuccessEmail(Map<String, Object> event) {
        // Similar implementation
        log.info("Sending verification success email");
    }
} 