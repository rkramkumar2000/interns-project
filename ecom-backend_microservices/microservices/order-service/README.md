# Order Service

## Overview
The Order Service is a critical microservice responsible for order processing, management, and fulfillment tracking in the e-commerce platform. It handles order creation from cart data, manages order lifecycle states, integrates with inventory and payment services, and publishes events for downstream processing.

## Architecture Position
```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   Frontend  │────▶│ API Gateway │────▶│Order Service │
└─────────────┘     └─────────────┘     └──────────────┘
                                               │
                            ┌──────────────────┼──────────────────┐
                            │                  │                  │
                       ┌────▼────┐      ┌─────▼──────┐    ┌─────▼────────┐
                       │Cart Svc │      │Product Svc │    │Payment Svc   │
                       └─────────┘      └────────────┘    └──────────────┘
```

## Technologies Used

### Core Framework
- **Spring Boot 3.2.0** - Microservice framework
- **Spring Data JPA** - ORM for PostgreSQL
- **Spring Web** - REST API development

### Data Storage
- **PostgreSQL 15** - Primary database for orders
  - Why: ACID compliance for critical order data, complex queries
- **Redis 7** - Caching layer
  - Why: Fast access for order status, user order history

### Service Communication
- **Feign Client** - REST client for sync calls
  - Why: Simple integration with Cart, Product, and Auth services
- **Resilience4j** - Circuit breaker and retry
  - Why: Fault tolerance for distributed operations
- **Apache Kafka** - Event streaming
  - Why: Publishing order events for fulfillment, notifications

### Additional Features
- **Spring Security** - JWT-based auth
  - Why: Secure order access, user isolation
- **Spring Scheduler** - Background tasks
  - Why: Order status updates, cleanup tasks
- **Spring Async** - Asynchronous processing
  - Why: Non-blocking event publishing

### Development Tools
- **Lombok** - Boilerplate reduction
- **ModelMapper** - Object mapping
- **Jackson** - JSON processing with Java Time support
- **Swagger/OpenAPI 3** - API documentation

## Project Structure

```
order-service/
├── src/main/java/com/ecommerce/order/
│   ├── OrderServiceApplication.java          # Main application class
│   ├── client/                               # Feign clients
│   │   ├── CartServiceClient.java            # Cart integration
│   │   ├── CartServiceFallback.java          # Cart fallback
│   │   ├── ProductServiceClient.java         # Product integration
│   │   └── ProductServiceFallback.java       # Product fallback
│   ├── config/
│   │   └── ApplicationConfig.java            # Bean configurations
│   ├── controller/
│   │   ├── OrderController.java              # Order endpoints
│   │   └── OrderAdminController.java         # Admin endpoints
│   ├── dto/                                  # Data Transfer Objects
│   │   ├── OrderDTO.java                     # Order representation
│   │   ├── OrderItemDTO.java                 # Order item details
│   │   ├── CreateOrderRequest.java           # Order creation
│   │   ├── UpdateOrderStatusRequest.java     # Status update
│   │   ├── AddressDTO.java                   # Delivery address
│   │   ├── OrderResponse.java                # Paginated response
│   │   ├── OrderStatisticsDTO.java           # Order analytics
│   │   ├── CartDTO.java                      # Cart from service
│   │   ├── CartItemDTO.java                  # Cart item details
│   │   └── ProductDTO.java                   # Product from service
│   ├── event/
│   │   ├── OrderEvent.java                   # Base order event
│   │   ├── OrderCreatedEvent.java            # Creation event
│   │   ├── OrderStatusChangedEvent.java      # Status change
│   │   ├── OrderCancelledEvent.java          # Cancellation
│   │   └── OrderDeliveredEvent.java          # Delivery completion
│   ├── exception/
│   │   ├── OrderNotFoundException.java       # Order not found
│   │   ├── OrderException.java               # Business errors
│   │   └── GlobalExceptionHandler.java       # Error handling
│   ├── model/
│   │   ├── Order.java                        # Order entity
│   │   ├── OrderItem.java                    # Order item entity
│   │   └── OrderStatus.java                  # Status enum
│   ├── repository/
│   │   ├── OrderRepository.java              # Order data access
│   │   └── OrderItemRepository.java          # Order item access
│   ├── scheduler/
│   │   ├── OrderStatusScheduler.java         # Status updates
│   │   └── OrderCleanupScheduler.java        # Data cleanup
│   ├── security/                             # Security components
│   │   ├── JwtAuthenticationFilter.java      # JWT validation
│   │   ├── JwtAuthenticationToken.java       # Auth token
│   │   ├── JwtAuthenticationEntryPoint.java  # Error handler
│   │   └── SecurityConfig.java               # Security config
│   └── service/
│       ├── OrderService.java                 # Service interface
│       ├── OrderServiceImpl.java             # Implementation
│       ├── OrderEventPublisher.java          # Event publisher
│       └── OrderNumberGenerator.java         # Order ID generator
├── src/main/resources/
│   └── bootstrap.yml                         # Config server
└── pom.xml                                   # Dependencies
```

## Key Components

### 1. OrderController
Main REST controller for order operations:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> createOrder(
        @Valid @RequestBody CreateOrderRequest request)
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getUserOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String sortBy)
    
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long orderId)
    
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> cancelOrder(
        @PathVariable Long orderId,
        @RequestParam(required = false) String reason)
    
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<OrderDTO> trackOrder(@PathVariable String trackingNumber)
    
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserOrderStatisticsDTO> getUserStatistics()
    
    @PostMapping("/{orderId}/return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDTO> initiateReturn(
        @PathVariable Long orderId,
        @Valid @RequestBody ReturnRequest request)
}
```

### 2. Order Entity
Comprehensive order data model:

```java
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    
    @Column(unique = true)
    private String orderNumber;
    
    private Long userId;
    private String userEmail;
    private String userName;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;  // JSON string
    
    @Column(columnDefinition = "TEXT")
    private String billingAddress;   // JSON string
    
    @CreationTimestamp
    private LocalDateTime orderDate;
    
    private LocalDateTime deliveryDate;
    private LocalDateTime shippedDate;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;
    
    private String paymentId;
    private String paymentMethod;
    private String paymentStatus;
    
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal shippingCharge;
    private BigDecimal totalAmount;
    
    private String couponCode;
    private BigDecimal couponDiscount;
    
    private String trackingNumber;
    private String shippingCarrier;
    
    private String notes;
    private String cancellationReason;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Business methods
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
    
    public void calculateTotals() {
        this.subtotal = orderItems.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = subtotal
            .subtract(discount)
            .subtract(couponDiscount)
            .add(tax)
            .add(shippingCharge);
    }
}
```

### 3. OrderStatus Enum
Order lifecycle states:

```java
public enum OrderStatus {
    PENDING("Order placed, awaiting payment"),
    PAYMENT_PENDING("Awaiting payment confirmation"),
    CONFIRMED("Payment confirmed, preparing for shipment"),
    PROCESSING("Order is being processed"),
    SHIPPED("Order has been shipped"),
    OUT_FOR_DELIVERY("Order is out for delivery"),
    DELIVERED("Order has been delivered"),
    CANCELLED("Order has been cancelled"),
    REFUNDED("Order has been refunded"),
    RETURN_REQUESTED("Return has been requested"),
    RETURN_APPROVED("Return has been approved"),
    RETURNED("Order has been returned"),
    FAILED("Order processing failed");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
}
```

## Database Schema

### Tables

#### orders
| Column | Type | Description |
|--------|------|-------------|
| order_id | BIGINT | Primary key |
| order_number | VARCHAR(50) | Unique order number |
| user_id | BIGINT | User identifier |
| user_email | VARCHAR(100) | User email |
| user_name | VARCHAR(100) | User name |
| delivery_address | TEXT | JSON address data |
| billing_address | TEXT | JSON address data |
| order_date | TIMESTAMP | Order creation |
| delivery_date | TIMESTAMP | Expected delivery |
| shipped_date | TIMESTAMP | Shipment date |
| order_status | VARCHAR(30) | Current status |
| payment_id | VARCHAR(100) | Payment reference |
| payment_method | VARCHAR(50) | Payment type |
| payment_status | VARCHAR(30) | Payment state |
| subtotal | DECIMAL(10,2) | Items total |
| discount | DECIMAL(10,2) | Total discount |
| tax | DECIMAL(10,2) | Tax amount |
| shipping_charge | DECIMAL(10,2) | Shipping cost |
| total_amount | DECIMAL(10,2) | Final amount |
| coupon_code | VARCHAR(50) | Applied coupon |
| coupon_discount | DECIMAL(10,2) | Coupon value |
| tracking_number | VARCHAR(100) | Tracking ID |
| shipping_carrier | VARCHAR(50) | Carrier name |
| notes | TEXT | Order notes |
| cancellation_reason | TEXT | Cancel reason |
| created_at | TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | Last update |

#### order_items
| Column | Type | Description |
|--------|------|-------------|
| order_item_id | BIGINT | Primary key |
| order_id | BIGINT | Foreign key to orders |
| product_id | BIGINT | Product identifier |
| product_name | VARCHAR(200) | Product name |
| product_sku | VARCHAR(50) | Product SKU |
| product_image | VARCHAR(255) | Product image |
| product_description | TEXT | Description |
| unit_price | DECIMAL(10,2) | Price per unit |
| quantity | INTEGER | Item quantity |
| discount | DECIMAL(10,2) | Item discount |
| total_price | DECIMAL(10,2) | Item total |
| created_at | TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | Last update |

## API Endpoints

### Customer Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/orders` | Create order | Required |
| GET | `/api/orders` | List user orders | Required |
| GET | `/api/orders/{orderId}` | Get order details | Required |
| PUT | `/api/orders/{orderId}/cancel` | Cancel order | Required |
| GET | `/api/orders/track/{trackingNumber}` | Track order | Public |
| GET | `/api/orders/statistics` | User statistics | Required |
| POST | `/api/orders/{orderId}/return` | Request return | Required |
| GET | `/api/orders/{orderId}/invoice` | Download invoice | Required |

### Admin Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/admin/orders` | List all orders | ADMIN |
| PUT | `/api/admin/orders/{orderId}/status` | Update status | ADMIN |
| GET | `/api/admin/orders/statistics` | System statistics | ADMIN |
| GET | `/api/admin/orders/export` | Export orders | ADMIN |
| PUT | `/api/admin/orders/{orderId}/assign` | Assign to staff | ADMIN |

## Configuration

### Environment Variables
```yaml
# Database
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: order_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS: localhost:9092

# Order Settings
ORDER_NUMBER_PREFIX: ORD
ORDER_CANCELLATION_WINDOW_HOURS: 24
AUTO_CONFIRM_PAYMENT_MINUTES: 30
```

### Application Properties
```yaml
server:
  port: 8085

spring:
  application:
    name: order-service
  
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/order_db
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false

resilience4j:
  circuitbreaker:
    instances:
      cart-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30000
      product-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30000
  
  retry:
    instances:
      cart-service:
        max-attempts: 3
        wait-duration: 1000
      product-service:
        max-attempts: 3
        wait-duration: 1000

app:
  order:
    number-prefix: ${ORDER_NUMBER_PREFIX:ORD}
    cancellation-window-hours: ${ORDER_CANCELLATION_WINDOW_HOURS:24}
    auto-confirm-minutes: ${AUTO_CONFIRM_PAYMENT_MINUTES:30}
```

## Events Published

### ORDER_CREATED
```json
{
  "eventType": "ORDER_CREATED",
  "timestamp": "2024-01-20T10:30:00Z",
  "orderId": 1001,
  "orderNumber": "ORD-2024-001001",
  "userId": 123,
  "userEmail": "user@example.com",
  "totalAmount": 1299.99,
  "itemCount": 3,
  "paymentMethod": "CREDIT_CARD",
  "deliveryAddress": {
    "city": "New York",
    "state": "NY",
    "zipCode": "10001"
  }
}
```

### ORDER_STATUS_CHANGED
```json
{
  "eventType": "ORDER_STATUS_CHANGED",
  "timestamp": "2024-01-20T11:00:00Z",
  "orderId": 1001,
  "orderNumber": "ORD-2024-001001",
  "oldStatus": "PENDING",
  "newStatus": "CONFIRMED",
  "updatedBy": "SYSTEM",
  "notes": "Payment verified"
}
```

### ORDER_CANCELLED
```json
{
  "eventType": "ORDER_CANCELLED",
  "timestamp": "2024-01-20T11:30:00Z",
  "orderId": 1001,
  "orderNumber": "ORD-2024-001001",
  "userId": 123,
  "reason": "Customer requested cancellation",
  "refundAmount": 1299.99,
  "cancelledBy": "CUSTOMER"
}
```

## Business Logic

### 1. Order Creation Flow
```
1. Receive order request with cartId
2. Fetch cart details from Cart Service
3. Validate cart is not empty
4. Fetch current product details and validate stock
5. Create order with PENDING status
6. Reserve inventory via Product Service
7. Clear user's cart
8. Publish ORDER_CREATED event
9. Return order details
```

### 2. Payment Integration
```
1. Order created with PAYMENT_PENDING status
2. Payment Service processes payment
3. On success: Update to CONFIRMED status
4. On failure: Release inventory, update to FAILED
5. Publish appropriate events
```

### 3. Order Cancellation Rules
- Customer can cancel within 24 hours if not shipped
- Admin can cancel any order with reason
- Cancellation triggers inventory release
- Refund initiated through Payment Service
- Email notification sent

### 4. Status Transition Rules
```
PENDING → PAYMENT_PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                ↓                ↓           ↓            ↓
             CANCELLED      CANCELLED    CANCELLED    RETURN_REQUESTED
                ↓                                           ↓
             REFUNDED                                 RETURN_APPROVED
                                                           ↓
                                                       RETURNED
```

## Scheduled Tasks

### 1. Payment Confirmation Check
- Runs every 5 minutes
- Checks orders in PAYMENT_PENDING > 30 minutes
- Auto-cancels if payment not confirmed
- Releases reserved inventory

### 2. Delivery Date Update
- Runs daily at 2 AM
- Updates expected delivery dates
- Checks with shipping carriers
- Sends delay notifications

### 3. Abandoned Order Cleanup
- Runs weekly
- Archives old cancelled orders
- Cleans up failed order attempts
- Generates abandonment reports

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

1. **Create Order:**
```bash
curl -X POST http://localhost:8085/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cartId": 123,
    "deliveryAddress": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "paymentMethod": "CREDIT_CARD",
    "notes": "Please deliver between 9 AM - 5 PM"
  }'
```

2. **Get Order Status:**
```bash
curl -X GET http://localhost:8085/api/orders/1001 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

3. **Cancel Order:**
```bash
curl -X PUT "http://localhost:8085/api/orders/1001/cancel?reason=Changed%20my%20mind" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Performance Optimizations

### 1. Database
- Composite indexes on (userId, orderStatus)
- Partial index on active orders
- Materialized views for statistics
- Partitioning for large order tables

### 2. Caching
- User's recent orders in Redis
- Order statistics caching
- Frequently accessed orders
- Shipping rate caching

### 3. Async Processing
- Event publishing is async
- Email notifications queued
- Inventory updates batched
- Statistics calculated offline

## Monitoring

### Health Endpoints
- `/actuator/health` - Service health
- `/actuator/health/db` - Database status
- `/actuator/health/redis` - Cache status
- `/actuator/metrics` - Performance metrics

### Key Metrics
- Orders per minute
- Average order value
- Cancellation rate
- Payment failure rate
- Order processing time
- Status transition time

### Alerts
- High cancellation rate
- Payment failures spike
- Order processing delays
- Inventory sync failures

## Troubleshooting

### Common Issues

1. **Order Creation Failed**
   - Check Cart Service availability
   - Verify product stock
   - Validate address format
   - Review payment method

2. **Status Update Failed**
   - Check status transition rules
   - Verify user permissions
   - Review audit logs
   - Check event publishing

3. **Inventory Mismatch**
   - Sync with Product Service
   - Check reservation status
   - Review cancellation events
   - Verify transaction boundaries

## Best Practices

1. **Data Integrity**
   - Use database transactions
   - Implement idempotency
   - Maintain audit trail
   - Version order updates

2. **Reliability**
   - Retry failed operations
   - Handle partial failures
   - Implement compensating transactions
   - Monitor SLA compliance

3. **User Experience**
   - Real-time order tracking
   - Proactive notifications
   - Clear error messages
   - Self-service options

## Future Enhancements

1. **Advanced Features**
   - Split shipments
   - Partial fulfillment
   - Recurring orders
   - Order templates
   - Bulk ordering

2. **Integration**
   - Multiple payment gateways
   - Shipping carrier APIs
   - Tax calculation services
   - ERP integration
   - Warehouse management

3. **Analytics**
   - Order forecasting
   - Demand prediction
   - Customer segmentation
   - Fulfillment optimization
   - Revenue analytics 