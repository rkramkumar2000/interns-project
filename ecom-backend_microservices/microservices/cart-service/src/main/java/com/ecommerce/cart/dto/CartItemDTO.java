package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    
    private Long cartItemId;
    
    private Long cartId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private String productName;
    
    private String productImage;
    
    private BigDecimal productPrice;
    
    private BigDecimal specialPrice;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50, message = "Quantity cannot exceed 50")
    private Integer quantity;
    
    private BigDecimal discount;
    
    private BigDecimal totalPrice;
    
    private String productBrand;
    
    private String productCategory;
    
    private Integer availableStock;
    
    private String productSku;
    
    private LocalDateTime addedAt;
    
    private LocalDateTime updatedAt;
    
    // Convenience methods
    public Boolean getInStock() {
        return availableStock != null && availableStock > 0;
    }
    
    public Boolean getStockWarning() {
        return availableStock != null && availableStock < quantity;
    }
    
    public String getStockMessage() {
        if (availableStock == null) {
            return "Stock information unavailable";
        } else if (availableStock == 0) {
            return "Out of stock";
        } else if (availableStock < quantity) {
            return "Only " + availableStock + " items available";
        } else if (availableStock <= 5) {
            return "Only " + availableStock + " left in stock";
        }
        return null;
    }
} 