package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckDTO {
    
    private Long productId;
    private Integer requestedQuantity;
    private Integer availableQuantity;
    private boolean inStock;
    private String message;
    
    // Static factory method for out of stock scenario
    public static InventoryCheckDTO outOfStock(Long productId) {
        return InventoryCheckDTO.builder()
                .productId(productId)
                .requestedQuantity(0)
                .availableQuantity(0)
                .inStock(false)
                .message("Product out of stock")
                .build();
    }
} 