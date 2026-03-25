package com.ecommerce.product.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(length = 2000)
    private String description;
    
    private String image;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    private BigDecimal specialPrice;
    
    private Double discount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(nullable = false)
    private Long sellerId;
    
    private String brand;
    
    @Column(name = "is_active")
    private Boolean active = true;
    
    private Double rating = 0.0;
    
    private Integer reviewCount = 0;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Additional fields for microservices
    private String sku;
    
    private String tags;
    
    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity = 1;
    
    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity = 10;
} 