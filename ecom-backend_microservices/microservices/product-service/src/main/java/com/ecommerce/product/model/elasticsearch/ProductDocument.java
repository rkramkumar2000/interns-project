package com.ecommerce.product.model.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "products")
@Setting(shards = 2, replicas = 1)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {
    
    @Id
    private Long productId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String productName;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String brand;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    @Field(type = FieldType.Integer)
    private Integer quantity;
    
    @Field(type = FieldType.Long)
    private Long categoryId;
    
    @Field(type = FieldType.Text)
    private String categoryName;
    
    @Field(type = FieldType.Keyword)
    private String sku;
    
    @Field(type = FieldType.Text)
    private String imageUrl;
    
    @Field(type = FieldType.Long)
    private Long sellerId;
    
    @Field(type = FieldType.Double)
    private Double discountPercentage;
    
    @Field(type = FieldType.Double)
    private Double rating;
    
    @Field(type = FieldType.Integer)
    private Integer reviewCount;
    
    @Field(type = FieldType.Boolean)
    private Boolean active;
    
    @Field(type = FieldType.Boolean)
    private Boolean featured;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
    
    // Computed fields for search
    @Field(type = FieldType.Double)
    private BigDecimal discountedPrice;
    
    @Field(type = FieldType.Boolean)
    private Boolean inStock;
    
    // Nested field for better search
    @Field(type = FieldType.Nested)
    private CategoryHierarchy categoryHierarchy;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryHierarchy {
        @Field(type = FieldType.Long)
        private Long parentId;
        
        @Field(type = FieldType.Text)
        private String parentName;
        
        @Field(type = FieldType.Long)
        private Long childId;
        
        @Field(type = FieldType.Text)
        private String childName;
    }
} 