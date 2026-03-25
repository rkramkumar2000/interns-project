package com.ecommerce.product.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.dto.SearchCriteria;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.elasticsearch.ProductDocument;
import com.ecommerce.product.repository.elasticsearch.ProductSearchRepository;
import com.ecommerce.product.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {
    
    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ModelMapper modelMapper;
    
    @Override
    public void indexProduct(ProductDocument product) {
        try {
            productSearchRepository.save(product);
            log.info("Indexed product: {}", product.getProductId());
        } catch (Exception e) {
            log.error("Error indexing product: {}", product.getProductId(), e);
        }
    }
    
    @Override
    public void indexProducts(List<ProductDocument> products) {
        try {
            productSearchRepository.saveAll(products);
            log.info("Indexed {} products", products.size());
        } catch (Exception e) {
            log.error("Error indexing products", e);
        }
    }
    
    @Override
    public void deleteProductFromIndex(Long productId) {
        try {
            productSearchRepository.deleteById(productId);
            log.info("Deleted product from index: {}", productId);
        } catch (Exception e) {
            log.error("Error deleting product from index: {}", productId, e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    @Async
    public void reindexAllProducts() {
        log.info("Starting full product reindex...");
        try {
            // Delete existing index
            productSearchRepository.deleteAll();
            
            // Fetch all active products
            List<Product> products = productRepository.findByActiveTrue(Pageable.unpaged()).getContent();
            
            // Convert to documents and index
            List<ProductDocument> documents = products.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
            
            // Index in batches
            int batchSize = 100;
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                List<ProductDocument> batch = documents.subList(i, end);
                productSearchRepository.saveAll(batch);
                log.info("Indexed batch {}-{} of {}", i, end, documents.size());
            }
            
            log.info("Completed full product reindex. Total products: {}", documents.size());
        } catch (Exception e) {
            log.error("Error during full reindex", e);
        }
    }
    
    @Override
    public Page<ProductDTO> searchProducts(String query, Pageable pageable) {
        try {
            Page<ProductDocument> results = productSearchRepository.globalSearch(query, pageable);
            return results.map(this::convertToDTO);
        } catch (Exception e) {
            log.error("Error searching products for query: {}", query, e);
            return Page.empty();
        }
    }
    
    @Override
    public Page<ProductDTO> advancedSearch(SearchCriteria criteria, Pageable pageable) {
        try {
            // Build the query
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            
            // Text search
            if (criteria.getQuery() != null && !criteria.getQuery().isEmpty()) {
                boolQuery.must(QueryBuilders.multiMatch()
                    .query(criteria.getQuery())
                    .fields("productName^3", "description^2", "brand", "categoryName")
                    .type(TextQueryType.BestFields)
                    .build()._toQuery());
            }
            
            // Category filter
            if (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty()) {
                boolQuery.filter(QueryBuilders.terms()
                    .field("categoryId")
                    .terms(t -> t.value(criteria.getCategoryIds().stream()
                        .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id))
                        .collect(Collectors.toList())))
                    .build()._toQuery());
            }
            
            // Brand filter
            if (criteria.getBrands() != null && !criteria.getBrands().isEmpty()) {
                boolQuery.filter(QueryBuilders.terms()
                    .field("brand")
                    .terms(t -> t.value(criteria.getBrands().stream()
                        .map(brand -> co.elastic.clients.elasticsearch._types.FieldValue.of(brand))
                        .collect(Collectors.toList())))
                    .build()._toQuery());
            }
            
            // Price range
            if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
                RangeQuery.Builder priceRange = QueryBuilders.range()
                    .field("discountedPrice");
                if (criteria.getMinPrice() != null) {
                    priceRange.gte(co.elastic.clients.json.JsonData.of(criteria.getMinPrice()));
                }
                if (criteria.getMaxPrice() != null) {
                    priceRange.lte(co.elastic.clients.json.JsonData.of(criteria.getMaxPrice()));
                }
                boolQuery.filter(priceRange.build()._toQuery());
            }
            
            // Rating filter
            if (criteria.getMinRating() != null) {
                boolQuery.filter(QueryBuilders.range()
                    .field("rating")
                    .gte(co.elastic.clients.json.JsonData.of(criteria.getMinRating()))
                    .build()._toQuery());
            }
            
            // Stock filter
            if (Boolean.TRUE.equals(criteria.getInStockOnly())) {
                boolQuery.filter(QueryBuilders.term()
                    .field("inStock")
                    .value(true)
                    .build()._toQuery());
            }
            
            // Featured filter
            if (Boolean.TRUE.equals(criteria.getFeaturedOnly())) {
                boolQuery.filter(QueryBuilders.term()
                    .field("featured")
                    .value(true)
                    .build()._toQuery());
            }
            
            // Execute search
            NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(pageable)
                .build();
            
            SearchHits<ProductDocument> searchHits = elasticsearchTemplate.search(query, ProductDocument.class);
            
            List<ProductDTO> products = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            return new PageImpl<>(products, pageable, searchHits.getTotalHits());
            
        } catch (Exception e) {
            log.error("Error in advanced search", e);
            return Page.empty();
        }
    }
    
    private ProductDocument convertToDocument(Product product) {
        ProductDocument document = ProductDocument.builder()
            .productId(product.getProductId())
            .productName(product.getProductName())
            .description(product.getDescription())
            .brand(product.getBrand())
            .price(product.getPrice())
            .quantity(product.getQuantity())
            .categoryId(product.getCategory().getCategoryId())
            .categoryName(product.getCategory().getCategoryName())
            .sku(product.getSku())
            .imageUrl(product.getImage())  // Changed from getImageUrl() to getImage()
            .sellerId(product.getSellerId())
            .discountPercentage(product.getDiscount())  // Changed from getDiscountPercentage() to getDiscount()
            .rating(product.getRating())
            .reviewCount(product.getReviewCount())
            .active(product.getActive())
            .featured(false)  // Set default as featured field doesn't exist in Product
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .inStock(product.getQuantity() > 0)
            .build();
        
        // Calculate discounted price
        if (product.getDiscount() != null && product.getDiscount() > 0) {
            BigDecimal discount = product.getPrice()
                .multiply(BigDecimal.valueOf(product.getDiscount()))
                .divide(BigDecimal.valueOf(100));
            document.setDiscountedPrice(product.getPrice().subtract(discount));
        } else {
            document.setDiscountedPrice(product.getPrice());
        }
        
        // Set category hierarchy if available
        Category category = product.getCategory();
        if (category.getParent() != null) {
            Category parent = category.getParent();
            document.setCategoryHierarchy(ProductDocument.CategoryHierarchy.builder()
                .parentId(parent.getCategoryId())
                .parentName(parent.getCategoryName())
                .childId(category.getCategoryId())
                .childName(category.getCategoryName())
                .build());
        }
        
        return document;
    }
    
    private ProductDTO convertToDTO(ProductDocument document) {
        return modelMapper.map(document, ProductDTO.class);
    }
    
    @Override
    public Page<ProductDTO> searchByCategory(Long categoryId, Pageable pageable) {
        Page<ProductDocument> results = productSearchRepository.findByCategoryId(categoryId, pageable);
        return results.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> searchByBrand(String brand, Pageable pageable) {
        Page<ProductDocument> results = productSearchRepository.findByBrand(brand, pageable);
        return results.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<ProductDocument> results = productSearchRepository.findByDiscountedPriceBetween(minPrice, maxPrice, pageable);
        return results.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> findSimilarProducts(Long productId, Pageable pageable) {
        try {
            Page<ProductDocument> results = productSearchRepository.findSimilarProducts(productId, pageable);
            return results.map(this::convertToDTO);
        } catch (Exception e) {
            log.error("Error finding similar products for: {}", productId, e);
            return Page.empty();
        }
    }
    
    @Override
    public Map<String, List<FacetResult>> getFacets(String query) {
        // This would require aggregation queries
        // Simplified implementation for now
        Map<String, List<FacetResult>> facets = new HashMap<>();
        
        // Brand facets
        List<FacetResult> brandFacets = new ArrayList<>();
        // This would come from aggregation
        brandFacets.add(new FacetResult("Apple", 10L));
        brandFacets.add(new FacetResult("Samsung", 8L));
        facets.put("brands", brandFacets);
        
        // Category facets
        List<FacetResult> categoryFacets = new ArrayList<>();
        categoryFacets.add(new FacetResult("Electronics", 25L));
        categoryFacets.add(new FacetResult("Clothing", 18L));
        facets.put("categories", categoryFacets);
        
        return facets;
    }
    
    @Override
    public List<String> getAutoCompleteSuggestions(String prefix) {
        // Simplified implementation
        // In production, use completion suggester
        try {
            Page<ProductDocument> results = productSearchRepository
                .findByProductNameContainingIgnoreCase(prefix, Pageable.ofSize(10));
            
            return results.stream()
                .map(ProductDocument::getProductName)
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting suggestions for: {}", prefix, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<String> getPopularSearchTerms() {
        // This would typically come from a search analytics index
        return Arrays.asList(
            "iPhone", 
            "Laptop", 
            "Headphones", 
            "Nike shoes", 
            "Books"
        );
    }
    
    @Override
    @Async
    public void trackSearch(String query, Long userId) {
        // Track search for analytics
        log.info("Search tracked - Query: {}, User: {}", query, userId);
        // In production, this would save to an analytics index
    }
} 