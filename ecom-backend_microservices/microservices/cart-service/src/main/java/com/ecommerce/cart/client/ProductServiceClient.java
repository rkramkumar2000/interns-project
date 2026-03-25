package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.ProductDTO;
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
        ProductDTO fallbackProduct = new ProductDTO();
        fallbackProduct.setProductId(productId);
        fallbackProduct.setProductName("Product Unavailable");
        fallbackProduct.setInStock(false);
        return fallbackProduct;
    }
    
    default Boolean updateStockFallback(Long productId, Integer quantity, boolean increase, Exception ex) {
        // Log the error and return false to indicate stock update failed
        return false;
    }
} 