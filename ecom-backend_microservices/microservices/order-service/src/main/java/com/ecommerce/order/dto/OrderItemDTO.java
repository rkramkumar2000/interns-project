package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImage;
    private String productDescription;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal discount;
    private BigDecimal discountPercentage;
    private BigDecimal totalPrice;
    private String notes;
} 