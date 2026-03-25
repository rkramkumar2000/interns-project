package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.dto.SearchCriteria;
import com.ecommerce.product.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
@Tag(name = "Product Search", description = "Advanced product search endpoints")
public class ProductSearchController {
    
    private final ProductSearchService productSearchService;
    
    @GetMapping
    @Operation(summary = "Search products by query")
    public ResponseEntity<Page<ProductDTO>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sort) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<ProductDTO> results = productSearchService.searchProducts(q, pageable);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/advanced")
    @Operation(summary = "Advanced product search with multiple criteria")
    public ResponseEntity<Page<ProductDTO>> advancedSearch(
            @RequestBody SearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> results = productSearchService.advancedSearch(criteria, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Search products by category")
    public ResponseEntity<Page<ProductDTO>> searchByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> results = productSearchService.searchByCategory(categoryId, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/brand/{brand}")
    @Operation(summary = "Search products by brand")
    public ResponseEntity<Page<ProductDTO>> searchByBrand(
            @PathVariable String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> results = productSearchService.searchByBrand(brand, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/price-range")
    @Operation(summary = "Search products by price range")
    public ResponseEntity<Page<ProductDTO>> searchByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> results = productSearchService.searchByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/similar/{productId}")
    @Operation(summary = "Find similar products")
    public ResponseEntity<Page<ProductDTO>> findSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> results = productSearchService.findSimilarProducts(productId, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/facets")
    @Operation(summary = "Get search facets for filtering")
    public ResponseEntity<Map<String, List<ProductSearchService.FacetResult>>> getFacets(
            @RequestParam(required = false) String q) {
        
        Map<String, List<ProductSearchService.FacetResult>> facets = productSearchService.getFacets(q);
        return ResponseEntity.ok(facets);
    }
    
    @GetMapping("/suggestions")
    @Operation(summary = "Get autocomplete suggestions")
    public ResponseEntity<List<String>> getAutocompleteSuggestions(
            @RequestParam String prefix) {
        
        List<String> suggestions = productSearchService.getAutoCompleteSuggestions(prefix);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/popular")
    @Operation(summary = "Get popular search terms")
    public ResponseEntity<List<String>> getPopularSearchTerms() {
        List<String> popularTerms = productSearchService.getPopularSearchTerms();
        return ResponseEntity.ok(popularTerms);
    }
    
    @PostMapping("/reindex")
    @Operation(summary = "Reindex all products (Admin only)")
    public ResponseEntity<String> reindexProducts() {
        productSearchService.reindexAllProducts();
        return ResponseEntity.ok("Reindexing started in background");
    }
    
    private Pageable createPageable(int page, int size, String sort) {
        Sort sortOrder;
        switch (sort.toLowerCase()) {
            case "price_asc":
                sortOrder = Sort.by("discountedPrice").ascending();
                break;
            case "price_desc":
                sortOrder = Sort.by("discountedPrice").descending();
                break;
            case "rating":
                sortOrder = Sort.by("rating").descending();
                break;
            case "newest":
                sortOrder = Sort.by("createdAt").descending();
                break;
            default:
                sortOrder = Sort.by("_score").descending(); // relevance
        }
        return PageRequest.of(page, size, sortOrder);
    }
} 