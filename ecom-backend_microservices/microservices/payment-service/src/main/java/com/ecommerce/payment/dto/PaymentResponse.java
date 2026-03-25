package com.ecommerce.payment.dto;

import com.ecommerce.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    
    private Long paymentId;
    private Long orderId;
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String message;
    
    // For 3D Secure or additional authentication
    private boolean requiresAction;
    private String actionUrl;
    private String clientSecret; // For Stripe Payment Intent
    
    // Error details
    private String errorCode;
    private String errorMessage;
    
    // Webhook URL for status updates
    private String webhookUrl;
} 