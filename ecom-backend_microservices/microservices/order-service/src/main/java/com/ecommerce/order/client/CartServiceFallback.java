package com.ecommerce.order.client;

import com.ecommerce.order.dto.CartDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CartServiceFallback implements CartServiceClient {
    
    @Override
    public CartDTO getCartByUserId(Long userId) {
        log.error("Fallback: Unable to get cart for user {}", userId);
        return createEmptyCart();
    }
    
    @Override
    public CartDTO getCartById(Long cartId) {
        log.error("Fallback: Unable to get cart with id {}", cartId);
        return createEmptyCart();
    }
    
    @Override
    public void clearCart(Long userId) {
        log.error("Fallback: Unable to clear cart for user {}", userId);
        // In a real scenario, might want to queue this operation for retry
    }
    
    private CartDTO createEmptyCart() {
        CartDTO cart = new CartDTO();
        cart.setActive(false);
        return cart;
    }
} 