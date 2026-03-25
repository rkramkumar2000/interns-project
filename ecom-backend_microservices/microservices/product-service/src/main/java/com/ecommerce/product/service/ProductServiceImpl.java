package com.ecommerce.product.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.elasticsearch.ProductDocument;
import com.ecommerce.product.repository.jpa.CategoryRepository;
import com.ecommerce.product.repository.jpa.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductSearchService productSearchService;
    
    @Value("${product.image.upload-dir}")
    private String uploadDir;
    
    @Value("${kafka.topics.product-events}")
    private String productEventsTopic;
    
    @Override
    @CacheEvict(value = {"products", "productsByCategory", "productSearch"}, allEntries = true)
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Creating new product: {}", productDTO.getProductName());
        
        Product product = modelMapper.map(productDTO, Product.class);
        
        // Set category
        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDTO.getCategoryId()));
            product.setCategory(category);
        }
        
        // Calculate final price if discount is present
        if (product.getDiscount() != null && product.getDiscount() > 0) {
            BigDecimal discountAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(product.getDiscount() / 100));
            product.setSpecialPrice(product.getPrice().subtract(discountAmount));
        }
        
        Product savedProduct = productRepository.save(product);
        
        // Index product in Elasticsearch
        indexProductInElasticsearch(savedProduct);
        
        // Publish product created event
        publishProductEvent("PRODUCT_CREATED", savedProduct);
        
        return convertToDTO(savedProduct);
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = {"productsByCategory", "productSearch"}, allEntries = true)
    })
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        log.info("Updating product with id: {}", productId);
        
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Update fields
        existingProduct.setProductName(productDTO.getProductName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setQuantity(productDTO.getQuantity());
        existingProduct.setBrand(productDTO.getBrand());
        existingProduct.setDiscount(productDTO.getDiscount());
        existingProduct.setSku(productDTO.getSku());
        existingProduct.setTags(productDTO.getTags());
        existingProduct.setMinOrderQuantity(productDTO.getMinOrderQuantity());
        existingProduct.setMaxOrderQuantity(productDTO.getMaxOrderQuantity());
        
        // Update category if changed
        if (productDTO.getCategoryId() != null && 
            !productDTO.getCategoryId().equals(existingProduct.getCategory().getCategoryId())) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDTO.getCategoryId()));
            existingProduct.setCategory(category);
        }
        
        // Recalculate special price
        if (existingProduct.getDiscount() != null && existingProduct.getDiscount() > 0) {
            BigDecimal discountAmount = existingProduct.getPrice()
                    .multiply(BigDecimal.valueOf(existingProduct.getDiscount() / 100));
            existingProduct.setSpecialPrice(existingProduct.getPrice().subtract(discountAmount));
        } else {
            existingProduct.setSpecialPrice(existingProduct.getPrice());
        }
        
        Product updatedProduct = productRepository.save(existingProduct);
        
        // Index updated product in Elasticsearch
        indexProductInElasticsearch(updatedProduct);
        
        // Publish product updated event
        publishProductEvent("PRODUCT_UPDATED", updatedProduct);
        
        return convertToDTO(updatedProduct);
    }
    
    @Override
    @Cacheable(value = "products", key = "#productId")
    public ProductDTO getProductById(Long productId) {
        log.info("Fetching product with id: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        return convertToDTO(product);
    }
    
    @Override
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pagination");
        
        Page<Product> products = productRepository.findByActiveTrue(pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    @Cacheable(value = "productsByCategory", key = "#categoryId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.info("Fetching products for category: {}", categoryId);
        
        Page<Product> products = productRepository.findByCategoryCategoryIdAndActiveTrue(categoryId, pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    @Cacheable(value = "productSearch", key = "#keyword + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        log.info("Searching products with keyword: {}", keyword);
        
        Page<Product> products = productRepository.findByProductNameContainingIgnoreCaseAndActiveTrue(keyword, pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> getProductsBySeller(Long sellerId, Pageable pageable) {
        log.info("Fetching products for seller: {}", sellerId);
        
        Page<Product> products = productRepository.findBySellerIdAndActiveTrue(sellerId, pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.info("Fetching products in price range: {} - {}", minPrice, maxPrice);
        
        Page<Product> products = productRepository.findByPriceRange(minPrice, maxPrice, pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> getProductsByBrand(String brand, Pageable pageable) {
        log.info("Fetching products for brand: {}", brand);
        
        Page<Product> products = productRepository.findByBrand(brand, pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    public Page<ProductDTO> getDiscountedProducts(Pageable pageable) {
        log.info("Fetching discounted products");
        
        Page<Product> products = productRepository.findDiscountedProducts(pageable);
        return products.map(this::convertToDTO);
    }
    
    @Override
    public List<ProductDTO> getNewArrivals() {
        log.info("Fetching new arrival products");
        
        List<Product> products = productRepository.findTop10ByActiveTrueOrderByCreatedAtDesc();
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDTO> getTopRatedProducts() {
        log.info("Fetching top rated products");
        
        List<Product> products = productRepository.findTop10ByActiveTrueOrderByRatingDesc();
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getAllBrands() {
        log.info("Fetching all brands");
        return productRepository.findAllBrands();
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#productId"),
        @CacheEvict(value = {"productsByCategory", "productSearch"}, allEntries = true)
    })
    public void deleteProduct(Long productId) {
        log.info("Deleting product with id: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Soft delete
        product.setActive(false);
        productRepository.save(product);
        
        // Publish product deleted event
        publishProductEvent("PRODUCT_DELETED", product);
        deleteProductFromElasticsearch(productId); // Delete from Elasticsearch
    }
    
    @Override
    @CacheEvict(value = "products", key = "#productId")
    public ProductDTO updateProductImage(Long productId, MultipartFile image) {
        log.info("Updating image for product: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        try {
            String fileName = saveImage(image);
            product.setImage(fileName);
            Product updatedProduct = productRepository.save(product);
            
            return convertToDTO(updatedProduct);
        } catch (IOException e) {
            log.error("Error uploading image for product: {}", productId, e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }
    
    @Override
    @Transactional
    public boolean updateStock(Long productId, Integer quantity, boolean increase) {
        log.info("Updating stock for product: {} - quantity: {} - increase: {}", productId, quantity, increase);
        
        int updated;
        if (increase) {
            updated = productRepository.increaseStock(productId, quantity);
        } else {
            updated = productRepository.decreaseStock(productId, quantity);
        }
        
        if (updated > 0) {
            // Publish inventory event
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                publishInventoryEvent(increase ? "STOCK_INCREASED" : "STOCK_DECREASED", product, quantity);
            }
        }
        
        return updated > 0;
    }
    
    @Override
    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        log.info("Fetching low stock products with threshold: {}", threshold);
        
        List<Product> products = productRepository.findLowStockProducts(threshold);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @CacheEvict(value = {"products", "productsByCategory", "productSearch"}, allEntries = true)
    public void activateProduct(Long productId) {
        log.info("Activating product: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        product.setActive(true);
        productRepository.save(product);
        
        publishProductEvent("PRODUCT_ACTIVATED", product);
    }
    
    @Override
    @CacheEvict(value = {"products", "productsByCategory", "productSearch"}, allEntries = true)
    public void deactivateProduct(Long productId) {
        log.info("Deactivating product: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        product.setActive(false);
        productRepository.save(product);
        
        publishProductEvent("PRODUCT_DEACTIVATED", product);
    }
    
    @Override
    public ProductDTO getProductBySku(String sku) {
        log.info("Fetching product by SKU: {}", sku);
        
        Product product = productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        
        return convertToDTO(product);
    }
    
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = modelMapper.map(product, ProductDTO.class);
        
        // Set additional calculated fields
        dto.setInStock(product.getQuantity() > 0);
        dto.setFinalPrice(product.getSpecialPrice() != null ? product.getSpecialPrice() : product.getPrice());
        
        // Set category info
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getCategoryId());
            dto.setCategoryName(product.getCategory().getCategoryName());
        }
        
        return dto;
    }
    
    private String saveImage(MultipartFile image) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return fileName;
    }
    
    private void publishProductEvent(String eventType, Product product) {
        try {
            ProductEvent event = new ProductEvent(eventType, product.getProductId(), convertToDTO(product));
            kafkaTemplate.send(productEventsTopic, event);
            log.info("Published {} event for product: {}", eventType, product.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish product event", e);
        }
    }
    
    private void publishInventoryEvent(String eventType, Product product, Integer quantity) {
        try {
            InventoryEvent event = new InventoryEvent(eventType, product.getProductId(), quantity, product.getQuantity(), product.getSellerId());
            kafkaTemplate.send("inventory-events", event);
            log.info("Published {} event for product: {} with quantity: {}", eventType, product.getProductId(), quantity);
        } catch (Exception e) {
            log.error("Failed to publish inventory event", e);
        }
    }
    
    // Event classes
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProductEvent {
        private String eventType;
        private Long productId;
        private ProductDTO product;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class InventoryEvent {
        private String eventType;
        private Long productId;
        private Integer quantityChanged;
        private Integer newQuantity;
        private Long sellerId;
    }
    
    // Elasticsearch sync methods
    private void indexProductInElasticsearch(Product product) {
        try {
            ProductDocument document = convertToDocument(product);
            productSearchService.indexProduct(document);
            log.info("Indexed product in Elasticsearch: {}", product.getProductId());
        } catch (Exception e) {
            log.error("Failed to index product in Elasticsearch: {}", product.getProductId(), e);
            // Don't fail the transaction if ES indexing fails
        }
    }
    
    private void deleteProductFromElasticsearch(Long productId) {
        try {
            productSearchService.deleteProductFromIndex(productId);
            log.info("Deleted product from Elasticsearch: {}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product from Elasticsearch: {}", productId, e);
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
            .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
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
        if (product.getCategory() != null && product.getCategory().getParent() != null) {
            Category parentCategory = product.getCategory().getParent();
            document.setCategoryHierarchy(ProductDocument.CategoryHierarchy.builder()
                .parentId(parentCategory.getCategoryId())
                .parentName(parentCategory.getCategoryName())
                .childId(product.getCategory().getCategoryId())
                .childName(product.getCategory().getCategoryName())
                .build());
        }
        
        return document;
    }
} 