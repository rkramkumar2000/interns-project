package com.ecommerce.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth routes
                .route("auth_route", r -> r
                        .path("/api/auth/**")
                        .uri("http://localhost:8080"))
                
                // Product routes
                .route("product_route", r -> r
                        .path("/api/products/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("productService")
                                        .setFallbackUri("forward:/fallback/products")))
                        .uri("http://localhost:8080"))
                
                // Category routes
                .route("category_route", r -> r
                        .path("/api/categories/**")
                        .uri("http://localhost:8080"))
                
                // Cart routes
                .route("cart_route", r -> r
                        .path("/api/carts/**")
                        .uri("http://localhost:8080"))
                
                // Order routes
                .route("order_route", r -> r
                        .path("/api/orders/**")
                        .uri("http://localhost:8080"))
                
                // User routes
                .route("user_route", r -> r
                        .path("/api/users/**")
                        .uri("http://localhost:8080"))
                
                // Address routes
                .route("address_route", r -> r
                        .path("/api/addresses/**")
                        .uri("http://localhost:8080"))
                
                // Cache management routes (Admin only)
                .route("cache_route", r -> r
                        .path("/api/cache/**")
                        .uri("http://localhost:8080"))
                
                // Static resources (images)
                .route("images_route", r -> r
                        .path("/images/**")
                        .uri("http://localhost:8080"))
                
                .build();
    }
} 