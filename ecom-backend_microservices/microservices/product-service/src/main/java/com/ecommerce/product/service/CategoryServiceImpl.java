package com.ecommerce.product.service;

import com.ecommerce.product.dto.CategoryDTO;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.repository.jpa.CategoryRepository;
import com.ecommerce.product.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        log.info("Creating new category: {}", categoryDTO.getCategoryName());
        
        Category category = modelMapper.map(categoryDTO, Category.class);
        
        // Set parent category if provided
        if (categoryDTO.getParentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", categoryDTO.getParentId()));
            category.setParent(parent);
        }
        
        Category savedCategory = categoryRepository.save(category);
        
        // Publish category created event
        publishCategoryEvent("CATEGORY_CREATED", savedCategory);
        
        return convertToDTO(savedCategory);
    }
    
    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        log.info("Updating category with id: {}", categoryId);
        
        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        // Update fields
        existingCategory.setCategoryName(categoryDTO.getCategoryName());
        existingCategory.setDescription(categoryDTO.getDescription());
        existingCategory.setImage(categoryDTO.getImage());
        existingCategory.setDisplayOrder(categoryDTO.getDisplayOrder());
        
        // Update parent if changed
        if (categoryDTO.getParentId() != null && 
            (existingCategory.getParent() == null || 
             !categoryDTO.getParentId().equals(existingCategory.getParent().getCategoryId()))) {
            
            // Check for circular reference
            if (categoryDTO.getParentId().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", categoryDTO.getParentId()));
            existingCategory.setParent(parent);
        } else if (categoryDTO.getParentId() == null) {
            existingCategory.setParent(null);
        }
        
        Category updatedCategory = categoryRepository.save(existingCategory);
        
        // Publish category updated event
        publishCategoryEvent("CATEGORY_UPDATED", updatedCategory);
        
        return convertToDTO(updatedCategory);
    }
    
    @Override
    @Cacheable(value = "categories", key = "#categoryId")
    public CategoryDTO getCategoryById(Long categoryId) {
        log.info("Fetching category with id: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        return convertToDTO(category);
    }
    
    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryDTO> getAllCategories() {
        log.info("Fetching all categories");
        
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "categories", key = "'active'")
    public List<CategoryDTO> getActiveCategories() {
        log.info("Fetching active categories");
        
        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "categories", key = "'root'")
    public List<CategoryDTO> getRootCategories() {
        log.info("Fetching root categories");
        
        List<Category> categories = categoryRepository.findByParentIsNullAndActiveTrue();
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "categories", key = "'children-' + #parentId")
    public List<CategoryDTO> getChildCategories(Long parentId) {
        log.info("Fetching child categories for parent: {}", parentId);
        
        List<Category> categories = categoryRepository.findByParentCategoryIdAndActiveTrue(parentId);
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @CacheEvict(value = {"categories", "productsByCategory"}, allEntries = true)
    public void deleteCategory(Long categoryId) {
        log.info("Deleting category with id: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        // Check if category has products
        Long productCount = productRepository.countByCategoryId(categoryId);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category with associated products");
        }
        
        // Check if category has child categories
        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with child categories");
        }
        
        // Soft delete
        category.setActive(false);
        categoryRepository.save(category);
        
        // Publish category deleted event
        publishCategoryEvent("CATEGORY_DELETED", category);
    }
    
    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void activateCategory(Long categoryId) {
        log.info("Activating category: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        category.setActive(true);
        categoryRepository.save(category);
        
        publishCategoryEvent("CATEGORY_ACTIVATED", category);
    }
    
    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void deactivateCategory(Long categoryId) {
        log.info("Deactivating category: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        category.setActive(false);
        categoryRepository.save(category);
        
        publishCategoryEvent("CATEGORY_DEACTIVATED", category);
    }
    
    @Override
    public boolean isCategoryNameUnique(String categoryName, Long excludeId) {
        log.info("Checking if category name is unique: {}", categoryName);
        
        if (excludeId != null) {
            return !categoryRepository.existsByCategoryNameAndCategoryIdNot(categoryName, excludeId);
        } else {
            return categoryRepository.findByCategoryName(categoryName).isEmpty();
        }
    }
    
    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = modelMapper.map(category, CategoryDTO.class);
        
        // Set parent info
        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getCategoryId());
            dto.setParentName(category.getParent().getCategoryName());
        }
        
        // Set product count
        Long productCount = productRepository.countByCategoryId(category.getCategoryId());
        dto.setProductCount(productCount.intValue());
        
        // Map children if needed (avoid deep recursion)
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<CategoryDTO> childDTOs = category.getChildren().stream()
                    .filter(Category::getActive)
                    .map(child -> {
                        CategoryDTO childDTO = new CategoryDTO();
                        childDTO.setCategoryId(child.getCategoryId());
                        childDTO.setCategoryName(child.getCategoryName());
                        childDTO.setActive(child.getActive());
                        childDTO.setDisplayOrder(child.getDisplayOrder());
                        return childDTO;
                    })
                    .collect(Collectors.toList());
            dto.setChildren(childDTOs);
        }
        
        return dto;
    }
    
    private void publishCategoryEvent(String eventType, Category category) {
        try {
            CategoryEvent event = new CategoryEvent(eventType, category.getCategoryId(), convertToDTO(category));
            kafkaTemplate.send("product-events", event);
            log.info("Published {} event for category: {}", eventType, category.getCategoryId());
        } catch (Exception e) {
            log.error("Failed to publish category event", e);
        }
    }
    
    // Event class
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CategoryEvent {
        private String eventType;
        private Long categoryId;
        private CategoryDTO category;
    }
} 