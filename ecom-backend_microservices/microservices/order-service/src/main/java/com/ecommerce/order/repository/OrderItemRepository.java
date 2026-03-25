package com.ecommerce.order.repository;

import com.ecommerce.order.model.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Find items by order
    List<OrderItem> findByOrderOrderId(Long orderId);
    
    // Find items by product
    List<OrderItem> findByProductId(Long productId);
    
    // Count how many times a product was ordered
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.productId = :productId")
    Long countByProductId(@Param("productId") Long productId);
    
    // Get total quantity sold for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalQuantitySoldByProductId(@Param("productId") Long productId);
    
    // Find most ordered products
    @Query("SELECT oi.productId, oi.productName, COUNT(oi) as orderCount, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi GROUP BY oi.productId, oi.productName " +
           "ORDER BY orderCount DESC")
    List<Object[]> findMostOrderedProducts(Pageable pageable);
} 