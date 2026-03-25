package com.ecommerce.cart.repository;

import com.ecommerce.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    Optional<CartItem> findByCartCartIdAndProductId(Long cartId, Long productId);
    
    List<CartItem> findByCartCartId(Long cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId AND ci.productId = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.productId = :productId AND ci.cart.status = 'ACTIVE'")
    List<CartItem> findActiveCartItemsByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    Long countItemsByCartId(@Param("cartId") Long cartId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    Integer getTotalQuantityByCartId(@Param("cartId") Long cartId);
    
    @Query("SELECT ci.productId, COUNT(ci) as count FROM CartItem ci GROUP BY ci.productId ORDER BY count DESC")
    List<Object[]> findMostPopularProducts();
} 