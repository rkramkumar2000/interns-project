package com.ecommerce.payment.client;

import com.ecommerce.payment.dto.OrderDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service", fallback = OrderServiceFallback.class)
public interface OrderServiceClient {
    
    @GetMapping("/api/orders/{orderId}")
    @CircuitBreaker(name = "order-service", fallbackMethod = "getOrderByIdFallback")
    OrderDTO getOrderById(@PathVariable Long orderId);
    
    @PutMapping("/api/orders/{orderId}/payment")
    @CircuitBreaker(name = "order-service", fallbackMethod = "updatePaymentStatusFallback")
    OrderDTO updatePaymentStatus(@PathVariable Long orderId,
                                @RequestParam String paymentId,
                                @RequestParam String paymentStatus);
    
    // Fallback methods
    default OrderDTO getOrderByIdFallback(Long orderId, Exception ex) {
        // Return a minimal order DTO or throw custom exception
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderId);
        return order;
    }
    
    default OrderDTO updatePaymentStatusFallback(Long orderId, String paymentId, String paymentStatus, Exception ex) {
        // Log error and return minimal response
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderId);
        return order;
    }
} 