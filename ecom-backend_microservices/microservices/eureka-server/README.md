# Eureka Server

## Overview
Eureka Server is the service discovery component in our microservices architecture. It acts as a registry where all microservices register themselves and discover other services dynamically. This enables services to communicate without hardcoded URLs and provides load balancing capabilities.

## Architecture Position
```
                    ┌──────────────┐
                    │ Eureka Server│
                    │  Port: 8761  │
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

- **Spring Cloud Netflix Eureka Server** - Service registry
  - Why: Mature, battle-tested service discovery solution
- **Spring Boot Actuator** - Monitoring and health checks
  - Why: Production-ready features for monitoring

## Key Features

1. **Service Registration** - Services register on startup
2. **Service Discovery** - Services find each other dynamically
3. **Health Monitoring** - Tracks service health status
4. **Load Balancing** - Client-side load balancing support
5. **Self Preservation** - Handles network partitions gracefully

## Configuration

### Application Properties
```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: true
    eviction-interval-timer-in-ms: 60000
    renewal-percent-threshold: 0.85

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## Starting Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

Access the Eureka Dashboard at: http://localhost:8761

## Dashboard Features

- **Registered Services** - List of all registered microservices
- **Instance Status** - UP, DOWN, STARTING, OUT_OF_SERVICE
- **Last Heartbeat** - Time since last health check
- **Availability Zones** - Zone information for multi-region deployments

## Client Configuration

Services register with Eureka using:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

## High Availability Setup

For production, run multiple Eureka instances:

```yaml
# Eureka Server 1
eureka:
  client:
    service-url:
      defaultZone: http://eureka2:8762/eureka/

# Eureka Server 2  
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/
```

## Monitoring

- Health Check: http://localhost:8761/actuator/health
- Metrics: http://localhost:8761/actuator/metrics
- Info: http://localhost:8761/actuator/info

## Best Practices

1. Always run multiple instances in production
2. Configure appropriate timeouts
3. Monitor service registration/deregistration
4. Use health checks effectively
5. Configure self-preservation mode properly 