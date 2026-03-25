package com.ecommerce.product.repository.elasticsearch;

import com.ecommerce.product.model.elasticsearch.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    
    // Basic search queries
    Page<ProductDocument> findByProductNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<ProductDocument> findByBrand(String brand, Pageable pageable);
    
    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);
    
    Page<ProductDocument> findByActiveTrue(Pageable pageable);
    
    Page<ProductDocument> findByFeaturedTrue(Pageable pageable);
    
    // Price range queries
    Page<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    Page<ProductDocument> findByDiscountedPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Complex queries
    Page<ProductDocument> findByProductNameContainingIgnoreCaseAndBrandAndPriceBetween(
            String name, String brand, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Custom queries using @Query annotation
    @Query("{\"bool\": {\"must\": [{\"match\": {\"productName\": \"?0\"}}, {\"match\": {\"description\": \"?0\"}}]}}")
    Page<ProductDocument> searchByNameAndDescription(String searchTerm, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"rating\": {\"gte\": ?0}}}, {\"range\": {\"reviewCount\": {\"gte\": ?1}}}]}}")
    Page<ProductDocument> findHighlyRatedProducts(Double minRating, Integer minReviews, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"term\": {\"categoryId\": ?0}}, {\"range\": {\"price\": {\"gte\": ?1, \"lte\": ?2}}}]}}")
    Page<ProductDocument> findByCategoryAndPriceRange(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Multi-match query for global search
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"productName^3\", \"description^2\", \"brand\", \"categoryName\"], \"type\": \"best_fields\"}}")
    Page<ProductDocument> globalSearch(String searchTerm, Pageable pageable);
    
    // Aggregation queries
    @Query("{\"bool\": {\"filter\": [{\"term\": {\"sellerId\": ?0}}, {\"term\": {\"active\": true}}]}}")
    List<ProductDocument> findActiveProductsBySeller(Long sellerId);
    
    // Find similar products
    @Query("{\"more_like_this\": {\"fields\": [\"productName\", \"description\", \"categoryName\"], \"like\": [{\"_id\": \"?0\"}], \"min_term_freq\": 1, \"max_query_terms\": 12}}")
    Page<ProductDocument> findSimilarProducts(Long productId, Pageable pageable);
} 