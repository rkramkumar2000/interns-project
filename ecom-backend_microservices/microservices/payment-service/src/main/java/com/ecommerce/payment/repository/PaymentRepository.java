package com.ecommerce.payment.repository;

import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Find by order
    Optional<Payment> findByOrderId(Long orderId);
    
    // Find by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
    
    // Find by Stripe Payment Intent ID
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);
    
    // Find by user
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Payment> findByUserId(Long userId);
    
    // Find by status
    Page<Payment> findByPaymentStatus(PaymentStatus status, Pageable pageable);
    
    List<Payment> findByPaymentStatusIn(List<PaymentStatus> statuses);
    
    // Find pending payments older than specified time
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findPendingPaymentsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find payments for reconciliation
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.paymentStatus = :status")
    List<Payment> findPaymentsForReconciliation(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") PaymentStatus status
    );
    
    // Statistics queries
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.userId = :userId AND p.paymentStatus = 'COMPLETED'")
    Long countCompletedPaymentsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalAmountByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = :status AND p.createdAt BETWEEN :startDate AND :endDate")
    Long countPaymentsByStatusAndDateRange(
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
} 