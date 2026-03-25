package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ecommerce.order.dto.InventoryCheckDTO;
import com.ecommerce.order.dto.ProductDTO;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientOrderService {
    
    private final RestTemplate restTemplate;
    
    private static final String PRODUCT_SERVICE_CB = "product-service";
    private static final String INVENTORY_SERVICE_CB = "inventory-service";
    private static final String PAYMENT_SERVICE_CB = "payment-service";
    
    /**
     * Get product details with circuit breaker and retry
     */
    @CircuitBreaker(name = PRODUCT_SERVICE_CB, fallbackMethod = "getProductFallback")
    @Retry(name = PRODUCT_SERVICE_CB)
    public ProductDTO getProduct(Long productId) {
        log.info("Fetching product: {}", productId);
        return restTemplate.getForObject(
            "http://product-service/api/products/" + productId, 
            ProductDTO.class
        );
    }
    
    /**
     * Check inventory with circuit breaker, retry, and timeout
     */
    @CircuitBreaker(name = INVENTORY_SERVICE_CB, fallbackMethod = "checkInventoryFallback")
    @Retry(name = INVENTORY_SERVICE_CB)
    @TimeLimiter(name = INVENTORY_SERVICE_CB)
    public CompletableFuture<InventoryCheckDTO> checkInventoryAsync(Long productId, Integer quantity) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Checking inventory for product: {} quantity: {}", productId, quantity);
            return restTemplate.postForObject(
                "http://inventory-service/api/inventory/check",
                new InventoryCheckRequest(productId, quantity),
                InventoryCheckDTO.class
            );
        });
    }
    
    /**
     * Process payment with rate limiter and circuit breaker
     */
    @RateLimiter(name = PAYMENT_SERVICE_CB)
    @CircuitBreaker(name = PAYMENT_SERVICE_CB, fallbackMethod = "processPaymentFallback")
    @Retry(name = PAYMENT_SERVICE_CB)
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());
        return restTemplate.postForObject(
            "http://payment-service/api/payments",
            request,
            PaymentResult.class
        );
    }
    
    /**
     * Bulk operation with circuit breaker
     */
    @CircuitBreaker(name = PRODUCT_SERVICE_CB, fallbackMethod = "getProductsBulkFallback")
    @SuppressWarnings("unchecked")
    public List<ProductDTO> getProductsBulk(List<Long> productIds) {
        log.info("Fetching {} products in bulk", productIds.size());
        return restTemplate.postForObject(
            "http://product-service/api/products/bulk",
            productIds,
            List.class
        );
    }
    
    // Fallback methods
    
    public ProductDTO getProductFallback(Long productId, Exception ex) {
        log.error("Product service unavailable for product: {}, error: {}", productId, ex.getMessage());
        // Return cached or default product
        return ProductDTO.builder()
            .productId(productId)
            .productName("Product Unavailable")
            .price(BigDecimal.ZERO)
            .available(false)
            .build();
    }
    
    public CompletableFuture<InventoryCheckDTO> checkInventoryFallback(Long productId, Integer quantity, Exception ex) {
        log.error("Inventory service unavailable, error: {}", ex.getMessage());
        return CompletableFuture.completedFuture(
            InventoryCheckDTO.builder()
                .productId(productId)
                .requestedQuantity(quantity)
                .availableQuantity(0)
                .inStock(false)
                .message("Inventory service temporarily unavailable")
                .build()
        );
    }
    
    public PaymentResult processPaymentFallback(PaymentRequest request, Exception ex) {
        log.error("Payment service unavailable for order: {}, error: {}", request.getOrderId(), ex.getMessage());
        
        // Could queue the payment for later processing
        return PaymentResult.builder()
            .orderId(request.getOrderId())
            .status("PENDING")
            .message("Payment will be processed when service is available")
            .build();
    }
    
    public List<ProductDTO> getProductsBulkFallback(List<Long> productIds, Exception ex) {
        log.error("Bulk product fetch failed, error: {}", ex.getMessage());
        // Return empty list or cached data
        return List.of();
    }
    
    // DTOs for this example
    @lombok.Data
    public static class InventoryCheckRequest {
        private Long productId;
        private Integer quantity;
        
        public InventoryCheckRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PaymentRequest {
        private String orderId;
        private Double amount;
        private String currency;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PaymentResult {
        private String orderId;
        private String status;
        private String message;
    }
} 