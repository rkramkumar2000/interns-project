package com.ecommerce.order.dto;

import com.ecommerce.order.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String userName;
    
    private List<OrderItemDTO> orderItems = new ArrayList<>();
    
    private AddressDTO deliveryAddress;
    private AddressDTO billingAddress;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveryDate;
    
    private OrderStatus orderStatus;
    
    // Payment information
    private Long paymentId;
    private String paymentMethod;
    private String paymentStatus;
    
    // Pricing
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    
    // Additional information
    private String orderNotes;
    private String trackingNumber;
    private String couponCode;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
} 