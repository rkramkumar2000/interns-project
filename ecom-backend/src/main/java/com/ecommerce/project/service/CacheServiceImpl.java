package com.ecommerce.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @CacheEvict(value = {"products", "productsByCategory", "productsByKeyword", "categories", "carts", "orders"}, allEntries = true)
    public void evictAllCaches() {
        // This annotation will clear all caches
    }

    @Override
    public void evictCache(String cacheName) {
        Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
    }

    @Override
    public void evictCacheByKey(String cacheName, String key) {
        Objects.requireNonNull(cacheManager.getCache(cacheName)).evict(key);
    }

    @Override
    @CacheEvict(value = {"products", "productsByCategory", "productsByKeyword"}, allEntries = true)
    public void clearProductCaches() {
        // Clears all product-related caches
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void clearCategoryCaches() {
        // Clears all category caches
    }

    @Override
    @CacheEvict(value = "carts", allEntries = true)
    public void clearCartCaches() {
        // Clears all cart caches
    }

    @Override
    @CacheEvict(value = "orders", allEntries = true)
    public void clearOrderCaches() {
        // Clears all order caches
    }

    @Override
    @CacheEvict(value = {"userDetails", "sellers"}, allEntries = true)
    public void clearUserCaches() {
        // Clears all user-related caches
    }

    @Override
    @CacheEvict(value = "userDetails", key = "#username")
    public void evictUserDetailsCache(String username) {
        // Evicts specific user from cache
    }
} 