package com.ecommerce.cart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract pre-validated user info from headers (set by API Gateway)
            String userId = request.getHeader("X-User-Id");
            String userEmail = request.getHeader("X-User-Email");
            String userRoles = request.getHeader("X-User-Roles");
            
            if (userId != null && userEmail != null) {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(
                        userRoles != null ? userRoles.split(",") : new String[0])
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                
                // Create authentication token from trusted headers
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                        userEmail, Long.parseLong(userId), authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Set authentication from headers for user: {} with authorities: {}", 
                         userEmail, authorities);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication from headers: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        // Allow public endpoints without authentication
        return path.startsWith("/actuator/") || 
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui");
    }
} 