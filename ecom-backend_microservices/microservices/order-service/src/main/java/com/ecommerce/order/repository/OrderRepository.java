package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Find orders by user
    Page<Order> findByUserIdOrderByOrderDateDesc(Long userId, Pageable pageable);
    
    List<Order> findByUserId(Long userId);
    
    // Find orders by status
    Page<Order> findByOrderStatus(OrderStatus status, Pageable pageable);
    
    List<Order> findByOrderStatusIn(List<OrderStatus> statuses);
    
    // Find orders by user and status
    Page<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus status, Pageable pageable);
    
    // Find orders by date range
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Page<Order> findOrdersBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    // Find orders by tracking number
    Optional<Order> findByTrackingNumber(String trackingNumber);
    
    // Count orders by status
    Long countByOrderStatus(OrderStatus status);
    
    // Count orders by user and status
    Long countByUserIdAndOrderStatus(Long userId, OrderStatus status);
    
    // Find recent orders
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.orderDate DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Find orders with payment pending
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'PENDING' AND o.createdAt < :cutoffTime")
    List<Order> findPendingPaymentOrders(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Statistics queries
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.orderStatus = 'DELIVERED'")
    Long countDeliveredOrdersByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.orderStatus = 'DELIVERED'")
    Double getTotalSpentByUserId(@Param("userId") Long userId);
} 