package com.ecommerce.payment.service;

import com.ecommerce.payment.client.OrderServiceClient;
import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.exception.PaymentException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.model.Transaction;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final StripeService stripeService;
    private final OrderServiceClient orderServiceClient;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;
    
    private static final String PAYMENT_TOPIC = "payment-events";
    
    @Override
    public PaymentResponse processPayment(CreatePaymentRequest request) {
        try {
            // Verify order exists and amount matches
            OrderDTO order = orderServiceClient.getOrderById(request.getOrderId());
            if (order == null || !order.isAvailable()) {
                throw new PaymentException("Order not found or unavailable");
            }
            
            // Check if payment already exists for this order
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
            if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new PaymentException("Payment already completed for this order");
            }
            
            // Create or update payment record
            Payment payment = existingPayment.orElse(new Payment());
            payment.setOrderId(request.getOrderId());
            payment.setUserId(order.getUserId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setPaymentProvider("STRIPE");
            payment.setCustomerEmail(request.getCustomerEmail());
            payment.setCustomerName(request.getCustomerName());
            payment.setDescription(request.getDescription());
            payment.setPaymentStatus(PaymentStatus.PROCESSING);
            
            if (request.getBillingAddress() != null) {
                payment.setBillingAddress(convertAddressToJson(request.getBillingAddress()));
            }
            
            payment = paymentRepository.save(payment);
            
            // Create transaction record
            Transaction transaction = createTransaction(payment, "CHARGE", request.getAmount());
            
            try {
                // Process payment with Stripe
                PaymentIntent paymentIntent = stripeService.createPaymentIntent(request);
                
                // Update payment with Stripe details
                payment.setStripePaymentIntentId(paymentIntent.getId());
                payment.setTransactionId(paymentIntent.getId());
                
                // Update transaction with response
                transaction.setExternalTransactionId(paymentIntent.getId());
                transaction.setResponseData(paymentIntent.toJson());
                
                PaymentResponse response = PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .orderId(payment.getOrderId())
                    .transactionId(paymentIntent.getId())
                    .status(mapStripeStatusToPaymentStatus(paymentIntent.getStatus()))
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .clientSecret(paymentIntent.getClientSecret())
                    .build();
                
                // Check if payment requires additional action
                if ("requires_action".equals(paymentIntent.getStatus()) || 
                    "requires_payment_method".equals(paymentIntent.getStatus())) {
                    response.setRequiresAction(true);
                    response.setMessage("Additional authentication required");
                }
                
                payment.setPaymentStatus(response.getStatus());
                paymentRepository.save(payment);
                
                transaction.setStatus(response.getStatus());
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
                
                // Publish payment initiated event
                publishPaymentEvent("PAYMENT_INITIATED", payment);
                
                return response;
                
            } catch (Exception e) {
                log.error("Error processing payment with Stripe", e);
                
                // Update payment status to failed
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                paymentRepository.save(payment);
                
                // Update transaction
                transaction.setStatus(PaymentStatus.FAILED);
                transaction.setErrorMessage(e.getMessage());
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
                
                // Publish payment failed event
                publishPaymentEvent("PAYMENT_FAILED", payment);
                
                throw new PaymentException("Payment processing failed: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error processing payment", e);
            throw new PaymentException("Failed to process payment: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return convertToDTO(payment);
    }
    
    @Override
    public PaymentDTO getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        return convertToDTO(payment);
    }
    
    @Override
    public Page<PaymentDTO> getPaymentsByUserId(Long userId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return payments.map(this::convertToDTO);
    }
    
    @Override
    public PaymentDTO updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        payment.setPaymentStatus(status);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }
        
        if (status == PaymentStatus.COMPLETED) {
            payment.setCompletedAt(LocalDateTime.now());
            // Update order status
            orderServiceClient.updatePaymentStatus(payment.getOrderId(), 
                payment.getPaymentId().toString(), "COMPLETED");
        }
        
        Payment updatedPayment = paymentRepository.save(payment);
        
        // Publish status update event
        publishPaymentEvent("PAYMENT_STATUS_UPDATED", updatedPayment);
        
        return convertToDTO(updatedPayment);
    }
    
    private Transaction createTransaction(Payment payment, String type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setPayment(payment);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(payment.getCurrency());
        transaction.setStatus(PaymentStatus.PENDING);
        return transactionRepository.save(transaction);
    }
    
    private PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_payment_method", "requires_confirmation", "requires_action" -> PaymentStatus.PENDING;
            case "canceled" -> PaymentStatus.CANCELLED;
            case "failed" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
    
    private String convertAddressToJson(AddressDTO address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (Exception e) {
            log.error("Error converting address to JSON", e);
            return "{}";
        }
    }
    
    private void publishPaymentEvent(String eventType, Payment payment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("paymentId", payment.getPaymentId());
            event.put("orderId", payment.getOrderId());
            event.put("userId", payment.getUserId());
            event.put("amount", payment.getAmount());
            event.put("status", payment.getPaymentStatus().toString());
            event.put("timestamp", LocalDateTime.now());
            
            kafkaTemplate.send(PAYMENT_TOPIC, event);
            log.info("Published {} event for payment {}", eventType, payment.getPaymentId());
        } catch (Exception e) {
            log.error("Error publishing payment event", e);
        }
    }
    
    private PaymentDTO convertToDTO(Payment payment) {
        return modelMapper.map(payment, PaymentDTO.class);
    }
    
    @Override
    public PaymentDTO processRefund(RefundRequest refundRequest) {
        Payment payment = paymentRepository.findById(refundRequest.getPaymentId())
            .orElseThrow(() -> new PaymentNotFoundException(refundRequest.getPaymentId()));
        
        // Validate refund amount
        BigDecimal totalRefundable = payment.getAmount().subtract(payment.getRefundedAmount());
        if (refundRequest.getAmount().compareTo(totalRefundable) > 0) {
            throw new PaymentException("Refund amount exceeds refundable amount");
        }
        
        // Create refund transaction
        Transaction refundTransaction = createTransaction(payment, "REFUND", refundRequest.getAmount());
        
        try {
            // Process refund with Stripe
            com.stripe.model.Refund stripeRefund = stripeService.createRefund(
                payment.getStripePaymentIntentId(),
                refundRequest.getAmount(),
                refundRequest.getReason()
            );
            
            // Update refund transaction
            refundTransaction.setExternalTransactionId(stripeRefund.getId());
            refundTransaction.setStatus(PaymentStatus.COMPLETED);
            refundTransaction.setResponseData(stripeRefund.toJson());
            refundTransaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(refundTransaction);
            
            // Update payment
            payment.setRefundedAmount(payment.getRefundedAmount().add(refundRequest.getAmount()));
            payment.setRefundCount(payment.getRefundCount() + 1);
            payment.setLastRefundAt(LocalDateTime.now());
            
            if (payment.getRefundedAmount().compareTo(payment.getAmount()) == 0) {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            
            Payment updatedPayment = paymentRepository.save(payment);
            
            // Update order status
            orderServiceClient.updatePaymentStatus(payment.getOrderId(),
                payment.getPaymentId().toString(), payment.getPaymentStatus().toString());
            
            // Publish refund event
            publishPaymentEvent("PAYMENT_REFUNDED", updatedPayment);
            
            return convertToDTO(updatedPayment);
            
        } catch (Exception e) {
            log.error("Error processing refund", e);
            
            refundTransaction.setStatus(PaymentStatus.FAILED);
            refundTransaction.setErrorMessage(e.getMessage());
            refundTransaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(refundTransaction);
            
            throw new PaymentException("Refund processing failed: " + e.getMessage());
        }
    }
    
    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        try {
            Event event = stripeService.constructEvent(payload, sigHeader);
            
            log.info("Handling Stripe webhook event: {}", event.getType());
            
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "payment_intent.canceled":
                    handlePaymentIntentCanceled(event);
                    break;
                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;
                default:
                    log.warn("Unhandled webhook event type: {}", event.getType());
            }
            
        } catch (Exception e) {
            log.error("Error handling Stripe webhook", e);
            throw new PaymentException("Webhook processing failed: " + e.getMessage());
        }
    }
    
    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (paymentIntent != null) {
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                .orElse(null);
            
            if (payment != null) {
                updatePaymentStatus(payment.getPaymentId(), PaymentStatus.COMPLETED, paymentIntent.getId());
            }
        }
    }
    
    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (paymentIntent != null) {
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                .orElse(null);
            
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment failed");
                if (paymentIntent.getLastPaymentError() != null) {
                    payment.setFailureCode(paymentIntent.getLastPaymentError().getCode());
                    payment.setFailureReason(paymentIntent.getLastPaymentError().getMessage());
                }
                paymentRepository.save(payment);
                
                // Update order status
                orderServiceClient.updatePaymentStatus(payment.getOrderId(),
                    payment.getPaymentId().toString(), "FAILED");
                
                publishPaymentEvent("PAYMENT_FAILED", payment);
            }
        }
    }
    
    private void handlePaymentIntentCanceled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject().orElse(null);
        
        if (paymentIntent != null) {
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                .orElse(null);
            
            if (payment != null) {
                updatePaymentStatus(payment.getPaymentId(), PaymentStatus.CANCELLED, paymentIntent.getId());
            }
        }
    }
    
    private void handleChargeRefunded(Event event) {
        // Handle refund webhook
        log.info("Charge refunded event received");
    }
    
    @Override
    public PaymentDTO getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with transaction ID: " + transactionId));
        return convertToDTO(payment);
    }
    
    @Override
    public PaymentDTO cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot cancel completed payment");
        }
        
        try {
            // Cancel with Stripe
            if (payment.getStripePaymentIntentId() != null) {
                stripeService.cancelPaymentIntent(payment.getStripePaymentIntentId());
            }
            
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            Payment cancelledPayment = paymentRepository.save(payment);
            
            // Update order status
            orderServiceClient.updatePaymentStatus(payment.getOrderId(),
                payment.getPaymentId().toString(), "CANCELLED");
            
            publishPaymentEvent("PAYMENT_CANCELLED", cancelledPayment);
            
            return convertToDTO(cancelledPayment);
            
        } catch (Exception e) {
            log.error("Error cancelling payment", e);
            throw new PaymentException("Failed to cancel payment: " + e.getMessage());
        }
    }
    
    @Override
    public Page<PaymentDTO> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByPaymentStatus(status, pageable);
        return payments.map(this::convertToDTO);
    }
    
    @Override
    public List<PaymentDTO> getPaymentsForReconciliation(LocalDateTime startDate, LocalDateTime endDate, PaymentStatus status) {
        List<Payment> payments = paymentRepository.findPaymentsForReconciliation(startDate, endDate, status);
        return payments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public PaymentDTO syncPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        if (payment.getStripePaymentIntentId() == null) {
            throw new PaymentException("No Stripe payment intent ID found");
        }
        
        try {
            PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(payment.getStripePaymentIntentId());
            PaymentStatus newStatus = mapStripeStatusToPaymentStatus(paymentIntent.getStatus());
            
            if (payment.getPaymentStatus() != newStatus) {
                payment.setPaymentStatus(newStatus);
                payment = paymentRepository.save(payment);
                
                // Update order if needed
                if (newStatus == PaymentStatus.COMPLETED || newStatus == PaymentStatus.FAILED) {
                    orderServiceClient.updatePaymentStatus(payment.getOrderId(),
                        payment.getPaymentId().toString(), newStatus.toString());
                }
            }
            
            return convertToDTO(payment);
            
        } catch (Exception e) {
            log.error("Error syncing payment status", e);
            throw new PaymentException("Failed to sync payment status: " + e.getMessage());
        }
    }
    
    @Override
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void expirePendingPayments() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // 24 hour expiry
        List<Payment> pendingPayments = paymentRepository.findPendingPaymentsOlderThan(cutoffTime);
        
        for (Payment payment : pendingPayments) {
            payment.setPaymentStatus(PaymentStatus.EXPIRED);
            payment.setFailureReason("Payment expired after 24 hours");
            paymentRepository.save(payment);
            
            // Cancel with Stripe if applicable
            if (payment.getStripePaymentIntentId() != null) {
                try {
                    stripeService.cancelPaymentIntent(payment.getStripePaymentIntentId());
                } catch (Exception e) {
                    log.error("Error cancelling expired payment intent", e);
                }
            }
            
            publishPaymentEvent("PAYMENT_EXPIRED", payment);
        }
        
        log.info("Expired {} pending payments", pendingPayments.size());
    }
    
    @Override
    public PaymentStatisticsDTO getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        // Calculate statistics
        Long totalPayments = paymentRepository.count();
        Long completedPayments = paymentRepository.countPaymentsByStatusAndDateRange(
            PaymentStatus.COMPLETED, startDate, endDate);
        Long failedPayments = paymentRepository.countPaymentsByStatusAndDateRange(
            PaymentStatus.FAILED, startDate, endDate);
        
        BigDecimal totalRevenue = paymentRepository.getTotalRevenueBetweenDates(startDate, endDate);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        
        Double successRate = totalPayments > 0 ? 
            (completedPayments.doubleValue() / totalPayments.doubleValue()) * 100 : 0.0;
        
        return PaymentStatisticsDTO.builder()
            .totalPayments(totalPayments)
            .completedPayments(completedPayments)
            .failedPayments(failedPayments)
            .totalAmount(totalRevenue)
            .successRate(successRate)
            .build();
    }
} 