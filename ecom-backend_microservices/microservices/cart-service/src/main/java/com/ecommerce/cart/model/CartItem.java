package com.ecommerce.cart.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @ToString.Exclude
    private Cart cart;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private String productName;
    
    private String productImage;
    
    @Column(nullable = false)
    private BigDecimal productPrice;
    
    private BigDecimal specialPrice;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal totalPrice;
    
    // Store product details to avoid multiple service calls
    private String productBrand;
    
    private String productCategory;
    
    private Integer availableStock;
    
    // SKU for tracking
    private String productSku;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime addedAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Calculate total price for this item
    @PrePersist
    @PreUpdate
    public void calculateTotalPrice() {
        BigDecimal effectivePrice = specialPrice != null && specialPrice.compareTo(BigDecimal.ZERO) > 0 
                ? specialPrice : productPrice;
        
        if (effectivePrice != null && quantity != null) {
            BigDecimal itemTotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));
            
            if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountAmount = itemTotal.multiply(discount).divide(BigDecimal.valueOf(100));
                this.totalPrice = itemTotal.subtract(discountAmount);
            } else {
                this.totalPrice = itemTotal;
            }
        } else {
            this.totalPrice = BigDecimal.ZERO;
        }
    }
} 