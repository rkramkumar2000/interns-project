# Payment Service

## Overview
The Payment Service is a critical microservice responsible for processing payments, managing transactions, handling refunds, and integrating with external payment providers like Stripe. It ensures secure payment processing, maintains transaction history, and publishes payment events for order fulfillment and notifications.

## Architecture Position
```
┌─────────────┐     ┌─────────────┐     ┌────────────────┐
│   Frontend  │────▶│ API Gateway │────▶│Payment Service │
└─────────────┘     └─────────────┘     └────────────────┘
                                               │
                                    ┌──────────┴───────────┐
                                    │          │           │
                               ┌────▼───┐ ┌───▼───┐ ┌─────▼──────┐
                               │PostgreSQL│ │Stripe │ │Order Service│
                               └────────┘ └───────┘ └────────────┘
```

## Technologies Used

### Core Framework
- **Spring Boot 3.2.0** - Microservice framework
- **Spring Data JPA** - ORM for PostgreSQL
- **Spring Web** - REST API development

### Payment Integration
- **Stripe Java SDK 24.3.0** - Payment processing
  - Why: Industry-leading payment platform with excellent API
- **Stripe Webhooks** - Real-time payment updates
  - Why: Reliable event-driven payment status updates

### Data Storage
- **PostgreSQL 15** - Primary database
  - Why: ACID compliance critical for financial data
- **Redis 7** - Caching and idempotency
  - Why: Prevent duplicate payments, cache payment methods

### Service Communication
- **Feign Client** - REST client
  - Why: Integration with Order Service
- **Apache Kafka** - Event streaming
  - Why: Publishing payment events
- **Resilience4j** - Circuit breaker
  - Why: Fault tolerance for external services

### Security & Compliance
- **Spring Security** - API security
  - Why: Secure payment endpoints
- **JWT Authentication** - Token validation
  - Why: User authentication
- **PCI DSS Compliance** - Security standards
  - Why: Credit card data protection

### Development Tools
- **Lombok** - Boilerplate reduction
- **ModelMapper** - Object mapping
- **Swagger/OpenAPI 3** - API documentation
- **Jackson** - JSON processing

## Project Structure

```
payment-service/
├── src/main/java/com/ecommerce/payment/
│   ├── PaymentServiceApplication.java        # Main application
│   ├── client/
│   │   ├── OrderServiceClient.java           # Order integration
│   │   └── OrderServiceFallback.java         # Fallback handler
│   ├── config/
│   │   ├── ApplicationConfig.java            # Bean config
│   │   └── StripeConfig.java                 # Stripe setup
│   ├── controller/
│   │   ├── PaymentController.java            # Payment endpoints
│   │   ├── WebhookController.java            # Stripe webhooks
│   │   └── PaymentMethodController.java      # Saved cards
│   ├── dto/                                  # Data Transfer Objects
│   │   ├── PaymentDTO.java                   # Payment details
│   │   ├── CreatePaymentRequest.java         # Payment request
│   │   ├── PaymentResponse.java              # Payment result
│   │   ├── RefundRequest.java                # Refund request
│   │   ├── CardDetailsDTO.java               # Card information
│   │   ├── AddressDTO.java                   # Billing address
│   │   ├── PaymentMethodDTO.java             # Saved method
│   │   └── TransactionDTO.java               # Transaction log
│   ├── event/
│   │   ├── PaymentEvent.java                 # Base event
│   │   ├── PaymentCompletedEvent.java        # Success event
│   │   ├── PaymentFailedEvent.java           # Failure event
│   │   └── RefundProcessedEvent.java         # Refund event
│   ├── exception/
│   │   ├── PaymentNotFoundException.java     # Not found
│   │   ├── PaymentException.java             # Business errors
│   │   ├── StripeException.java              # Stripe errors
│   │   └── GlobalExceptionHandler.java       # Error handler
│   ├── model/
│   │   ├── Payment.java                      # Payment entity
│   │   ├── Transaction.java                  # Transaction log
│   │   ├── PaymentMethod.java                # Saved methods
│   │   └── PaymentStatus.java                # Status enum
│   ├── repository/
│   │   ├── PaymentRepository.java            # Payment access
│   │   ├── TransactionRepository.java        # Transaction log
│   │   └── PaymentMethodRepository.java      # Saved methods
│   ├── security/                             # Security config
│   │   ├── JwtAuthenticationFilter.java      # JWT filter
│   │   ├── JwtAuthenticationToken.java       # Auth token
│   │   ├── JwtAuthenticationEntryPoint.java  # Error handler
│   │   └── SecurityConfig.java               # Security rules
│   └── service/
│       ├── PaymentService.java               # Service interface
│       ├── PaymentServiceImpl.java           # Implementation
│       ├── StripeService.java                # Stripe interface
│       ├── StripeServiceImpl.java            # Stripe impl
│       ├── WebhookService.java               # Webhook handler
│       └── IdempotencyService.java           # Duplicate prevention
├── src/main/resources/
│   └── bootstrap.yml                         # Config server
└── pom.xml                                   # Dependencies
```

## Key Components

### 1. PaymentController
Main payment processing endpoints:

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> processPayment(
        @Valid @RequestBody CreatePaymentRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey)
    
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long paymentId)
    
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentDTO> refundPayment(
        @PathVariable Long paymentId,
        @Valid @RequestBody RefundRequest request)
    
    @PostMapping("/{paymentId}/capture")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDTO> capturePayment(@PathVariable Long paymentId)
    
    @PostMapping("/confirm/{paymentIntentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> confirmPayment(
        @PathVariable String paymentIntentId)
    
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentDTO>> getOrderPayments(@PathVariable Long orderId)
    
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentDTO>> getPaymentHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size)
}
```

### 2. Payment Entity
Comprehensive payment data model:

```java
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    
    @Column(unique = true)
    private String paymentNumber;
    
    private Long orderId;
    private Long userId;
    
    private String paymentMethod;  // CREDIT_CARD, DEBIT_CARD, PAYPAL, etc.
    private String paymentProvider;  // STRIPE, PAYPAL, etc.
    
    @Column(unique = true)
    private String transactionId;  // External transaction ID
    
    private String stripePaymentIntentId;
    private String stripePaymentMethodId;
    private String stripeChargeId;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    
    private String currency = "USD";
    
    @Column(precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON metadata
    
    private String customerEmail;
    private String customerName;
    
    @Column(columnDefinition = "TEXT")
    private String billingAddress;  // JSON address
    
    private String failureCode;
    private String failureMessage;
    
    private Integer attemptCount = 1;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();
    
    // Business methods
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setPayment(this);
    }
    
    public boolean isRefundable() {
        return paymentStatus == PaymentStatus.COMPLETED 
            && refundedAmount.compareTo(amount) < 0;
    }
}
```

### 3. StripeService
Stripe payment integration:

```java
@Service
public class StripeServiceImpl implements StripeService {
    
    @Override
    public PaymentIntent createPaymentIntent(CreatePaymentRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount().multiply(new BigDecimal(100)).longValue())
                .setCurrency(request.getCurrency().toLowerCase())
                .setDescription(request.getDescription())
                .setReceiptEmail(request.getCustomerEmail())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build())
                .putMetadata("orderId", String.valueOf(request.getOrderId()))
                .putMetadata("userId", String.valueOf(request.getUserId()))
                .build();
            
            return PaymentIntent.create(params);
        } catch (StripeException e) {
            throw new PaymentException("Failed to create payment intent", e);
        }
    }
    
    @Override
    public Refund processRefund(String chargeId, BigDecimal amount, String reason) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setCharge(chargeId)
                .setAmount(amount.multiply(new BigDecimal(100)).longValue())
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .putMetadata("reason", reason)
                .build();
            
            return Refund.create(params);
        } catch (StripeException e) {
            throw new PaymentException("Failed to process refund", e);
        }
    }
}
```

## Database Schema

### Tables

#### payments
| Column | Type | Description |
|--------|------|-------------|
| payment_id | BIGINT | Primary key |
| payment_number | VARCHAR(50) | Unique payment number |
| order_id | BIGINT | Associated order |
| user_id | BIGINT | User identifier |
| payment_method | VARCHAR(50) | Payment type |
| payment_provider | VARCHAR(50) | Provider name |
| transaction_id | VARCHAR(100) | External ID |
| stripe_payment_intent_id | VARCHAR(100) | Stripe intent |
| stripe_payment_method_id | VARCHAR(100) | Stripe method |
| stripe_charge_id | VARCHAR(100) | Stripe charge |
| amount | DECIMAL(10,2) | Payment amount |
| currency | VARCHAR(3) | Currency code |
| refunded_amount | DECIMAL(10,2) | Refund total |
| payment_status | VARCHAR(30) | Current status |
| description | TEXT | Payment description |
| metadata | TEXT | JSON metadata |
| customer_email | VARCHAR(100) | Customer email |
| customer_name | VARCHAR(100) | Customer name |
| billing_address | TEXT | JSON address |
| failure_code | VARCHAR(50) | Error code |
| failure_message | TEXT | Error message |
| attempt_count | INTEGER | Retry count |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |
| paid_at | TIMESTAMP | Payment time |
| refunded_at | TIMESTAMP | Refund time |

#### transactions
| Column | Type | Description |
|--------|------|-------------|
| transaction_id | BIGINT | Primary key |
| payment_id | BIGINT | Foreign key |
| transaction_type | VARCHAR(30) | CHARGE, REFUND, etc |
| external_transaction_id | VARCHAR(100) | Provider ID |
| amount | DECIMAL(10,2) | Transaction amount |
| currency | VARCHAR(3) | Currency code |
| status | VARCHAR(30) | Transaction status |
| response_code | VARCHAR(50) | Provider response |
| response_message | TEXT | Response details |
| created_at | TIMESTAMP | Transaction time |

#### payment_methods
| Column | Type | Description |
|--------|------|-------------|
| method_id | BIGINT | Primary key |
| user_id | BIGINT | User identifier |
| stripe_payment_method_id | VARCHAR(100) | Stripe ID |
| card_brand | VARCHAR(50) | Card brand |
| card_last4 | VARCHAR(4) | Last 4 digits |
| exp_month | INTEGER | Expiry month |
| exp_year | INTEGER | Expiry year |
| is_default | BOOLEAN | Default method |
| created_at | TIMESTAMP | Addition time |

## API Endpoints

### Payment Processing

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/payments` | Process payment | Required |
| GET | `/api/payments/{id}` | Get payment details | Required |
| POST | `/api/payments/{id}/refund` | Refund payment | Required |
| POST | `/api/payments/{id}/capture` | Capture payment | ADMIN |
| POST | `/api/payments/confirm/{intentId}` | Confirm 3DS payment | Required |
| GET | `/api/payments/order/{orderId}` | Get order payments | Required |
| GET | `/api/payments/history` | Payment history | Required |

### Payment Methods

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/payment-methods` | List saved methods | Required |
| POST | `/api/payment-methods` | Save new method | Required |
| DELETE | `/api/payment-methods/{id}` | Remove method | Required |
| PUT | `/api/payment-methods/{id}/default` | Set default | Required |

### Webhooks

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/webhooks/stripe` | Stripe webhooks | None* |

*Webhook authentication via signature verification

## Configuration

### Environment Variables
```yaml
# Database
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: payment_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# Stripe
STRIPE_API_KEY: sk_test_xxxxx
STRIPE_PUBLISHABLE_KEY: pk_test_xxxxx
STRIPE_WEBHOOK_SECRET: whsec_xxxxx

# Kafka
KAFKA_BOOTSTRAP_SERVERS: localhost:9092

# Security
JWT_SECRET: mySecretKey
```

### Application Properties
```yaml
server:
  port: 8086

spring:
  application:
    name: payment-service
  
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/payment_db
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

stripe:
  api-key: ${STRIPE_API_KEY}
  publishable-key: ${STRIPE_PUBLISHABLE_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

resilience4j:
  circuitbreaker:
    instances:
      order-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30000
  
  retry:
    instances:
      order-service:
        max-attempts: 3
        wait-duration: 1000

app:
  payment:
    idempotency-ttl-hours: 24
    webhook-tolerance-seconds: 300
```

## Events Published

### PAYMENT_INITIATED
```json
{
  "eventType": "PAYMENT_INITIATED",
  "timestamp": "2024-01-20T10:30:00Z",
  "paymentId": 5001,
  "orderId": 1001,
  "userId": 123,
  "amount": 1299.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD"
}
```

### PAYMENT_COMPLETED
```json
{
  "eventType": "PAYMENT_COMPLETED",
  "timestamp": "2024-01-20T10:31:00Z",
  "paymentId": 5001,
  "orderId": 1001,
  "userId": 123,
  "amount": 1299.99,
  "transactionId": "ch_3MQvxxxxxxxx",
  "paidAt": "2024-01-20T10:30:55Z"
}
```

### PAYMENT_FAILED
```json
{
  "eventType": "PAYMENT_FAILED",
  "timestamp": "2024-01-20T10:31:00Z",
  "paymentId": 5001,
  "orderId": 1001,
  "userId": 123,
  "amount": 1299.99,
  "failureCode": "card_declined",
  "failureMessage": "Your card was declined"
}
```

### REFUND_PROCESSED
```json
{
  "eventType": "REFUND_PROCESSED",
  "timestamp": "2024-01-20T12:00:00Z",
  "paymentId": 5001,
  "refundId": "re_3MQvxxxxxxxx",
  "amount": 1299.99,
  "reason": "Customer requested refund"
}
```

## Security Features

### 1. PCI Compliance
- No card details stored locally
- All sensitive data handled by Stripe
- TLS encryption for all API calls
- Tokenization for card data

### 2. Webhook Security
```java
@PostMapping("/api/webhooks/stripe")
public ResponseEntity<String> handleStripeWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signature) {
    
    try {
        Event event = Webhook.constructEvent(
            payload, signature, webhookSecret
        );
        
        // Process webhook
        webhookService.processWebhook(event);
        
        return ResponseEntity.ok("Webhook processed");
    } catch (SignatureVerificationException e) {
        return ResponseEntity.status(400).body("Invalid signature");
    }
}
```

### 3. Idempotency
Preventing duplicate payments:
```java
@Service
public class IdempotencyService {
    
    public PaymentResponse processIdempotent(
        String idempotencyKey, 
        Supplier<PaymentResponse> paymentProcessor) {
        
        String cacheKey = "idempotency:" + idempotencyKey;
        
        // Check if already processed
        PaymentResponse cached = redisTemplate.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Process payment
        PaymentResponse response = paymentProcessor.get();
        
        // Cache result
        redisTemplate.setex(cacheKey, 24 * 3600, response);
        
        return response;
    }
}
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -Pintegration-tests
```

### Manual Testing

1. **Process Payment:**
```bash
curl -X POST http://localhost:8086/api/payments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "orderId": 1001,
    "amount": 299.99,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "customerEmail": "user@example.com",
    "billingAddress": {
      "line1": "123 Main St",
      "city": "New York",
      "state": "NY",
      "postalCode": "10001",
      "country": "US"
    }
  }'
```

2. **Process Refund:**
```bash
curl -X POST http://localhost:8086/api/payments/5001/refund \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.00,
    "reason": "Partial refund for damaged item"
  }'
```

### Test Cards (Stripe)
- Success: `4242 4242 4242 4242`
- Decline: `4000 0000 0000 0002`
- 3D Secure: `4000 0000 0000 3220`

## Performance Optimizations

### 1. Database
- Indexes on orderId, userId, status
- Partitioning by created_at
- Archive old transactions
- Connection pooling

### 2. Caching
- Payment method caching
- Idempotency caching
- Webhook deduplication
- Response caching

### 3. Async Processing
- Webhook processing queued
- Event publishing async
- Notification triggers async
- Batch refund processing

## Monitoring

### Health Endpoints
- `/actuator/health` - Service health
- `/actuator/health/stripe` - Stripe connectivity
- `/actuator/metrics` - Performance metrics

### Key Metrics
- Payment success rate
- Average payment time
- Refund processing time
- Webhook processing lag
- Failed payment reasons

### Alerts
- High failure rate (> 5%)
- Stripe API errors
- Webhook signature failures
- Database connection issues
- Unusual refund patterns

## Troubleshooting

### Common Issues

1. **Payment Failed**
   - Check Stripe dashboard
   - Verify card details
   - Review decline codes
   - Check rate limits

2. **Webhook Not Received**
   - Verify webhook URL
   - Check signature
   - Review Stripe logs
   - Test with Stripe CLI

3. **Duplicate Payments**
   - Check idempotency
   - Review cache TTL
   - Verify request IDs
   - Check retry logic

## Best Practices

1. **Security**
   - Always use HTTPS
   - Validate webhook signatures
   - Implement rate limiting
   - Log security events

2. **Reliability**
   - Use idempotency keys
   - Implement retries
   - Handle partial failures
   - Monitor success rates

3. **Compliance**
   - Follow PCI DSS
   - Implement audit logs
   - Secure sensitive data
   - Regular security scans

## Future Enhancements

1. **Payment Methods**
   - Apple Pay / Google Pay
   - Cryptocurrency
   - Buy now, pay later
   - Digital wallets
   - Bank transfers

2. **Features**
   - Subscription billing
   - Payment plans
   - Multi-currency
   - Dynamic pricing
   - Fraud detection

3. **Integration**
   - Multiple providers
   - Payment orchestration
   - Tax calculation
   - Currency conversion
   - Accounting systems 