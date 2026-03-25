package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductServiceFallback implements ProductServiceClient {
    
    @Override
    public ProductDTO getProductById(Long productId) {
        log.error("Fallback: Unable to get product with id {}", productId);
        ProductDTO product = new ProductDTO();
        product.setProductId(productId);
        product.setProductName("Product Information Unavailable");
        product.setAvailable(false);
        return product;
    }
    
    @Override
    public Boolean updateStock(Long productId, Integer quantity, boolean increase) {
        log.error("Fallback: Unable to update stock for product {} by {}", productId, quantity);
        // Return false to indicate that stock update failed
        // The order service should handle this appropriately
        return false;
    }
} 