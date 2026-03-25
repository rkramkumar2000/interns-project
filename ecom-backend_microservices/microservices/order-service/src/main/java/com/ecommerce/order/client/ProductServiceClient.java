package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", fallback = ProductServiceFallback.class)
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{productId}")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductByIdFallback")
    ProductDTO getProductById(@PathVariable Long productId);
    
    @PutMapping("/api/products/{productId}/stock")
    @CircuitBreaker(name = "product-service", fallbackMethod = "updateStockFallback")
    Boolean updateStock(@PathVariable Long productId,
                        @RequestParam Integer quantity,
                        @RequestParam boolean increase);
    
    // Fallback methods
    default ProductDTO getProductByIdFallback(Long productId, Exception ex) {
        ProductDTO product = new ProductDTO();
        product.setProductId(productId);
        product.setProductName("Product Information Unavailable");
        return product;
    }
    
    default Boolean updateStockFallback(Long productId, Integer quantity, boolean increase, Exception ex) {
        // Log error and return false to indicate failure
        return false;
    }
} 