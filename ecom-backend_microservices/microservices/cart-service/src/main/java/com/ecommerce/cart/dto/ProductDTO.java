package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    
    private Long productId;
    
    private String productName;
    
    private String description;
    
    private String image;
    
    private Integer quantity;
    
    private BigDecimal price;
    
    private Double discount;
    
    private BigDecimal specialPrice;
    
    private String brand;
    
    private Long categoryId;
    
    private String categoryName;
    
    private String sku;
    
    private Boolean active;
    
    private Boolean inStock;
    
    private BigDecimal finalPrice;
} 