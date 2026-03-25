package com.ecommerce.payment.dto;

import com.ecommerce.payment.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String paymentMethod;
    private String paymentProvider;
    private String transactionId;
    private String stripePaymentIntentId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal refundedAmount;
    private PaymentStatus paymentStatus;
    private String description;
    private String customerEmail;
    private String customerName;
    private String failureReason;
    private String failureCode;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    
    private Integer refundCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRefundAt;
} 