package com.ecommerce.order.controller;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.security.JwtAuthenticationToken;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for managing orders")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = getCurrentUserId();
        OrderDTO order = orderService.createOrder(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
    
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        
        // Verify user can access this order
        Long currentUserId = getCurrentUserId();
        if (!order.getUserId().equals(currentUserId) && !hasAdminRole()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/user/my-orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<Page<OrderDTO>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by user ID (Admin only)")
    public ResponseEntity<Page<OrderDTO>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by status (Admin only)")
    public ResponseEntity<Page<OrderDTO>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin only)")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderDTO order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderDTO> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String reason) {
        // Verify user owns this order
        OrderDTO order = orderService.getOrderById(orderId);
        Long currentUserId = getCurrentUserId();
        
        if (!order.getUserId().equals(currentUserId) && !hasAdminRole()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        OrderDTO cancelledOrder = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(cancelledOrder);
    }
    
    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Get order by tracking number")
    public ResponseEntity<OrderDTO> getOrderByTrackingNumber(@PathVariable String trackingNumber) {
        OrderDTO order = orderService.getOrderByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin only)")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders between dates (Admin only)")
    public ResponseEntity<Page<OrderDTO>> getOrdersBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getOrdersBetweenDates(startDate, endDate, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get order statistics (Admin only)")
    public ResponseEntity<OrderStatisticsDTO> getOrderStatistics() {
        OrderStatisticsDTO statistics = orderService.getOrderStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/user/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's order statistics")
    public ResponseEntity<UserOrderStatisticsDTO> getUserOrderStatistics() {
        Long userId = getCurrentUserId();
        UserOrderStatisticsDTO statistics = orderService.getUserOrderStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
    
    @PutMapping("/{orderId}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update payment status (Admin only)")
    public ResponseEntity<OrderDTO> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam String paymentId,
            @RequestParam String paymentStatus) {
        OrderDTO order = orderService.updatePaymentStatus(orderId, paymentId, paymentStatus);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/user/product/{productId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if user has ordered a product")
    public ResponseEntity<Boolean> hasUserOrderedProduct(@PathVariable Long productId) {
        Long userId = getCurrentUserId();
        boolean hasOrdered = orderService.hasUserOrderedProduct(userId, productId);
        return ResponseEntity.ok(hasOrdered);
    }
    
    @GetMapping("/user/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user's recent orders")
    public ResponseEntity<List<OrderDTO>> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        Long userId = getCurrentUserId();
        List<OrderDTO> recentOrders = orderService.getRecentOrdersByUserId(userId, limit);
        return ResponseEntity.ok(recentOrders);
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getUserId();
        }
        throw new RuntimeException("User not authenticated");
    }
    
    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
} 