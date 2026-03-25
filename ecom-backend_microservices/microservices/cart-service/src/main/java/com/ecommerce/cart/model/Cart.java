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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;
    
    @Column(nullable = false)
    private Long userId;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<CartItem> cartItems = new ArrayList<>();
    
    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private Integer totalItems = 0;
    
    @Column(nullable = false)
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(name = "is_active")
    private Boolean active = true;
    
    // Session ID for anonymous carts
    private String sessionId;
    
    // Cart status
    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.ACTIVE;
    
    // Coupon code if applied
    private String couponCode;
    
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    
    // Helper methods
    public void addCartItem(CartItem item) {
        cartItems.add(item);
        item.setCart(this);
        recalculateTotals();
    }
    
    public void removeCartItem(CartItem item) {
        cartItems.remove(item);
        item.setCart(null);
        recalculateTotals();
    }
    
    public void recalculateTotals() {
        this.totalItems = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        this.subtotal = cartItems.stream()
                .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalDiscount = cartItems.stream()
                .map(CartItem::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(couponDiscount != null ? couponDiscount : BigDecimal.ZERO);
        
        this.totalPrice = subtotal.subtract(totalDiscount);
    }
    
    public enum CartStatus {
        ACTIVE,
        MERGED,
        ABANDONED,
        CHECKED_OUT
    }
} 