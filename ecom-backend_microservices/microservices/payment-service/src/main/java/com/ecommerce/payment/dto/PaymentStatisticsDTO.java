package com.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatisticsDTO {
    
    private Long totalPayments;
    private Long completedPayments;
    private Long failedPayments;
    private Long pendingPayments;
    private Long refundedPayments;
    
    private BigDecimal totalAmount;
    private BigDecimal completedAmount;
    private BigDecimal refundedAmount;
    private BigDecimal averagePaymentAmount;
    
    private Map<String, Long> paymentsByMethod;
    private Map<String, BigDecimal> amountByMethod;
    private Map<String, Long> paymentsByStatus;
    private Map<String, BigDecimal> dailyRevenue;
    
    private Double successRate;
    private Double failureRate;
} 