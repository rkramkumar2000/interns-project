package com.ecommerce.cart.repository;

import com.ecommerce.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    Optional<Cart> findByUserIdAndStatusAndActiveTrue(Long userId, Cart.CartStatus status);
    
    Optional<Cart> findBySessionIdAndStatusAndActiveTrue(String sessionId, Cart.CartStatus status);
    
    List<Cart> findByUserIdAndActiveTrue(Long userId);
    
    List<Cart> findByStatusAndActiveTrueAndUpdatedAtBefore(Cart.CartStatus status, LocalDateTime dateTime);
    
    @Modifying
    @Query("UPDATE Cart c SET c.status = :status WHERE c.cartId = :cartId")
    int updateCartStatus(@Param("cartId") Long cartId, @Param("status") Cart.CartStatus status);
    
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems WHERE c.cartId = :cartId AND c.active = true")
    Optional<Cart> findByIdWithItems(@Param("cartId") Long cartId);
    
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems WHERE c.userId = :userId AND c.status = :status AND c.active = true")
    Optional<Cart> findActiveCartByUserIdWithItems(@Param("userId") Long userId, @Param("status") Cart.CartStatus status);
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.userId = :userId AND c.status = 'ACTIVE' AND c.active = true")
    Long countActiveCartsByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Cart c SET c.active = false WHERE c.status = 'ABANDONED' AND c.updatedAt < :beforeDate")
    int deactivateAbandonedCarts(@Param("beforeDate") LocalDateTime beforeDate);
    
    @Query("SELECT SUM(c.totalPrice) FROM Cart c WHERE c.status = 'CHECKED_OUT' AND c.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalSalesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 