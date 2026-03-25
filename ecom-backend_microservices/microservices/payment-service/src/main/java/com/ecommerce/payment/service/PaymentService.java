package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    
    // Process payment
    PaymentResponse processPayment(CreatePaymentRequest request);
    
    // Get payment by ID
    PaymentDTO getPaymentById(Long paymentId);
    
    // Get payment by order ID
    PaymentDTO getPaymentByOrderId(Long orderId);
    
    // Get payments by user
    Page<PaymentDTO> getPaymentsByUserId(Long userId, Pageable pageable);
    
    // Update payment status
    PaymentDTO updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId);
    
    // Process refund
    PaymentDTO processRefund(RefundRequest refundRequest);
    
    // Handle Stripe webhook
    void handleStripeWebhook(String payload, String sigHeader);
    
    // Get payment by transaction ID
    PaymentDTO getPaymentByTransactionId(String transactionId);
    
    // Cancel payment
    PaymentDTO cancelPayment(Long paymentId);
    
    // Get payments by status
    Page<PaymentDTO> getPaymentsByStatus(PaymentStatus status, Pageable pageable);
    
    // Reconciliation
    List<PaymentDTO> getPaymentsForReconciliation(LocalDateTime startDate, LocalDateTime endDate, PaymentStatus status);
    
    // Check payment status with provider
    PaymentDTO syncPaymentStatus(Long paymentId);
    
    // Expire old pending payments
    void expirePendingPayments();
    
    // Get payment statistics
    PaymentStatisticsDTO getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);
} 