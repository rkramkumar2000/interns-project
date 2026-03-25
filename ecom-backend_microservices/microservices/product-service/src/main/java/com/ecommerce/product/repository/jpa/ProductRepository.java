package com.ecommerce.product.repository.jpa;

import com.ecommerce.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByActiveTrue(Pageable pageable);
    
    Page<Product> findByCategoryCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    
    Page<Product> findByProductNameContainingIgnoreCaseAndActiveTrue(String keyword, Pageable pageable);
    
    Page<Product> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);
    
    List<Product> findTop10ByActiveTrueOrderByCreatedAtDesc();
    
    List<Product> findTop10ByActiveTrueOrderByRatingDesc();
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                  @Param("maxPrice") BigDecimal maxPrice, 
                                  Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.brand = :brand")
    Page<Product> findByBrand(@Param("brand") String brand, Pageable pageable);
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true ORDER BY p.brand")
    List<String> findAllBrands();
    
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :quantity WHERE p.productId = :productId AND p.quantity >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :quantity WHERE p.productId = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.discount > 0 ORDER BY p.discount DESC")
    Page<Product> findDiscountedProducts(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.active = true")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
    
    Optional<Product> findBySkuAndActiveTrue(String sku);
    
    @Query("SELECT p FROM Product p WHERE p.quantity <= :threshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
} 