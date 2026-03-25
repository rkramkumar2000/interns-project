package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    
    // Create order
    OrderDTO createOrder(Long userId, CreateOrderRequest request);
    
    // Get order by ID
    OrderDTO getOrderById(Long orderId);
    
    // Get orders by user
    Page<OrderDTO> getOrdersByUserId(Long userId, Pageable pageable);
    
    List<OrderDTO> getAllOrdersByUserId(Long userId);
    
    // Get orders by status
    Page<OrderDTO> getOrdersByStatus(OrderStatus status, Pageable pageable);
    
    // Update order status
    OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
    
    // Cancel order
    OrderDTO cancelOrder(Long orderId, String reason);
    
    // Get order by tracking number
    OrderDTO getOrderByTrackingNumber(String trackingNumber);
    
    // Admin operations
    Page<OrderDTO> getAllOrders(Pageable pageable);
    
    Page<OrderDTO> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Order statistics
    OrderStatisticsDTO getOrderStatistics();
    
    UserOrderStatisticsDTO getUserOrderStatistics(Long userId);
    
    // Process payment update
    OrderDTO updatePaymentStatus(Long orderId, String paymentId, String paymentStatus);
    
    // Check if user has ordered a product
    boolean hasUserOrderedProduct(Long userId, Long productId);
    
    // Get recent orders
    List<OrderDTO> getRecentOrdersByUserId(Long userId, int limit);
} 