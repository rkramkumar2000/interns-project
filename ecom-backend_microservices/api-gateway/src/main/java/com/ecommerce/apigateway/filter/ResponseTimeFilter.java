package com.ecommerce.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ResponseTimeFilter implements GlobalFilter, Ordered {
    
    private static final String START_TIME = "startTime";
    private static final String RESPONSE_TIME_HEADER = "X-Response-Time";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Record start time
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME);
            if (startTime != null) {
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                // Add response time header
                exchange.getResponse().getHeaders().add(RESPONSE_TIME_HEADER, 
                        String.valueOf(responseTime) + "ms");
                
                // Log slow requests
                if (responseTime > 1000) {
                    log.warn("Slow request detected: {} {} took {}ms", 
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath(), 
                            responseTime);
                }
                
                log.debug("Request {} completed in {}ms", 
                        exchange.getRequest().getPath(), responseTime);
            }
        }));
    }
    
    @Override
    public int getOrder() {
        return 0; // Execute after request ID filter
    }
} 