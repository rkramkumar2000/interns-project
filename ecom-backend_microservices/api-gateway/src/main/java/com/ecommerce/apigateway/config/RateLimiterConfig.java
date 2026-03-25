package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    /**
     * Rate limiting based on user's email from JWT
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Get user email from the X-User-Email header (set by AuthenticationFilter)
            String userEmail = exchange.getRequest().getHeaders().getFirst("X-User-Email");
            
            // If no user email, use IP address as fallback
            if (userEmail == null || userEmail.isEmpty()) {
                String ip = extractIpAddress(exchange.getRequest().getRemoteAddress());
                return Mono.just(ip);
            }
            
            return Mono.just(userEmail);
        };
    }

    /**
     * Rate limiting based on IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = extractIpAddress(exchange.getRequest().getRemoteAddress());
            return Mono.just(ip);
        };
    }
    
    private String extractIpAddress(InetSocketAddress remoteAddress) {
        return Optional.ofNullable(remoteAddress)
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .orElse("anonymous");
    }
} 