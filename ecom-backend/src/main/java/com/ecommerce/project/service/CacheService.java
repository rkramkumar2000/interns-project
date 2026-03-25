package com.ecommerce.project.service;

public interface CacheService {
    void evictAllCaches();
    void evictCache(String cacheName);
    void evictCacheByKey(String cacheName, String key);
    void clearProductCaches();
    void clearCategoryCaches();
    void clearCartCaches();
    void clearOrderCaches();
    void clearUserCaches();
    void evictUserDetailsCache(String username);
} 