package com.ecommerce.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    
    // Order reference
    @Column(nullable = false, unique = true)
    private Long orderId;
    
    @Column(nullable = false)
    private Long userId;
    
    // Payment method and provider
    @Column(nullable = false)
    private String paymentMethod; // CARD, PAYPAL, BANK_TRANSFER, etc.
    
    @Column(nullable = false)
    private String paymentProvider; // STRIPE, PAYPAL, etc.
    
    // Transaction details
    @Column(unique = true)
    private String transactionId; // External payment gateway transaction ID
    
    private String stripePaymentIntentId; // Stripe specific
    
    private String stripePaymentMethodId; // Stripe specific
    
    // Amount details
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String currency = "USD";
    
    @Column(precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    // Additional details
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    // Customer details (denormalized for reporting)
    private String customerEmail;
    private String customerName;
    
    // Billing address (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String billingAddress;
    
    // Error handling
    private String failureReason;
    private String failureCode;
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime completedAt;
    
    // Refund tracking
    private Integer refundCount = 0;
    private LocalDateTime lastRefundAt;
    
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