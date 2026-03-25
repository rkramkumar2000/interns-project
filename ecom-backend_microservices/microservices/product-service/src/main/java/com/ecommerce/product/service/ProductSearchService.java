package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.dto.SearchCriteria;
import com.ecommerce.product.model.elasticsearch.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductSearchService {
    
    // Indexing operations
    void indexProduct(ProductDocument product);
    void indexProducts(List<ProductDocument> products);
    void deleteProductFromIndex(Long productId);
    void reindexAllProducts();
    
    // Search operations
    Page<ProductDTO> searchProducts(String query, Pageable pageable);
    Page<ProductDTO> advancedSearch(SearchCriteria criteria, Pageable pageable);
    Page<ProductDTO> searchByCategory(Long categoryId, Pageable pageable);
    Page<ProductDTO> searchByBrand(String brand, Pageable pageable);
    Page<ProductDTO> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<ProductDTO> findSimilarProducts(Long productId, Pageable pageable);
    
    // Faceted search
    Map<String, List<FacetResult>> getFacets(String query);
    
    // Auto-complete suggestions
    List<String> getAutoCompleteSuggestions(String prefix);
    
    // Popular searches
    List<String> getPopularSearchTerms();
    
    // Analytics
    void trackSearch(String query, Long userId);
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FacetResult {
        private String value;
        private Long count;
    }
} 