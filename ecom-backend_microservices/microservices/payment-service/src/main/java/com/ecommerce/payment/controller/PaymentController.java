package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.security.JwtAuthenticationToken;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for managing payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Process a new payment")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody CreatePaymentRequest request) {
        // TODO: Add order ownership verification
        
        PaymentResponse response = paymentService.processPayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long paymentId) {
        PaymentDTO payment = paymentService.getPaymentById(paymentId);
        
        // Verify user can access this payment
        Long currentUserId = getCurrentUserId();
        if (!payment.getUserId().equals(currentUserId) && !hasAdminRole()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<PaymentDTO> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);
        
        // Verify user can access this payment
        Long currentUserId = getCurrentUserId();
        if (!payment.getUserId().equals(currentUserId) && !hasAdminRole()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/user/my-payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's payments")
    public ResponseEntity<Page<PaymentDTO>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentDTO> payments = paymentService.getPaymentsByUserId(userId, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment by transaction ID")
    public ResponseEntity<PaymentDTO> getPaymentByTransactionId(@PathVariable String transactionId) {
        PaymentDTO payment = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(payment);
    }
    
    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a payment")
    public ResponseEntity<PaymentDTO> cancelPayment(@PathVariable Long paymentId) {
        // Verify user owns this payment
        PaymentDTO payment = paymentService.getPaymentById(paymentId);
        Long currentUserId = getCurrentUserId();
        
        if (!payment.getUserId().equals(currentUserId) && !hasAdminRole()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        PaymentDTO cancelledPayment = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(cancelledPayment);
    }
    
    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process a refund")
    public ResponseEntity<PaymentDTO> processRefund(@Valid @RequestBody RefundRequest refundRequest) {
        PaymentDTO refundedPayment = paymentService.processRefund(refundRequest);
        return ResponseEntity.ok(refundedPayment);
    }
    
    @PostMapping("/webhook/stripe")
    @Operation(summary = "Handle Stripe webhook")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{paymentId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync payment status with provider")
    public ResponseEntity<PaymentDTO> syncPaymentStatus(@PathVariable Long paymentId) {
        PaymentDTO payment = paymentService.syncPaymentStatus(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payments by status")
    public ResponseEntity<Page<PaymentDTO>> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentDTO> payments = paymentService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payments for reconciliation")
    public ResponseEntity<List<PaymentDTO>> getPaymentsForReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam PaymentStatus status) {
        List<PaymentDTO> payments = paymentService.getPaymentsForReconciliation(startDate, endDate, status);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment statistics")
    public ResponseEntity<PaymentStatisticsDTO> getPaymentStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        PaymentStatisticsDTO statistics = paymentService.getPaymentStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getUserId();
        }
        throw new RuntimeException("User not authenticated");
    }
    
    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
} 