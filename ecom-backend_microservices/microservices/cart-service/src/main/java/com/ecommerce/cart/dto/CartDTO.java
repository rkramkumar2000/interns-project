package com.ecommerce.cart.dto;

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
public class CartDTO {
    
    private Long cartId;
    
    private Long userId;
    
    private List<CartItemDTO> cartItems = new ArrayList<>();
    
    private BigDecimal totalPrice;
    
    private Integer totalItems;
    
    private BigDecimal totalDiscount;
    
    private BigDecimal subtotal;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String sessionId;
    
    private String status;
    
    private String couponCode;
    
    private BigDecimal couponDiscount;
    
    private Boolean active;
    
    // Additional calculated fields
    private BigDecimal shippingCost = BigDecimal.ZERO;
    
    private BigDecimal tax = BigDecimal.ZERO;
    
    private BigDecimal grandTotal;
    
    // Convenience methods
    public BigDecimal getGrandTotal() {
        if (grandTotal == null) {
            grandTotal = totalPrice
                    .add(shippingCost != null ? shippingCost : BigDecimal.ZERO)
                    .add(tax != null ? tax : BigDecimal.ZERO);
        }
        return grandTotal;
    }
} 