package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductServiceFallback implements ProductServiceClient {
    
    @Override
    public ProductDTO getProductById(Long productId) {
        log.warn("Fallback: Unable to fetch product with id: {}", productId);
        
        ProductDTO fallbackProduct = new ProductDTO();
        fallbackProduct.setProductId(productId);
        fallbackProduct.setProductName("Product Service Unavailable");
        fallbackProduct.setInStock(false);
        fallbackProduct.setQuantity(0);
        
        return fallbackProduct;
    }
    
    @Override
    public Boolean updateStock(Long productId, Integer quantity, boolean increase) {
        log.error("Fallback: Unable to update stock for product: {} - quantity: {} - increase: {}", 
                productId, quantity, increase);
        
        // Return false to indicate the stock update failed
        return false;
    }
} 