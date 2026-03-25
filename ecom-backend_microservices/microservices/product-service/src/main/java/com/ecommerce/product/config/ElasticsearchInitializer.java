package com.ecommerce.product.config;

import com.ecommerce.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchInitializer {
    
    private final ProductSearchService productSearchService;
    
    @Bean
    @ConditionalOnProperty(name = "elasticsearch.reindex.on-startup", havingValue = "true")
    public CommandLineRunner reindexProducts() {
        return args -> {
            log.info("Starting Elasticsearch reindex on startup...");
            try {
                productSearchService.reindexAllProducts();
                log.info("Elasticsearch reindex initiated successfully");
            } catch (Exception e) {
                log.error("Failed to reindex products on startup", e);
            }
        };
    }
} 