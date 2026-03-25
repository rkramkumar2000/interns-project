package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartDTO;
import com.ecommerce.cart.dto.CartDTO;
import com.ecommerce.cart.dto.CartItemDTO;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.cart.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "Endpoints for managing shopping cart")
public class CartController {
    
    private final CartService cartService;
    
    @GetMapping("/user/current")
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<CartDTO> getCurrentUserCart() {
        Long userId = getCurrentUserId();
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get cart by user ID")
    public ResponseEntity<CartDTO> getCartByUserId(@PathVariable Long userId) {
        // Verify user can only access their own cart
        Long currentUserId = getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }
    
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get cart by session ID (anonymous users)")
    public ResponseEntity<CartDTO> getCartBySessionId(@PathVariable String sessionId) {
        CartDTO cart = cartService.getCartBySessionId(sessionId);
        return ResponseEntity.ok(cart);
    }
    
    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart by cart ID")
    public ResponseEntity<CartDTO> getCartById(@PathVariable Long cartId) {
        CartDTO cart = cartService.getCartById(cartId);
        return ResponseEntity.ok(cart);
    }
    
    @PostMapping("/user/{userId}/items")
    @Operation(summary = "Add item to user's cart")
    public ResponseEntity<CartDTO> addItemToCart(
            @PathVariable Long userId,
            @Valid @RequestBody AddToCartDTO addToCartDTO) {
        CartDTO cart = cartService.addItemToCart(userId, addToCartDTO);
        return new ResponseEntity<>(cart, HttpStatus.CREATED);
    }
    
    @PostMapping("/session/{sessionId}/items")
    @Operation(summary = "Add item to anonymous cart")
    public ResponseEntity<CartDTO> addItemToAnonymousCart(
            @PathVariable String sessionId,
            @Valid @RequestBody AddToCartDTO addToCartDTO) {
        CartDTO cart = cartService.addItemToAnonymousCart(sessionId, addToCartDTO);
        return new ResponseEntity<>(cart, HttpStatus.CREATED);
    }
    
    @PutMapping("/user/{userId}/items/{productId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartDTO> updateCartItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        CartDTO cart = cartService.updateCartItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(cart);
    }
    
    @PutMapping("/session/{sessionId}/items/{productId}")
    @Operation(summary = "Update anonymous cart item quantity")
    public ResponseEntity<CartDTO> updateAnonymousCartItemQuantity(
            @PathVariable String sessionId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        CartDTO cart = cartService.updateAnonymousCartItemQuantity(sessionId, productId, quantity);
        return ResponseEntity.ok(cart);
    }
    
    @DeleteMapping("/user/{userId}/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/session/{sessionId}/items/{productId}")
    @Operation(summary = "Remove item from anonymous cart")
    public ResponseEntity<Void> removeItemFromAnonymousCart(
            @PathVariable String sessionId,
            @PathVariable Long productId) {
        cartService.removeItemFromAnonymousCart(sessionId, productId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/user/{userId}/clear")
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/session/{sessionId}/clear")
    @Operation(summary = "Clear anonymous cart")
    public ResponseEntity<Void> clearAnonymousCart(@PathVariable String sessionId) {
        cartService.clearAnonymousCart(sessionId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/merge")
    @Operation(summary = "Merge anonymous cart with user cart after login")
    public ResponseEntity<CartDTO> mergeCart(
            @RequestParam String sessionId,
            @RequestParam Long userId) {
        CartDTO cart = cartService.mergeAnonymousCart(sessionId, userId);
        return ResponseEntity.ok(cart);
    }
    
    @PostMapping("/user/{userId}/coupon")
    @Operation(summary = "Apply coupon to cart")
    public ResponseEntity<CartDTO> applyCoupon(
            @PathVariable Long userId,
            @RequestParam String couponCode) {
        CartDTO cart = cartService.applyCoupon(userId, couponCode);
        return ResponseEntity.ok(cart);
    }
    
    @DeleteMapping("/user/{userId}/coupon")
    @Operation(summary = "Remove coupon from cart")
    public ResponseEntity<Void> removeCoupon(@PathVariable Long userId) {
        cartService.removeCoupon(userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/user/{userId}/checkout")
    @Operation(summary = "Checkout cart")
    public ResponseEntity<CartDTO> checkoutCart(@PathVariable Long userId) {
        CartDTO cart = cartService.checkoutCart(userId);
        return ResponseEntity.ok(cart);
    }
    
    @GetMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Get specific cart item")
    public ResponseEntity<CartItemDTO> getCartItem(
            @PathVariable Long cartId,
            @PathVariable Long productId) {
        CartItemDTO item = cartService.getCartItem(cartId, productId);
        return ResponseEntity.ok(item);
    }
    
    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get cart item count")
    public ResponseEntity<Integer> getCartItemCount(@PathVariable Long userId) {
        Integer count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/user/{userId}/contains/{productId}")
    @Operation(summary = "Check if product is in cart")
    public ResponseEntity<Boolean> isProductInCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        Boolean inCart = cartService.isProductInCart(userId, productId);
        return ResponseEntity.ok(inCart);
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getUserId();
        }
        throw new RuntimeException("User not authenticated");
    }
} 