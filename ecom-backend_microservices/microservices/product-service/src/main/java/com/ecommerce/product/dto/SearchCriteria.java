package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCriteria {
    
    private String query;
    private List<Long> categoryIds;
    private List<String> brands;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private Integer minReviews;
    private Boolean inStockOnly;
    private Boolean featuredOnly;
    private String sortBy; // price_asc, price_desc, rating, newest, relevance
    private List<String> tags;
    private Long sellerId;
} 