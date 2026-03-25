package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    
    private Long productId;
    
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    private String productName;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    private String image;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    private BigDecimal specialPrice;
    
    @Min(value = 0, message = "Discount cannot be negative")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    private Double discount;
    
    private Long categoryId;
    private String categoryName;
    
    @NotNull(message = "Seller ID is required")
    private Long sellerId;
    
    private String brand;
    
    private Boolean active;
    
    private Double rating;
    
    private Integer reviewCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String sku;
    
    private String tags;
    
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minOrderQuantity;
    
    @Min(value = 1, message = "Maximum order quantity must be at least 1")
    private Integer maxOrderQuantity;
    
    // Calculated fields
    private Boolean inStock;
    private BigDecimal finalPrice;
} 