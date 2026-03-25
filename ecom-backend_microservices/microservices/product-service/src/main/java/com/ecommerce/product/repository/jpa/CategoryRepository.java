package com.ecommerce.product.repository.jpa;

import com.ecommerce.product.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByCategoryName(String categoryName);
    
    List<Category> findByActiveTrue();
    
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
    
    List<Category> findByParentIsNullAndActiveTrue();
    
    List<Category> findByParentCategoryIdAndActiveTrue(Long parentId);
    
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.active = true ORDER BY c.displayOrder")
    List<Category> findAllRootCategoriesWithChildren();
    
    boolean existsByCategoryNameAndCategoryIdNot(String categoryName, Long categoryId);
} 