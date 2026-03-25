# Config Server

## Overview
The Config Server provides centralized external configuration management for all microservices. It serves configuration from various sources (Git, file system, vault) and allows dynamic configuration updates without service restarts.

## Architecture Position
```
                    ┌──────────────┐
                    │Config Server │
                    │  Port: 8888  │
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐      ┌─────▼─────┐     ┌─────▼─────┐
   │  Auth   │      │  Product  │     │   Cart    │
   │ Service │      │  Service  │     │  Service  │
   └─────────┘      └───────────┘     └───────────┘
```

## Technologies Used

- **Spring Cloud Config Server** - Configuration management
  - Why: Centralized configuration with version control
- **Spring Boot Actuator** - Monitoring endpoints
  - Why: Health checks and refresh capabilities
- **Git/File System** - Configuration storage
  - Why: Version control and easy management

## Key Features

1. **Centralized Configuration** - Single source of truth
2. **Environment Specific** - Dev, staging, prod configs
3. **Dynamic Updates** - Refresh without restart
4. **Encryption/Decryption** - Secure sensitive data
5. **Version Control** - Track configuration changes

## Configuration Structure

```
config-server/src/main/resources/config/
├── application.yml           # Shared configuration
├── auth-service.yml         # Auth Service specific
├── product-service.yml      # Product Service specific
├── cart-service.yml         # Cart Service specific
├── order-service.yml        # Order Service specific
├── payment-service.yml      # Payment Service specific
└── notification-service.yml # Notification Service specific
```

## Server Configuration

### application.yml
```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config
  profiles:
    active: native

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
```

## Shared Configuration Example

### application.yml (shared by all services)
```yaml
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
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}

spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

logging:
  level:
    com.ecommerce: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

## Service-Specific Configuration

### auth-service.yml
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: postgres
  
  redis:
    host: localhost
    port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  jwt:
    secret: mySecretKey
    expiration-ms: 86400000
    refresh-expiration-ms: 604800000
  
  cors:
    allowed-origins: http://localhost:5173
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
```

## Client Configuration

Services connect to Config Server using bootstrap.yml:

```yaml
spring:
  application:
    name: auth-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
```

## Dynamic Configuration Refresh

1. Update configuration in Config Server
2. Call refresh endpoint on the service:
```bash
curl -X POST http://localhost:8081/actuator/refresh
```

## Encryption & Decryption

### Enable encryption:
```yaml
encrypt:
  key: ${ENCRYPT_KEY:defaultKey}
```

### Encrypt sensitive data:
```bash
curl http://localhost:8888/encrypt -d "mypassword"
```

### Use in configuration:
```yaml
spring:
  datasource:
    password: '{cipher}ENCRYPTED_VALUE'
```

## Environment Profiles

### Development
```yaml
spring:
  profiles: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/dev_db
```

### Production
```yaml
spring:
  profiles: prod
  datasource:
    url: jdbc:postgresql://prod-server:5432/prod_db
```

## Git Backend Configuration

For production, use Git repository:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/company/config-repo
          username: ${GIT_USERNAME}
          password: ${GIT_PASSWORD}
          default-label: main
          search-paths: 
            - microservices-config
            - common-config
```

## Monitoring

- Health Check: http://localhost:8888/actuator/health
- Environment: http://localhost:8888/auth-service/default
- Properties: http://localhost:8888/auth-service-default.yml

## Best Practices

1. **Security**
   - Encrypt sensitive properties
   - Use secure transport (HTTPS)
   - Implement access control
   - Audit configuration changes

2. **Organization**
   - Use meaningful property names
   - Group related properties
   - Document configuration
   - Version control configs

3. **Performance**
   - Cache configuration locally
   - Configure appropriate timeouts
   - Use fail-fast for critical configs
   - Monitor config server health

## Troubleshooting

### Service Can't Connect
- Verify Config Server is running
- Check bootstrap.yml configuration
- Review network connectivity
- Check service logs

### Configuration Not Loading
- Verify file naming convention
- Check profile activation
- Review property precedence
- Validate YAML syntax

### Refresh Not Working
- Ensure actuator endpoints exposed
- Check @RefreshScope annotations
- Verify refresh endpoint called
- Review security settings 