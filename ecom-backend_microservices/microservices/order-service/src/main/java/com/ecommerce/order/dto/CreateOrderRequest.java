package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Cart ID is required")
    private Long cartId;
    
    @NotNull(message = "Delivery address is required")
    private AddressDTO deliveryAddress;
    
    private AddressDTO billingAddress; // Optional, uses delivery address if not provided
    
    private String paymentMethod = "CARD"; // Default payment method
    
    private String orderNotes;
    
    private String couponCode;
    
    // Shipping option
    private String shippingMethod = "STANDARD";
    private BigDecimal shippingCost = BigDecimal.ZERO;
} 