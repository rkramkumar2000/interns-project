package com.ecommerce.payment.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Propagate authentication headers from API Gateway
                String userId = request.getHeader("X-User-Id");
                String userEmail = request.getHeader("X-User-Email");
                String userRoles = request.getHeader("X-User-Roles");
                String authorization = request.getHeader("Authorization");
                
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }
                if (userRoles != null) {
                    requestTemplate.header("X-User-Roles", userRoles);
                }
                if (authorization != null) {
                    requestTemplate.header("Authorization", authorization);
                }
            }
        };
    }
} 