package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    
    ProductDTO createProduct(ProductDTO productDTO);
    
    ProductDTO updateProduct(Long productId, ProductDTO productDTO);
    
    ProductDTO getProductById(Long productId);
    
    Page<ProductDTO> getAllProducts(Pageable pageable);
    
    Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable);
    
    Page<ProductDTO> searchProducts(String keyword, Pageable pageable);
    
    Page<ProductDTO> getProductsBySeller(Long sellerId, Pageable pageable);
    
    Page<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    Page<ProductDTO> getProductsByBrand(String brand, Pageable pageable);
    
    Page<ProductDTO> getDiscountedProducts(Pageable pageable);
    
    List<ProductDTO> getNewArrivals();
    
    List<ProductDTO> getTopRatedProducts();
    
    List<String> getAllBrands();
    
    void deleteProduct(Long productId);
    
    ProductDTO updateProductImage(Long productId, MultipartFile image);
    
    boolean updateStock(Long productId, Integer quantity, boolean increase);
    
    List<ProductDTO> getLowStockProducts(Integer threshold);
    
    void activateProduct(Long productId);
    
    void deactivateProduct(Long productId);
    
    ProductDTO getProductBySku(String sku);
} 