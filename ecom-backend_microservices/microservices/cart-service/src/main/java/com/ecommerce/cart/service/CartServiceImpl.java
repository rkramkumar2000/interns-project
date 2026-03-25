package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductServiceClient;
import com.ecommerce.cart.dto.AddToCartDTO;
import com.ecommerce.cart.dto.CartDTO;
import com.ecommerce.cart.dto.CartItemDTO;
import com.ecommerce.cart.dto.ProductDTO;
import com.ecommerce.cart.exception.CartException;
import com.ecommerce.cart.exception.ResourceNotFoundException;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${cart.abandoned.hours:24}")
    private int abandonedCartHours;
    
    @Value("${kafka.topics.cart-events}")
    private String cartEventsTopic;
    
    @Override
    @Cacheable(value = "carts", key = "#userId")
    public CartDTO getCartByUserId(Long userId) {
        log.info("Fetching cart for user: {}", userId);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(userId, null));
        
        return convertToDTO(cart);
    }
    
    @Override
    public CartDTO getCartBySessionId(String sessionId) {
        log.info("Fetching cart for session: {}", sessionId);
        
        Cart cart = cartRepository.findBySessionIdAndStatusAndActiveTrue(sessionId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(null, sessionId));
        
        return convertToDTO(cart);
    }
    
    @Override
    @Cacheable(value = "carts", key = "#cartId")
    public CartDTO getCartById(Long cartId) {
        log.info("Fetching cart by id: {}", cartId);
        
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "id", cartId));
        
        return convertToDTO(cart);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public CartDTO addItemToCart(Long userId, AddToCartDTO addToCartDTO) {
        log.info("Adding item to cart for user: {} - product: {}", userId, addToCartDTO.getProductId());
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(userId, null));
        
        return addItemToCartInternal(cart, addToCartDTO);
    }
    
    @Override
    public CartDTO addItemToAnonymousCart(String sessionId, AddToCartDTO addToCartDTO) {
        log.info("Adding item to anonymous cart for session: {} - product: {}", sessionId, addToCartDTO.getProductId());
        
        Cart cart = cartRepository.findBySessionIdAndStatusAndActiveTrue(sessionId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(null, sessionId));
        
        return addItemToCartInternal(cart, addToCartDTO);
    }
    
    private CartDTO addItemToCartInternal(Cart cart, AddToCartDTO addToCartDTO) {
        // Fetch product details
        ProductDTO product = productServiceClient.getProductById(addToCartDTO.getProductId());
        
        if (product == null || !product.getActive() || !product.getInStock()) {
            throw new CartException("Product is not available");
        }
        
        if (product.getQuantity() < addToCartDTO.getQuantity()) {
            throw new CartException("Insufficient stock. Available: " + product.getQuantity());
        }
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartCartIdAndProductId(
                cart.getCartId(), addToCartDTO.getProductId());
        
        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + addToCartDTO.getQuantity();
            
            if (newQuantity > product.getQuantity()) {
                throw new CartException("Cannot add more than available stock");
            }
            
            item.setQuantity(newQuantity);
            item.calculateTotalPrice();
            cartItemRepository.save(item);
        } else {
            // Create new cart item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(product.getProductId());
            newItem.setProductName(product.getProductName());
            newItem.setProductImage(product.getImage());
            newItem.setProductPrice(product.getPrice());
            newItem.setSpecialPrice(product.getSpecialPrice());
            newItem.setQuantity(addToCartDTO.getQuantity());
            newItem.setDiscount(BigDecimal.valueOf(product.getDiscount() != null ? product.getDiscount() : 0));
            newItem.setProductBrand(product.getBrand());
            newItem.setProductCategory(product.getCategoryName());
            newItem.setAvailableStock(product.getQuantity());
            newItem.setProductSku(product.getSku());
            newItem.calculateTotalPrice();
            
            cart.addCartItem(newItem);
            cartItemRepository.save(newItem);
        }
        
        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Publish cart updated event
        publishCartEvent("ITEM_ADDED", savedCart, addToCartDTO.getProductId());
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public CartDTO updateCartItemQuantity(Long userId, Long productId, Integer quantity) {
        log.info("Updating cart item quantity for user: {} - product: {} - quantity: {}", userId, productId, quantity);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        
        return updateCartItemQuantityInternal(cart, productId, quantity);
    }
    
    @Override
    public CartDTO updateAnonymousCartItemQuantity(String sessionId, Long productId, Integer quantity) {
        log.info("Updating anonymous cart item quantity for session: {} - product: {} - quantity: {}", 
                sessionId, productId, quantity);
        
        Cart cart = cartRepository.findBySessionIdAndStatusAndActiveTrue(sessionId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));
        
        return updateCartItemQuantityInternal(cart, productId, quantity);
    }
    
    private CartDTO updateCartItemQuantityInternal(Cart cart, Long productId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));
        
        if (quantity <= 0) {
            cart.removeCartItem(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            // Verify stock availability
            ProductDTO product = productServiceClient.getProductById(productId);
            if (product.getQuantity() < quantity) {
                throw new CartException("Insufficient stock. Available: " + product.getQuantity());
            }
            
            cartItem.setQuantity(quantity);
            cartItem.calculateTotalPrice();
            cartItemRepository.save(cartItem);
        }
        
        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Publish cart updated event
        publishCartEvent("ITEM_UPDATED", savedCart, productId);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public void removeItemFromCart(Long userId, Long productId) {
        log.info("Removing item from cart for user: {} - product: {}", userId, productId);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        
        removeItemFromCartInternal(cart, productId);
    }
    
    @Override
    public void removeItemFromAnonymousCart(String sessionId, Long productId) {
        log.info("Removing item from anonymous cart for session: {} - product: {}", sessionId, productId);
        
        Cart cart = cartRepository.findBySessionIdAndStatusAndActiveTrue(sessionId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));
        
        removeItemFromCartInternal(cart, productId);
    }
    
    private void removeItemFromCartInternal(Cart cart, Long productId) {
        CartItem cartItem = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));
        
        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);
        
        cart.recalculateTotals();
        cartRepository.save(cart);
        
        // Publish cart updated event
        publishCartEvent("ITEM_REMOVED", cart, productId);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        
        clearCartInternal(cart);
    }
    
    @Override
    public void clearAnonymousCart(String sessionId) {
        log.info("Clearing anonymous cart for session: {}", sessionId);
        
        Cart cart = cartRepository.findBySessionIdAndStatusAndActiveTrue(sessionId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));
        
        clearCartInternal(cart);
    }
    
    private void clearCartInternal(Cart cart) {
        cartItemRepository.deleteAllByCartId(cart.getCartId());
        cart.getCartItems().clear();
        cart.recalculateTotals();
        cartRepository.save(cart);
        
        // Publish cart cleared event
        publishCartEvent("CART_CLEARED", cart, null);
    }
    
    @Override
    @CacheEvict(value = "carts", allEntries = true)
    public CartDTO mergeAnonymousCart(String sessionId, Long userId) {
        log.info("Merging anonymous cart {} with user cart {}", sessionId, userId);
        
        // Get anonymous cart
        Optional<Cart> anonymousCartOpt = cartRepository.findBySessionIdAndStatusAndActiveTrue(
                sessionId, Cart.CartStatus.ACTIVE);
        
        if (anonymousCartOpt.isEmpty()) {
            // No anonymous cart to merge
            return getCartByUserId(userId);
        }
        
        Cart anonymousCart = anonymousCartOpt.get();
        
        // Get or create user cart
        Cart userCart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(userId, null));
        
        // Merge items
        for (CartItem anonymousItem : anonymousCart.getCartItems()) {
            Optional<CartItem> existingItem = cartItemRepository.findByCartCartIdAndProductId(
                    userCart.getCartId(), anonymousItem.getProductId());
            
            if (existingItem.isPresent()) {
                // Update quantity
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + anonymousItem.getQuantity());
                item.calculateTotalPrice();
                cartItemRepository.save(item);
            } else {
                // Add new item
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setProductId(anonymousItem.getProductId());
                newItem.setProductName(anonymousItem.getProductName());
                newItem.setProductImage(anonymousItem.getProductImage());
                newItem.setProductPrice(anonymousItem.getProductPrice());
                newItem.setSpecialPrice(anonymousItem.getSpecialPrice());
                newItem.setQuantity(anonymousItem.getQuantity());
                newItem.setDiscount(anonymousItem.getDiscount());
                newItem.setProductBrand(anonymousItem.getProductBrand());
                newItem.setProductCategory(anonymousItem.getProductCategory());
                newItem.setAvailableStock(anonymousItem.getAvailableStock());
                newItem.setProductSku(anonymousItem.getProductSku());
                newItem.calculateTotalPrice();
                
                userCart.addCartItem(newItem);
                cartItemRepository.save(newItem);
            }
        }
        
        // Mark anonymous cart as merged
        anonymousCart.setStatus(Cart.CartStatus.MERGED);
        cartRepository.save(anonymousCart);
        
        // Recalculate and save user cart
        userCart.recalculateTotals();
        Cart savedCart = cartRepository.save(userCart);
        
        // Publish cart merged event
        publishCartEvent("CART_MERGED", savedCart, null);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public CartDTO applyCoupon(Long userId, String couponCode) {
        log.info("Applying coupon {} to cart for user: {}", couponCode, userId);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        
        // TODO: Validate coupon with coupon service
        // For now, apply a fixed 10% discount
        cart.setCouponCode(couponCode);
        cart.setCouponDiscount(cart.getSubtotal().multiply(BigDecimal.valueOf(0.1)));
        cart.recalculateTotals();
        
        Cart savedCart = cartRepository.save(cart);
        
        // Publish coupon applied event
        publishCartEvent("COUPON_APPLIED", savedCart, null);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public void removeCoupon(Long userId) {
        log.info("Removing coupon from cart for user: {}", userId);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        
        cart.setCouponCode(null);
        cart.setCouponDiscount(BigDecimal.ZERO);
        cart.recalculateTotals();
        
        cartRepository.save(cart);
        
        // Publish coupon removed event
        publishCartEvent("COUPON_REMOVED", cart, null);
    }
    
    @Override
    @CacheEvict(value = "carts", key = "#userId")
    public CartDTO checkoutCart(Long userId) {
        log.info("Checking out cart for user: {}", userId);
        
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        
        if (cart.getCartItems().isEmpty()) {
            throw new CartException("Cannot checkout empty cart");
        }
        
        // Update cart status
        cart.setStatus(Cart.CartStatus.CHECKED_OUT);
        Cart savedCart = cartRepository.save(cart);
        
        // Publish checkout event
        publishCartEvent("CART_CHECKED_OUT", savedCart, null);
        
        return convertToDTO(savedCart);
    }
    
    @Override
    public void markCartAsAbandoned(Long cartId) {
        log.info("Marking cart as abandoned: {}", cartId);
        
        cartRepository.updateCartStatus(cartId, Cart.CartStatus.ABANDONED);
        
        // Publish abandoned cart event
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart != null) {
            publishCartEvent("CART_ABANDONED", cart, null);
        }
    }
    
    @Override
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupAbandonedCarts() {
        log.info("Running abandoned cart cleanup");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(abandonedCartHours);
        int deactivated = cartRepository.deactivateAbandonedCarts(cutoffTime);
        
        log.info("Deactivated {} abandoned carts", deactivated);
    }
    
    @Override
    public CartItemDTO getCartItem(Long cartId, Long productId) {
        CartItem cartItem = cartItemRepository.findByCartCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));
        
        return convertToItemDTO(cartItem);
    }
    
    @Override
    public Integer getCartItemCount(Long userId) {
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElse(null);
        
        return cart != null ? cart.getTotalItems() : 0;
    }
    
    @Override
    public Boolean isProductInCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserIdAndStatusAndActiveTrue(userId, Cart.CartStatus.ACTIVE)
                .orElse(null);
        
        if (cart == null) {
            return false;
        }
        
        return cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId).isPresent();
    }
    
    private Cart createNewCart(Long userId, String sessionId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setSessionId(sessionId);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        cart.setActive(true);
        
        return cartRepository.save(cart);
    }
    
    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = modelMapper.map(cart, CartDTO.class);
        
        // Map cart items
        dto.setCartItems(cart.getCartItems().stream()
                .map(this::convertToItemDTO)
                .collect(Collectors.toList()));
        
        // Set status string
        dto.setStatus(cart.getStatus().toString());
        
        return dto;
    }
    
    private CartItemDTO convertToItemDTO(CartItem item) {
        CartItemDTO dto = modelMapper.map(item, CartItemDTO.class);
        
        // No need to set these fields - they are calculated by getter methods
        // dto.setInStock(dto.getInStock());
        // dto.setStockWarning(dto.getStockWarning());
        // dto.setStockMessage(dto.getStockMessage());
        
        return dto;
    }
    
    private void publishCartEvent(String eventType, Cart cart, Long productId) {
        try {
            CartEvent event = new CartEvent(eventType, cart.getCartId(), cart.getUserId(), productId, convertToDTO(cart));
            kafkaTemplate.send(cartEventsTopic, event);
            log.info("Published {} event for cart: {}", eventType, cart.getCartId());
        } catch (Exception e) {
            log.error("Failed to publish cart event", e);
        }
    }
    
    // Event class
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CartEvent {
        private String eventType;
        private Long cartId;
        private Long userId;
        private Long productId;
        private CartDTO cart;
    }
} 