package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    
    private Long categoryId;
    
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String categoryName;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String image;
    
    private Boolean active;
    
    private Integer displayOrder;
    
    private Long parentId;
    
    private String parentName;
    
    private List<CategoryDTO> children;
    
    private Integer productCount;
} 