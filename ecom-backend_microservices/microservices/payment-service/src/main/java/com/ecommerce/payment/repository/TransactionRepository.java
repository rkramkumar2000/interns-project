package com.ecommerce.payment.repository;

import com.ecommerce.payment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find by payment
    List<Transaction> findByPaymentPaymentIdOrderByCreatedAtDesc(Long paymentId);
    
    // Find by external transaction ID
    Optional<Transaction> findByExternalTransactionId(String externalTransactionId);
    
    // Find by transaction type
    List<Transaction> findByTransactionType(String transactionType);
    
    // Find transactions for a payment
    List<Transaction> findByPaymentPaymentIdAndTransactionType(Long paymentId, String transactionType);
} 