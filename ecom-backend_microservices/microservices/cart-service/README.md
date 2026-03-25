# Cart Service

## Overview
The Cart Service manages shopping cart functionality for both authenticated users and anonymous sessions. It provides cart persistence, item management, stock validation, and seamless cart merging when anonymous users authenticate. The service integrates with Product Service for real-time stock validation and pricing.

## Architecture Position
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│ API Gateway │────▶│ Cart Service │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                    ┌──────────┴───────────┐
                                    │          │           │
                               ┌────▼───┐ ┌───▼───┐ ┌─────▼────────┐
                               │PostgreSQL│ │ Redis │ │Product Service│
                               └────────┘ └───────┘ └──────────────┘
```

## Technologies Used

### Core Framework
- **Spring Boot 3.2.0** - Microservice framework
- **Spring Data JPA** - ORM for database operations
- **Spring Web** - REST API development

### Data Storage
- **PostgreSQL 15** - Primary database for cart persistence
  - Why: ACID compliance, complex queries, relational data
- **Redis 7** - Caching layer
  - Why: Fast access for active carts, session storage

### Service Communication
- **Feign Client** - Declarative REST client
  - Why: Simple inter-service communication with Product Service
- **Resilience4j** - Circuit breaker and retry
  - Why: Fault tolerance for service calls
- **Apache Kafka** - Event streaming
  - Why: Publishing cart events for analytics

### Security & Configuration
- **Spring Security** - JWT-based authentication
  - Why: Secure cart access, user isolation
- **Eureka Client** - Service discovery
  - Why: Dynamic service location
- **Spring Cloud Config** - Configuration management
  - Why: Centralized configuration

### Development Tools
- **Lombok** - Boilerplate reduction
  - Why: Cleaner code
- **ModelMapper** - Object mapping
  - Why: DTO conversions
- **Swagger/OpenAPI 3** - API documentation
  - Why: Interactive documentation

## Project Structure

```
cart-service/
├── src/main/java/com/ecommerce/cart/
│   ├── CartServiceApplication.java           # Main application class
│   ├── client/
│   │   ├── ProductServiceClient.java         # Feign client interface
│   │   └── ProductServiceFallback.java       # Circuit breaker fallback
│   ├── config/
│   │   └── ApplicationConfig.java            # Bean configurations
│   ├── controller/
│   │   └── CartController.java               # REST endpoints
│   ├── dto/                                  # Data Transfer Objects
│   │   ├── CartDTO.java                      # Cart representation
│   │   ├── CartItemDTO.java                  # Cart item representation
│   │   ├── AddToCartDTO.java                 # Add item request
│   │   ├── UpdateQuantityDTO.java            # Update quantity request
│   │   ├── ProductDTO.java                   # Product from service
│   │   └── CartResponse.java                 # API response wrapper
│   ├── event/
│   │   ├── CartEvent.java                    # Base cart event
│   │   ├── CartCreatedEvent.java             # Cart creation
│   │   ├── CartUpdatedEvent.java             # Cart modification
│   │   └── CartAbandonedEvent.java           # Abandoned cart
│   ├── exception/
│   │   ├── ResourceNotFoundException.java    # Not found errors
│   │   ├── CartException.java                # Business logic errors
│   │   └── GlobalExceptionHandler.java       # Error handling
│   ├── model/
│   │   ├── Cart.java                         # Cart entity
│   │   ├── CartItem.java                     # Cart item entity
│   │   └── CartStatus.java                   # Cart status enum
│   ├── repository/
│   │   ├── CartRepository.java               # Cart data access
│   │   └── CartItemRepository.java           # Cart item data access
│   ├── scheduler/
│   │   └── CartCleanupScheduler.java         # Cleanup abandoned carts
│   ├── security/                             # Security components
│   │   ├── JwtAuthenticationFilter.java      # JWT validation
│   │   ├── JwtAuthenticationToken.java       # Custom auth token
│   │   ├── JwtAuthenticationEntryPoint.java  # Unauthorized handler
│   │   └── SecurityConfig.java               # Security configuration
│   └── service/
│       ├── CartService.java                  # Service interface
│       └── CartServiceImpl.java              # Service implementation
├── src/main/resources/
│   └── bootstrap.yml                         # Config server connection
└── pom.xml                                   # Maven dependencies
```

## Key Components

### 1. CartController
Main REST controller for cart operations:

```java
@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    // Anonymous cart operations
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<CartDTO> getCartBySession(@PathVariable String sessionId)
    
    @PostMapping("/session/{sessionId}/items")
    public ResponseEntity<CartDTO> addToSessionCart(
        @PathVariable String sessionId,
        @Valid @RequestBody AddToCartDTO addToCartDTO)
    
    // Authenticated user operations
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> getCurrentUserCart()
    
    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> addToCart(@Valid @RequestBody AddToCartDTO addToCartDTO)
    
    @PutMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> updateQuantity(
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateQuantityDTO updateDTO)
    
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> removeFromCart(@PathVariable Long itemId)
    
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearCart()
    
    @PostMapping("/merge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> mergeCart(@RequestParam String sessionId)
    
    @PostMapping("/apply-coupon")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> applyCoupon(@RequestParam String couponCode)
    
    @DeleteMapping("/remove-coupon")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartDTO> removeCoupon()
}
```

### 2. Cart Entity
JPA entity with comprehensive cart management:

```java
@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;
    
    private Long userId;
    private String sessionId;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();
    
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private Integer totalItems = 0;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.ACTIVE;
    
    private String couponCode;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastActivityAt;
    
    // Helper methods
    public void recalculateTotals() {
        this.subtotal = cartItems.stream()
            .map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalDiscount = cartItems.stream()
            .map(CartItem::getDiscount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(couponDiscount);
        
        this.totalPrice = subtotal.subtract(totalDiscount);
        this.totalItems = cartItems.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
    }
}
```

### 3. ProductServiceClient
Feign client for Product Service integration:

```java
@FeignClient(
    name = "product-service",
    fallback = ProductServiceFallback.class
)
public interface ProductServiceClient {
    
    @GetMapping("/api/products/{productId}")
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
    @Retry(name = "product-service")
    ProductDTO getProduct(@PathVariable Long productId);
    
    @PutMapping("/api/products/{productId}/stock")
    @CircuitBreaker(name = "product-service", fallbackMethod = "updateStockFallback")
    @Retry(name = "product-service")
    ProductDTO updateStock(
        @PathVariable Long productId,
        @RequestParam Integer quantity,
        @RequestParam String operation);
    
    @PostMapping("/api/products/validate-stock")
    @CircuitBreaker(name = "product-service", fallbackMethod = "validateStockFallback")
    Map<Long, Boolean> validateStock(@RequestBody Map<Long, Integer> productQuantities);
}
```

## Database Schema

### Tables

#### carts
| Column | Type | Description |
|--------|------|-------------|
| cart_id | BIGINT | Primary key |
| user_id | BIGINT | User identifier (null for anonymous) |
| session_id | VARCHAR(255) | Session ID for anonymous carts |
| total_price | DECIMAL(10,2) | Total after discounts |
| total_items | INTEGER | Total item count |
| total_discount | DECIMAL(10,2) | Total discounts |
| subtotal | DECIMAL(10,2) | Price before discounts |
| status | VARCHAR(20) | ACTIVE, MERGED, CONVERTED, ABANDONED |
| coupon_code | VARCHAR(50) | Applied coupon |
| coupon_discount | DECIMAL(10,2) | Coupon discount amount |
| created_at | TIMESTAMP | Cart creation |
| updated_at | TIMESTAMP | Last update |
| last_activity_at | TIMESTAMP | Last user activity |

#### cart_items
| Column | Type | Description |
|--------|------|-------------|
| cart_item_id | BIGINT | Primary key |
| cart_id | BIGINT | Foreign key to carts |
| product_id | BIGINT | Product identifier |
| product_name | VARCHAR(200) | Denormalized name |
| product_image | VARCHAR(255) | Denormalized image |
| product_price | DECIMAL(10,2) | Price at addition time |
| available_stock | INTEGER | Stock at last check |
| quantity | INTEGER | Item quantity |
| discount | DECIMAL(10,2) | Item discount |
| total_price | DECIMAL(10,2) | Item total |
| created_at | TIMESTAMP | Addition time |
| updated_at | TIMESTAMP | Last update |

## API Endpoints

### Anonymous Cart Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cart/session/{sessionId}` | Get cart by session |
| POST | `/api/cart/session/{sessionId}/items` | Add item to session cart |
| PUT | `/api/cart/session/{sessionId}/items/{itemId}` | Update item in session cart |
| DELETE | `/api/cart/session/{sessionId}/items/{itemId}` | Remove item from session cart |
| DELETE | `/api/cart/session/{sessionId}` | Clear session cart |

### Authenticated Cart Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cart` | Get current user's cart |
| POST | `/api/cart/items` | Add item to cart |
| PUT | `/api/cart/items/{itemId}` | Update item quantity |
| DELETE | `/api/cart/items/{itemId}` | Remove item |
| DELETE | `/api/cart` | Clear cart |
| POST | `/api/cart/merge` | Merge anonymous cart |
| POST | `/api/cart/apply-coupon` | Apply coupon |
| DELETE | `/api/cart/remove-coupon` | Remove coupon |
| GET | `/api/cart/summary` | Get cart summary |

## Configuration

### Environment Variables
```yaml
# Database
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: cart_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# Service URLs
PRODUCT_SERVICE_URL: http://product-service

# Kafka
KAFKA_BOOTSTRAP_SERVERS: localhost:9092

# Cart Settings
CART_EXPIRY_DAYS: 7
ANONYMOUS_CART_EXPIRY_DAYS: 30
MAX_ITEMS_PER_CART: 50
```

### Application Properties
```yaml
server:
  port: 8084

spring:
  application:
    name: cart-service
  
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/cart_db
  
  jpa:
    hibernate:
      ddl-auto: update
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      topic:
        cart-events: cart-events

resilience4j:
  circuitbreaker:
    instances:
      product-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60000
        minimum-number-of-calls: 5
  
  retry:
    instances:
      product-service:
        max-attempts: 3
        wait-duration: 1000
        retry-exceptions:
          - java.io.IOException
          - java.net.ConnectException

app:
  cart:
    expiry-days: ${CART_EXPIRY_DAYS:7}
    anonymous-expiry-days: ${ANONYMOUS_CART_EXPIRY_DAYS:30}
    max-items: ${MAX_ITEMS_PER_CART:50}
```

## Events Published

### CART_CREATED
```json
{
  "eventType": "CART_CREATED",
  "timestamp": "2024-01-20T10:30:00Z",
  "cartId": 456,
  "userId": 123,
  "sessionId": "session-abc-123",
  "source": "WEB"
}
```

### CART_UPDATED
```json
{
  "eventType": "CART_UPDATED",
  "timestamp": "2024-01-20T10:35:00Z",
  "cartId": 456,
  "userId": 123,
  "updateType": "ITEM_ADDED",
  "productId": 789,
  "quantity": 2,
  "cartTotal": 599.98
}
```

### CART_ABANDONED
```json
{
  "eventType": "CART_ABANDONED",
  "timestamp": "2024-01-20T10:40:00Z",
  "cartId": 456,
  "userId": 123,
  "cartValue": 599.98,
  "itemCount": 3,
  "lastActivityDaysAgo": 7,
  "products": [789, 790, 791]
}
```

## Business Logic

### 1. Cart Merging
When an anonymous user logs in:
1. Check if user has existing cart
2. If yes, merge anonymous cart items
3. Handle duplicate products by summing quantities
4. Validate stock for merged quantities
5. Preserve user's existing coupon if any

### 2. Stock Validation
Real-time stock checking:
1. Validate stock on item addition
2. Re-validate on quantity update
3. Check stock before checkout
4. Update cached stock information
5. Handle out-of-stock scenarios

### 3. Price Management
Dynamic pricing updates:
1. Store price at addition time
2. Show current price vs cart price
3. Alert users to price changes
4. Recalculate totals on updates
5. Apply discounts and coupons

### 4. Cart Cleanup
Automated maintenance:
1. Mark inactive carts as abandoned
2. Clean up old anonymous carts
3. Archive converted carts
4. Publish abandonment events
5. Free up database resources

## Caching Strategy

### Redis Cache Keys
- `cart::user::{userId}` - User's active cart (TTL: 2 hours)
- `cart::session::{sessionId}` - Anonymous cart (TTL: 24 hours)
- `cart::item::stock::{productId}` - Product stock cache (TTL: 5 minutes)
- `cart::summary::user::{userId}` - Cart summary (TTL: 15 minutes)

### Cache Operations
- **Write-through**: Update cache on cart modifications
- **Cache-aside**: Load from DB if cache miss
- **Eviction**: Clear cache on cart clear/merge
- **Refresh**: Update stock info periodically

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -Pintegration-tests
```

### Manual Testing Examples

1. **Add to Cart (Anonymous):**
```bash
curl -X POST http://localhost:8084/api/cart/session/test-session-123/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

2. **Get User Cart:**
```bash
curl -X GET http://localhost:8084/api/cart \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

3. **Update Quantity:**
```bash
curl -X PUT http://localhost:8084/api/cart/items/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

4. **Merge Anonymous Cart:**
```bash
curl -X POST "http://localhost:8084/api/cart/merge?sessionId=test-session-123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Performance Optimizations

### 1. Database
- Indexes on userId, sessionId, and status
- Batch operations for cart items
- Optimistic locking for concurrent updates
- Connection pooling

### 2. Caching
- Active cart caching in Redis
- Product information caching
- Lazy loading of cart items
- Cache warming for active users

### 3. Service Calls
- Circuit breaker for Product Service
- Retry with exponential backoff
- Asynchronous stock validation
- Bulk product fetching

## Monitoring

### Health Endpoints
- `/actuator/health` - Overall health
- `/actuator/health/db` - Database health
- `/actuator/health/redis` - Redis health
- `/actuator/metrics/cart.operations` - Cart metrics

### Key Metrics
- Cart creation rate
- Average cart value
- Abandonment rate
- Item addition frequency
- Stock validation failures

## Troubleshooting

### Common Issues

1. **Stock Validation Failed**
   - Check Product Service availability
   - Verify circuit breaker status
   - Review fallback responses
   - Check network connectivity

2. **Cart Merge Conflicts**
   - Validate merge logic
   - Check for duplicate items
   - Verify stock availability
   - Review transaction boundaries

3. **Performance Issues**
   - Monitor cache hit rate
   - Check database query performance
   - Review service call latency
   - Analyze cart size distribution

## Best Practices

1. **Data Consistency**
   - Use transactions for cart updates
   - Implement idempotent operations
   - Handle concurrent modifications
   - Validate business rules

2. **User Experience**
   - Real-time stock updates
   - Clear error messages
   - Preserve cart state
   - Handle edge cases gracefully

3. **Security**
   - Validate user cart ownership
   - Sanitize input data
   - Rate limit cart operations
   - Audit cart modifications

## Future Enhancements

1. **Advanced Features**
   - Wishlist functionality
   - Save for later
   - Cart sharing
   - Price drop notifications
   - Bulk operations

2. **Personalization**
   - Recommended additions
   - Frequently bought together
   - Personalized coupons
   - Smart cart suggestions

3. **Analytics**
   - Cart analytics dashboard
   - Abandonment analysis
   - User behavior tracking
   - A/B testing support 