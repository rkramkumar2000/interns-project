package com.ecommerce.order.client;

import com.ecommerce.order.dto.CartDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "cart-service", fallback = CartServiceFallback.class)
public interface CartServiceClient {
    
    @GetMapping("/api/cart/user/{userId}")
    @CircuitBreaker(name = "cart-service", fallbackMethod = "getCartByUserIdFallback")
    CartDTO getCartByUserId(@PathVariable Long userId);
    
    @GetMapping("/api/cart/{cartId}")
    @CircuitBreaker(name = "cart-service", fallbackMethod = "getCartByIdFallback")
    CartDTO getCartById(@PathVariable Long cartId);
    
    @PutMapping("/api/cart/user/{userId}/clear")
    @CircuitBreaker(name = "cart-service", fallbackMethod = "clearCartFallback")
    void clearCart(@PathVariable Long userId);
    
    // Fallback methods
    default CartDTO getCartByUserIdFallback(Long userId, Exception ex) {
        // Return empty cart or throw custom exception
        return new CartDTO();
    }
    
    default CartDTO getCartByIdFallback(Long cartId, Exception ex) {
        // Return empty cart or throw custom exception
        return new CartDTO();
    }
    
    default void clearCartFallback(Long userId, Exception ex) {
        // Log error, might throw custom exception
    }
} 