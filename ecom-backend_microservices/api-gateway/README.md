# API Gateway Service

## Overview
The API Gateway is the single entry point for all client requests in the microservices architecture. Built with Spring Cloud Gateway, it provides intelligent request routing, load balancing, security, rate limiting, and cross-cutting concerns like authentication validation and circuit breaking.

## Architecture Position
```
┌─────────────┐          ┌─────────────┐          ┌──────────────┐
│   Frontend  │─────────▶│ API Gateway │─────────▶│Microservices │
│   (React)   │          │  Port 8080  │          │              │
└─────────────┘          └─────────────┘          └──────────────┘
                                │
                         ┌──────┴───────┐
                         │              │
                    ┌────▼────┐   ┌────▼────┐
                    │ Eureka  │   │ Config  │
                    │ Server  │   │ Server  │
                    └─────────┘   └─────────┘
```

## Technologies Used

### Core Framework
- **Spring Cloud Gateway 4.1.0** - Reactive gateway framework
  - Why: Non-blocking, high-performance routing with excellent Spring integration
- **Spring WebFlux** - Reactive web framework
  - Why: Handles high concurrency with reactive streams
- **Project Reactor** - Reactive programming
  - Why: Asynchronous, non-blocking request handling

### Service Integration
- **Eureka Client** - Service discovery
  - Why: Dynamic routing to healthy service instances
- **Spring Cloud LoadBalancer** - Client-side load balancing
  - Why: Distributes traffic across service instances

### Resilience & Monitoring
- **Resilience4j** - Circuit breaker, rate limiter
  - Why: Fault tolerance and traffic management
- **Spring Boot Actuator** - Monitoring endpoints
  - Why: Health checks and metrics
- **Micrometer** - Metrics collection
  - Why: Integration with Prometheus

### Security
- **Spring Security** - Security framework
  - Why: Request filtering and authorization
- **JWT Validation** - Token verification
  - Why: Stateless authentication across services

## Project Structure

```
api-gateway/
├── src/main/java/com/ecommerce/apigateway/
│   ├── ApiGatewayApplication.java           # Main application with route definitions
│   ├── config/
│   │   └── RateLimiterConfig.java           # Rate limiting configuration
│   ├── filter/
│   │   ├── AuthenticationFilter.java        # JWT validation
│   │   ├── LoggingFilter.java               # Request/response logging
│   │   ├── RequestIdFilter.java             # Correlation ID generation
│   │   └── ResponseTimeFilter.java          # Performance monitoring
│   ├── controller/
│   │   └── FallbackController.java          # Circuit breaker fallback responses
│   ├── exception/
│   │   └── GlobalErrorWebExceptionHandler.java # Centralized error handling
│   └── util/
│       └── JwtUtil.java                     # JWT validation utilities
├── src/main/resources/
│   └── application.yml                      # Complete configuration (routes, CORS, eureka)
└── pom.xml                                  # Dependencies
```

## Route Configuration

### Service Routes
```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth Service
        - id: auth-service
          uri: lb://AUTH-SERVICE
          predicates:
            - Path=/api/auth/**
          filters:
            - name: CircuitBreaker
              args:
                name: auth-service
                fallbackUri: forward:/fallback/auth
            
        # Product Service - Public Routes
        - id: product-public
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/public/products/**
          filters:
            - name: CircuitBreaker
              args:
                name: product-service
                fallbackUri: forward:/fallback/product
                
        # Cart Service
        - id: cart-service
          uri: lb://cart-service
          predicates:
            - Path=/api/cart/**
          filters:
            - StripPrefix=1
            - AuthenticationFilter
            
        # Order Service
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**, /api/admin/orders/**
          filters:
            - StripPrefix=1
            - AuthenticationFilter
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY
                
        # Payment Service
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**, /api/webhooks/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

## Key Components

### 1. AuthenticationFilter
Global filter for JWT validation:

```java
@Component
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                            GatewayFilterChain chain) {
        
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip auth for public endpoints
        if (isPublicEndpoint(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        
        // Extract JWT token
        String token = extractToken(request);
        if (token == null) {
            return onError(exchange, "Missing authorization header", 
                          HttpStatus.UNAUTHORIZED);
        }
        
        // Validate token
        try {
            Claims claims = jwtUtil.validateToken(token);
            
            // Add user info to headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", claims.get("userId").toString())
                .header("X-User-Roles", claims.get("roles").toString())
                .build();
            
            return chain.filter(
                exchange.mutate().request(modifiedRequest).build()
            );
            
        } catch (Exception e) {
            return onError(exchange, "Invalid token", 
                          HttpStatus.UNAUTHORIZED);
        }
    }
}
```

### 2. Circuit Breaker Configuration
Resilience patterns for fault tolerance:

```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> 
           defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slowCallRateThreshold(50)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .build())
            .build());
    }
}
```

### 3. Rate Limiting
Request rate limiting configuration:

```java
@Configuration
public class RateLimiterConfig {
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }
}
```

## Security Configuration

### CORS Configuration
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            if (CorsUtils.isCorsRequest(request)) {
                ServerHttpResponse response = ctx.getResponse();
                HttpHeaders headers = response.getHeaders();
                headers.add("Access-Control-Allow-Origin", "http://localhost:5173");
                headers.add("Access-Control-Allow-Methods", 
                           "GET, PUT, POST, DELETE, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "*");
                headers.add("Access-Control-Allow-Credentials", "true");
                headers.add("Access-Control-Max-Age", "3600");
                if (request.getMethod() == HttpMethod.OPTIONS) {
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
            return chain.filter(ctx);
        };
    }
}
```

### Public Endpoints
Endpoints that don't require authentication:
- `/api/auth/signin`
- `/api/auth/signup`
- `/api/auth/check-username/*`
- `/api/auth/check-email/*`
- `/api/products/**` (GET only)
- `/api/categories/**` (GET only)
- `/api/orders/track/*`
- `/api/webhooks/**`

## Filters

### Global Filters

1. **AuthenticationFilter** - JWT validation
2. **LoggingFilter** - Request/response logging
3. **RequestIdFilter** - Correlation ID generation
4. **ResponseTimeFilter** - Performance monitoring

### Route-Specific Filters

1. **StripPrefix** - Remove API prefix from path
2. **CircuitBreaker** - Fault tolerance
3. **Retry** - Automatic retry on failure
4. **RequestRateLimiter** - Rate limiting
5. **RewritePath** - Path transformation

## Error Handling

### Global Exception Handler
```java
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends 
        AbstractErrorWebExceptionHandler {
    
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(
            ErrorAttributes errorAttributes) {
        
        return RouterFunctions.route(
            RequestPredicates.all(), 
            this::renderErrorResponse
        );
    }
    
    private Mono<ServerResponse> renderErrorResponse(
            ServerRequest request) {
        
        Map<String, Object> errorAttributes = getErrorAttributes(
            request, ErrorAttributeOptions.defaults()
        );
        
        int statusCode = (int) errorAttributes.getOrDefault(
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ServerResponse
            .status(statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorAttributes));
    }
}
```

### Fallback Responses
```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @GetMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productsFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product service is temporarily unavailable");
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("timestamp", LocalDateTime.now());
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response));
    }
}
```

## Configuration

### Application Properties
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
    
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
      
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "http://localhost:5173"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,gateway
  metrics:
    tags:
      application: ${spring.application.name}

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty.http.client: DEBUG

jwt:
  secret: ${JWT_SECRET:mySecretKey}
```

## Monitoring

### Health Endpoints
- `/actuator/health` - Overall health
- `/actuator/gateway/routes` - Route information
- `/actuator/gateway/globalfilters` - Global filters
- `/actuator/gateway/routefilters` - Route filters
- `/actuator/metrics` - Performance metrics

### Key Metrics
- Gateway request count
- Response time per route
- Circuit breaker status
- Rate limiter statistics
- Error rates

### Prometheus Metrics
```
# Gateway requests
gateway_requests_total{route="product-service",status="200"} 
gateway_request_duration_seconds{route="product-service"}

# Circuit breaker
resilience4j_circuitbreaker_state{name="product-service"}
resilience4j_circuitbreaker_failure_rate{name="product-service"}

# Rate limiter
spring_cloud_gateway_rate_limiter_allowed_total
spring_cloud_gateway_rate_limiter_denied_total
```

## Testing

### Integration Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class GatewayIntegrationTest {
    
    @Test
    public void testProductRouting() {
        webTestClient.get()
            .uri("/api/products")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Response-Time");
    }
    
    @Test
    public void testAuthenticationRequired() {
        webTestClient.get()
            .uri("/api/orders")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
```

### Load Testing
```bash
# Using Apache Bench
ab -n 1000 -c 10 http://localhost:8080/api/products

# Using curl
for i in {1..100}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products
done
```

## Performance Tuning

### Connection Pool
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 1000
        response-timeout: 5s
        pool:
          type: elastic
          max-idle-time: 10s
          max-life-time: 60s
```

### Memory Settings
```bash
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -jar api-gateway.jar
```

## Troubleshooting

### Common Issues

1. **Service Not Found**
   - Check Eureka registration
   - Verify service name in routes
   - Check network connectivity

2. **Circuit Breaker Open**
   - Check downstream service health
   - Review circuit breaker thresholds
   - Check timeout settings

3. **Rate Limit Exceeded**
   - Review rate limit configuration
   - Check Redis connectivity
   - Monitor usage patterns

4. **CORS Issues**
   - Verify allowed origins
   - Check preflight handling
   - Review allowed headers

## Best Practices

1. **Security**
   - Validate all incoming requests
   - Sanitize headers
   - Implement rate limiting
   - Use HTTPS in production

2. **Performance**
   - Enable response caching
   - Optimize route predicates
   - Use connection pooling
   - Monitor response times

3. **Reliability**
   - Configure circuit breakers
   - Implement retry logic
   - Add fallback responses
   - Health check integration

## Future Enhancements

1. **Advanced Features**
   - Request/Response transformation
   - API versioning
   - WebSocket support
   - GraphQL gateway

2. **Security**
   - OAuth2 integration
   - API key management
   - Request signing
   - DDoS protection

3. **Monitoring**
   - Distributed tracing
   - Real-time dashboards
   - Alerting rules
   - API analytics 