package com.ecommerce.apigateway.filter;

import com.ecommerce.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${jwt.cookieName}")
    private String jwtCookieName;
    
    @Value("${jwt.cache.ttl:300}") // 5 minutes default
    private long cacheJwtTtl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(request.getPath().toString())) {
            return chain.filter(exchange);
        }

        // Extract JWT from cookie or Authorization header
        String jwtFromCookie = extractJwtFromCookie(request);
        String jwtFromHeader = extractJwtFromHeader(request);
        final String jwt = jwtFromCookie != null ? jwtFromCookie : jwtFromHeader;

        if (jwt == null) {
            return onError(exchange, "JWT token is missing", HttpStatus.UNAUTHORIZED);
        }

        // Try to get from cache first
        String cacheKey = "jwt:validation:" + jwt.hashCode();
        
        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(validateAndCacheToken(jwt, cacheKey))
                .flatMap(cachedResult -> {
                    if ("INVALID".equals(cachedResult)) {
                        return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                    }
                    
                    // Parse cached result
                    String[] parts = cachedResult.split("\\|");
                    if (parts.length < 3) {
                        return validateAndCacheToken(jwt, cacheKey)
                                .flatMap(result -> processValidToken(exchange, chain, result));
                    }
                    
                    return processValidToken(exchange, chain, cachedResult);
                })
                .onErrorResume(e -> {
                    log.error("Error processing JWT: {}", e.getMessage());
                    return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                });
    }
    
    @Override
    public int getOrder() {
        return 1; // Execute after response time filter
    }

    private boolean isPublicEndpoint(String path) {
        List<String> publicPaths = List.of(
                "/api/auth/signin",
                "/api/auth/signup",
                "/api/auth/refresh",
                "/api/public/products",
                "/api/public/categories",
                "/api/products",  // backward compatibility
                "/api/categories",  // backward compatibility
                "/images",
                "/actuator/health"
        );
        
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    private String extractJwtFromCookie(ServerHttpRequest request) {
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (cookies.containsKey(jwtCookieName)) {
            HttpCookie cookie = cookies.getFirst(jwtCookieName);
            return cookie != null ? cookie.getValue() : null;
        }
        return null;
    }

    private String extractJwtFromHeader(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    private Mono<String> validateAndCacheToken(String jwt, String cacheKey) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = jwtUtil.validateToken(jwt);
                
                // Extract user information
                Long userId = claims.get("id", Long.class);
                String email = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles", List.class);
                
                // Create cache value
                String cacheValue = (userId != null ? userId : "") + "|" + 
                                  (email != null ? email : "") + "|" + 
                                  (roles != null ? String.join(",", roles) : "");
                
                // Cache the result
                redisTemplate.opsForValue()
                    .set(cacheKey, cacheValue, Duration.ofSeconds(cacheJwtTtl))
                    .subscribe();
                
                return cacheValue;
            } catch (JwtException e) {
                // Cache invalid tokens too (to prevent repeated validation)
                redisTemplate.opsForValue()
                    .set(cacheKey, "INVALID", Duration.ofSeconds(60))
                    .subscribe();
                throw new RuntimeException("Invalid JWT token");
            }
        });
    }
    
    private Mono<Void> processValidToken(ServerWebExchange exchange, GatewayFilterChain chain, String cachedResult) {
        String[] parts = cachedResult.split("\\|", -1);
        String userId = parts[0];
        String email = parts[1];
        String roles = parts[2];
        
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email)
                .header("X-User-Roles", roles)
                .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String errorResponse = String.format("{\"error\": \"%s\"}", error);
        DataBuffer buffer = response.bufferFactory()
                .wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
} 