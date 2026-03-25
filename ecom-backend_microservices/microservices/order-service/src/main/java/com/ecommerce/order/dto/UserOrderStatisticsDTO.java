package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrderStatisticsDTO {
    
    private Long userId;
    private Long totalOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    private Long activeOrders; // Pending, Processing, Shipped
    
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    
    private String memberSince;
    private String lastOrderDate;
} 