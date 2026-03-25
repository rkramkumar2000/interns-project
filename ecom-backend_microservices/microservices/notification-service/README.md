# Notification Service

## Overview
The Notification Service is an event-driven microservice responsible for sending multi-channel notifications (Email, SMS, Push) based on business events from other services. It consumes Kafka events, processes notification templates, manages delivery status, and provides retry mechanisms for failed notifications.

## Architecture Position
```
┌──────────────┐   Events    ┌─────────────────────┐
│Other Services│─────────────▶│                     │
└──────────────┘              │  Notification       │
                              │     Service         │
┌──────────────┐   Kafka     │                     │     ┌─────────┐
│   Kafka      │◀─────────────│                     │────▶│  Email  │
└──────────────┘              │                     │     ├─────────┤
                              │                     │────▶│   SMS   │
                              │                     │     ├─────────┤
                              └─────────────────────┘────▶│  Push   │
                                                          └─────────┘
```

## Technologies Used

### Core Framework
- **Spring Boot 3.2.0** - Microservice framework
- **Spring Kafka** - Event streaming consumer
- **Spring Data JPA** - Database operations

### Notification Channels
- **Spring Mail** - Email sending
  - Why: Native Spring integration for SMTP
- **Twilio SDK** - SMS messaging
  - Why: Reliable SMS delivery worldwide
- **Thymeleaf** - Email templating
  - Why: Dynamic HTML email generation

### Data Storage
- **PostgreSQL 15** - Notification history
  - Why: Audit trail and retry management
- **Redis 7** - Template caching
  - Why: Fast template access, rate limiting

### Service Integration
- **Apache Kafka** - Event consumption
  - Why: Decoupled event-driven architecture
- **Eureka Client** - Service discovery
  - Why: Dynamic service registration
- **Spring Cloud Config** - Configuration
  - Why: Centralized config management

### Additional Features
- **Spring Scheduler** - Retry mechanism
  - Why: Scheduled notification retries
- **Spring Async** - Asynchronous processing
  - Why: Non-blocking notification sending

## Project Structure

```
notification-service/
├── src/main/java/com/ecommerce/notification/
│   ├── NotificationServiceApplication.java    # Main application
│   ├── config/
│   │   ├── ApplicationConfig.java            # Bean config
│   │   ├── KafkaConfig.java                  # Kafka consumer
│   │   ├── MailConfig.java                   # Email settings
│   │   └── TwilioConfig.java                 # SMS settings
│   ├── controller/
│   │   ├── NotificationController.java       # REST endpoints
│   │   └── TemplateController.java           # Template mgmt
│   ├── dto/                                  # Data Transfer Objects
│   │   ├── NotificationRequest.java          # Send request
│   │   ├── NotificationResponse.java         # Send response
│   │   ├── EmailRequest.java                 # Email details
│   │   ├── SmsRequest.java                   # SMS details
│   │   ├── PushRequest.java                  # Push details
│   │   ├── TemplateDTO.java                  # Template data
│   │   └── NotificationStatusDTO.java        # Status info
│   ├── event/                                # Event listeners
│   │   ├── NotificationEventListener.java    # Kafka consumer
│   │   ├── AuthEventHandler.java             # Auth events
│   │   ├── OrderEventHandler.java            # Order events
│   │   ├── PaymentEventHandler.java          # Payment events
│   │   └── CartEventHandler.java             # Cart events
│   ├── exception/
│   │   ├── NotificationException.java        # Business errors
│   │   ├── TemplateException.java            # Template errors
│   │   └── GlobalExceptionHandler.java       # Error handler
│   ├── model/
│   │   ├── Notification.java                 # Notification entity
│   │   ├── NotificationTemplate.java         # Template entity
│   │   ├── NotificationType.java             # Type enum
│   │   └── NotificationStatus.java           # Status enum
│   ├── repository/
│   │   ├── NotificationRepository.java       # Notification access
│   │   └── TemplateRepository.java           # Template access
│   ├── scheduler/
│   │   ├── RetryScheduler.java               # Retry failed
│   │   └── CleanupScheduler.java             # Archive old
│   └── service/
│       ├── NotificationService.java          # Main interface
│       ├── NotificationServiceImpl.java      # Implementation
│       ├── EmailService.java                 # Email interface
│       ├── EmailServiceImpl.java             # Email impl
│       ├── SmsService.java                   # SMS interface
│       ├── SmsServiceImpl.java               # SMS impl
│       ├── TemplateService.java              # Template interface
│       └── TemplateServiceImpl.java          # Template impl
├── src/main/resources/
│   ├── bootstrap.yml                         # Config server
│   ├── application.yml                       # Local config
│   └── templates/                            # Email templates
│       ├── welcome.html                      # Welcome email
│       ├── order-confirmation.html           # Order confirm
│       ├── payment-success.html              # Payment confirm
│       ├── shipping-update.html              # Shipping status
│       └── password-reset.html               # Password reset
└── pom.xml                                   # Dependencies
```

## Key Components

### 1. NotificationEventListener
Kafka event consumer for all business events:

```java
@Component
@Slf4j
public class NotificationEventListener {
    
    @KafkaListener(topics = "auth-events", groupId = "notification-service")
    public void handleAuthEvent(AuthEvent event) {
        switch (event.getEventType()) {
            case "USER_REGISTERED":
                sendWelcomeEmail(event);
                break;
            case "PASSWORD_RESET_REQUESTED":
                sendPasswordResetEmail(event);
                break;
            case "USER_VERIFIED":
                sendVerificationSuccessEmail(event);
                break;
        }
    }
    
    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case "ORDER_CREATED":
                sendOrderConfirmation(event);
                break;
            case "ORDER_SHIPPED":
                sendShippingNotification(event);
                break;
            case "ORDER_DELIVERED":
                sendDeliveryConfirmation(event);
                break;
            case "ORDER_CANCELLED":
                sendCancellationNotification(event);
                break;
        }
    }
    
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentEvent(PaymentEvent event) {
        switch (event.getEventType()) {
            case "PAYMENT_COMPLETED":
                sendPaymentReceipt(event);
                break;
            case "PAYMENT_FAILED":
                sendPaymentFailureAlert(event);
                break;
            case "REFUND_PROCESSED":
                sendRefundNotification(event);
                break;
        }
    }
}
```

### 2. NotificationService
Core service handling notification logic:

```java
@Service
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {
    
    @Override
    @Async
    public CompletableFuture<NotificationResponse> sendNotification(
            NotificationRequest request) {
        
        // Create notification record
        Notification notification = createNotification(request);
        
        try {
            // Process based on type
            switch (request.getType()) {
                case EMAIL:
                    sendEmail(notification, request);
                    break;
                case SMS:
                    sendSms(notification, request);
                    break;
                case PUSH:
                    sendPush(notification, request);
                    break;
                case IN_APP:
                    createInAppNotification(notification, request);
                    break;
            }
            
            // Update status
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            
        } catch (Exception e) {
            handleFailure(notification, e);
        }
        
        notificationRepository.save(notification);
        
        return CompletableFuture.completedFuture(
            mapToResponse(notification)
        );
    }
    
    @Override
    public void scheduleNotification(NotificationRequest request, 
                                   LocalDateTime scheduledTime) {
        Notification notification = createNotification(request);
        notification.setStatus(NotificationStatus.SCHEDULED);
        notification.setScheduledAt(scheduledTime);
        notificationRepository.save(notification);
    }
}
```

### 3. EmailService
Email sending implementation:

```java
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    
    @Override
    @Async
    public void sendEmail(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, true, "UTF-8"
            );
            
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setFrom(fromEmail);
            
            // Process template if provided
            if (request.getTemplateName() != null) {
                String html = templateService.processTemplate(
                    request.getTemplateName(),
                    request.getTemplateVariables()
                );
                helper.setText(html, true);
            } else {
                helper.setText(request.getBody(), request.isHtml());
            }
            
            // Add attachments if any
            if (request.getAttachments() != null) {
                for (EmailAttachment attachment : request.getAttachments()) {
                    helper.addAttachment(
                        attachment.getName(),
                        attachment.getDataSource()
                    );
                }
            }
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", request.getTo());
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.getTo(), e);
            throw new NotificationException("Email sending failed", e);
        }
    }
}
```

## Database Schema

### Tables

#### notifications
| Column | Type | Description |
|--------|------|-------------|
| notification_id | BIGINT | Primary key |
| user_id | BIGINT | Recipient user |
| recipient | VARCHAR(255) | Email/Phone |
| type | VARCHAR(20) | EMAIL, SMS, PUSH |
| subject | VARCHAR(255) | Notification subject |
| content | TEXT | Message content |
| template_name | VARCHAR(100) | Template used |
| template_variables | TEXT | JSON variables |
| status | VARCHAR(30) | Current status |
| priority | VARCHAR(20) | HIGH, MEDIUM, LOW |
| retry_count | INTEGER | Retry attempts |
| scheduled_at | TIMESTAMP | Scheduled time |
| sent_at | TIMESTAMP | Actual send time |
| delivered_at | TIMESTAMP | Delivery time |
| failed_reason | TEXT | Failure details |
| metadata | TEXT | JSON metadata |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

#### notification_templates
| Column | Type | Description |
|--------|------|-------------|
| template_id | BIGINT | Primary key |
| name | VARCHAR(100) | Template name |
| type | VARCHAR(20) | EMAIL, SMS |
| subject | VARCHAR(255) | Email subject |
| content | TEXT | Template content |
| variables | TEXT | Required variables |
| active | BOOLEAN | Is active |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

## Events Consumed

### Auth Events
- `USER_REGISTERED` - Send welcome email
- `PASSWORD_RESET_REQUESTED` - Send reset link
- `EMAIL_VERIFIED` - Send confirmation
- `USER_DEACTIVATED` - Send goodbye email

### Order Events
- `ORDER_CREATED` - Order confirmation
- `ORDER_CONFIRMED` - Payment confirmed
- `ORDER_SHIPPED` - Shipping notification
- `ORDER_DELIVERED` - Delivery confirmation
- `ORDER_CANCELLED` - Cancellation notice
- `RETURN_APPROVED` - Return instructions

### Payment Events
- `PAYMENT_COMPLETED` - Payment receipt
- `PAYMENT_FAILED` - Failure notification
- `REFUND_PROCESSED` - Refund confirmation

### Cart Events
- `CART_ABANDONED` - Abandonment reminder
- `PRICE_DROP` - Price alert
- `BACK_IN_STOCK` - Stock notification

## Email Templates

### Welcome Email
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Welcome to E-Commerce</title>
</head>
<body>
    <h1>Welcome <span th:text="${firstName}">User</span>!</h1>
    <p>Thank you for joining our e-commerce platform.</p>
    <p>Your account has been created with email: 
       <strong th:text="${email}">email@example.com</strong>
    </p>
    <a th:href="${verificationLink}" 
       style="background-color: #4CAF50; color: white; 
              padding: 10px 20px; text-decoration: none;">
        Verify Your Email
    </a>
</body>
</html>
```

### Order Confirmation
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Order Confirmation</title>
</head>
<body>
    <h1>Order Confirmed!</h1>
    <p>Order Number: <strong th:text="${orderNumber}">ORD-001</strong></p>
    <p>Total Amount: $<span th:text="${totalAmount}">0.00</span></p>
    
    <h2>Order Items:</h2>
    <table>
        <tr th:each="item : ${orderItems}">
            <td th:text="${item.productName}">Product</td>
            <td th:text="${item.quantity}">1</td>
            <td>$<span th:text="${item.price}">0.00</span></td>
        </tr>
    </table>
    
    <p>Expected Delivery: <span th:text="${deliveryDate}">Date</span></p>
</body>
</html>
```

## Configuration

### Environment Variables
```yaml
# Database
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: notification_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# Email (SMTP)
MAIL_HOST: smtp.gmail.com
MAIL_PORT: 587
MAIL_USERNAME: your-email@gmail.com
MAIL_PASSWORD: your-app-password
MAIL_FROM: noreply@ecommerce.com

# SMS (Twilio)
TWILIO_ACCOUNT_SID: ACxxxxx
TWILIO_AUTH_TOKEN: xxxxx
TWILIO_FROM_NUMBER: +1234567890

# Kafka
KAFKA_BOOTSTRAP_SERVERS: localhost:9092
```

### Application Properties
```yaml
server:
  port: 8087

spring:
  application:
    name: notification-service
  
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/notification_db
  
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

twilio:
  account-sid: ${TWILIO_ACCOUNT_SID}
  auth-token: ${TWILIO_AUTH_TOKEN}
  from-number: ${TWILIO_FROM_NUMBER}

app:
  notification:
    retry:
      max-attempts: 3
      delay-seconds: 300
    cleanup:
      days-to-keep: 90
    rate-limit:
      sms-per-day: 10
      email-per-hour: 50
```

## API Endpoints

### Notification Management

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/notifications/send` | Send notification | Required |
| POST | `/api/notifications/schedule` | Schedule notification | Required |
| GET | `/api/notifications/{id}` | Get notification | Required |
| GET | `/api/notifications/user/{userId}` | User notifications | Required |
| PUT | `/api/notifications/{id}/read` | Mark as read | Required |
| DELETE | `/api/notifications/{id}` | Delete notification | Required |

### Template Management

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/templates` | List templates | ADMIN |
| GET | `/api/templates/{name}` | Get template | ADMIN |
| POST | `/api/templates` | Create template | ADMIN |
| PUT | `/api/templates/{id}` | Update template | ADMIN |
| DELETE | `/api/templates/{id}` | Delete template | ADMIN |
| POST | `/api/templates/preview` | Preview template | ADMIN |

### Statistics

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/notifications/stats` | Overall statistics | ADMIN |
| GET | `/api/notifications/stats/daily` | Daily statistics | ADMIN |
| GET | `/api/notifications/stats/channel` | Channel statistics | ADMIN |

## Notification Types

### EMAIL
- Welcome emails
- Order confirmations
- Shipping updates
- Payment receipts
- Password resets
- Promotional campaigns

### SMS
- Order updates
- Delivery alerts
- OTP codes
- Payment confirmations
- Urgent notifications

### PUSH (Future)
- Real-time alerts
- Order status changes
- Price drops
- Back in stock

### IN_APP
- System messages
- Promotional offers
- Account updates

## Retry Mechanism

### Retry Strategy
```java
@Component
@Slf4j
public class RetryScheduler {
    
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void retryFailedNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        List<Notification> failed = notificationRepository
            .findByStatusAndCreatedAtAfterAndRetryCountLessThan(
                NotificationStatus.FAILED,
                cutoff,
                maxRetryAttempts
            );
        
        for (Notification notification : failed) {
            try {
                retryNotification(notification);
                notification.setRetryCount(notification.getRetryCount() + 1);
                
                if (isSuccessful(notification)) {
                    notification.setStatus(NotificationStatus.SENT);
                }
            } catch (Exception e) {
                if (notification.getRetryCount() >= maxRetryAttempts - 1) {
                    notification.setStatus(NotificationStatus.FAILED_PERMANENT);
                }
            }
            
            notificationRepository.save(notification);
        }
    }
}
```

## Rate Limiting

### Implementation
```java
@Service
public class RateLimitService {
    
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    
    public boolean checkRateLimit(Long userId, NotificationType type) {
        String key = String.format("rate_limit:%s:%d:%s", 
            type, userId, LocalDate.now());
        
        Integer count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            count = 0;
        }
        
        int limit = getRateLimit(type);
        if (count >= limit) {
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
        
        return true;
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

1. **Send Email:**
```bash
curl -X POST http://localhost:8087/api/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "type": "EMAIL",
    "recipient": "user@example.com",
    "subject": "Test Email",
    "templateName": "welcome",
    "templateVariables": {
      "firstName": "John",
      "email": "user@example.com"
    }
  }'
```

2. **Send SMS:**
```bash
curl -X POST http://localhost:8087/api/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "type": "SMS",
    "recipient": "+1234567890",
    "content": "Your order #1001 has been shipped!"
  }'
```

## Performance Optimizations

### 1. Async Processing
- All notifications sent asynchronously
- Event processing in parallel
- Batch processing for bulk notifications

### 2. Template Caching
- Templates cached in Redis
- Cache invalidation on update
- Compiled template caching

### 3. Connection Pooling
- SMTP connection pooling
- HTTP connection pooling for webhooks
- Database connection pooling

## Monitoring

### Health Endpoints
- `/actuator/health` - Service health
- `/actuator/health/mail` - Email service
- `/actuator/health/kafka` - Kafka connectivity

### Metrics
- Notifications sent per channel
- Success/failure rates
- Average sending time
- Queue depth
- Template usage

### Alerts
- High failure rate
- Email service down
- SMS quota exceeded
- Kafka lag increasing

## Troubleshooting

### Common Issues

1. **Email Not Sending**
   - Check SMTP credentials
   - Verify firewall rules
   - Review spam filters
   - Check email quotas

2. **SMS Failures**
   - Verify Twilio credentials
   - Check phone number format
   - Review balance/quotas
   - Check regional restrictions

3. **Template Errors**
   - Validate template syntax
   - Check variable names
   - Review template cache
   - Test with preview

## Best Practices

1. **Content**
   - Clear subject lines
   - Responsive email design
   - Concise SMS messages
   - Personalization

2. **Delivery**
   - Respect user preferences
   - Time zone awareness
   - Rate limiting
   - Unsubscribe options

3. **Security**
   - Validate recipients
   - Sanitize template data
   - Secure credentials
   - Audit logging

## Future Enhancements

1. **Channels**
   - Push notifications (FCM/APNS)
   - WhatsApp integration
   - Slack notifications
   - Voice calls

2. **Features**
   - A/B testing
   - Analytics dashboard
   - User preferences
   - Notification center
   - Rich media support

3. **Intelligence**
   - Smart delivery timing
   - Channel optimization
   - Engagement tracking
   - Predictive analytics 