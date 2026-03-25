package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartDTO;
import com.ecommerce.cart.dto.CartDTO;
import com.ecommerce.cart.dto.CartItemDTO;

public interface CartService {
    
    CartDTO getCartByUserId(Long userId);
    
    CartDTO getCartBySessionId(String sessionId);
    
    CartDTO getCartById(Long cartId);
    
    CartDTO addItemToCart(Long userId, AddToCartDTO addToCartDTO);
    
    CartDTO addItemToAnonymousCart(String sessionId, AddToCartDTO addToCartDTO);
    
    CartDTO updateCartItemQuantity(Long userId, Long productId, Integer quantity);
    
    CartDTO updateAnonymousCartItemQuantity(String sessionId, Long productId, Integer quantity);
    
    void removeItemFromCart(Long userId, Long productId);
    
    void removeItemFromAnonymousCart(String sessionId, Long productId);
    
    void clearCart(Long userId);
    
    void clearAnonymousCart(String sessionId);
    
    CartDTO mergeAnonymousCart(String sessionId, Long userId);
    
    CartDTO applyCoupon(Long userId, String couponCode);
    
    void removeCoupon(Long userId);
    
    CartDTO checkoutCart(Long userId);
    
    void markCartAsAbandoned(Long cartId);
    
    void cleanupAbandonedCarts();
    
    CartItemDTO getCartItem(Long cartId, Long productId);
    
    Integer getCartItemCount(Long userId);
    
    Boolean isProductInCart(Long userId, Long productId);
} 