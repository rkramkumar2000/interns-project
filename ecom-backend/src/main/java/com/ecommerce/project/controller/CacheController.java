package com.ecommerce.project.controller;

import com.ecommerce.project.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache Management", description = "Cache management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @Operation(summary = "Clear all caches")
    @DeleteMapping("/all")
    public ResponseEntity<String> clearAllCaches() {
        cacheService.evictAllCaches();
        return new ResponseEntity<>("All caches cleared successfully", HttpStatus.OK);
    }

    @Operation(summary = "Clear specific cache")
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
        cacheService.evictCache(cacheName);
        return new ResponseEntity<>("Cache '" + cacheName + "' cleared successfully", HttpStatus.OK);
    }

    @Operation(summary = "Clear product caches")
    @DeleteMapping("/products")
    public ResponseEntity<String> clearProductCaches() {
        cacheService.clearProductCaches();
        return new ResponseEntity<>("Product caches cleared successfully", HttpStatus.OK);
    }

    @Operation(summary = "Clear category caches")
    @DeleteMapping("/categories")
    public ResponseEntity<String> clearCategoryCaches() {
        cacheService.clearCategoryCaches();
        return new ResponseEntity<>("Category caches cleared successfully", HttpStatus.OK);
    }

    @Operation(summary = "Clear cart caches")
    @DeleteMapping("/carts")
    public ResponseEntity<String> clearCartCaches() {
        cacheService.clearCartCaches();
        return new ResponseEntity<>("Cart caches cleared successfully", HttpStatus.OK);
    }

    @Operation(summary = "Clear order caches")
    @DeleteMapping("/orders")
    public ResponseEntity<String> clearOrderCaches() {
        cacheService.clearOrderCaches();
        return new ResponseEntity<>("Order caches cleared successfully", HttpStatus.OK);
    }
} 