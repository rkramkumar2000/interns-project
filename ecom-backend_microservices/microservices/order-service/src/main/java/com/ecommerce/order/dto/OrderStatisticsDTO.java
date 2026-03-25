package com.ecommerce.order.dto;

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
public class OrderStatisticsDTO {
    
    private Long totalOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    private Long refundedOrders;
    
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    
    private Map<String, Long> ordersByStatus;
    private Map<String, BigDecimal> revenueByMonth;
    private Map<String, Long> ordersByMonth;
} 