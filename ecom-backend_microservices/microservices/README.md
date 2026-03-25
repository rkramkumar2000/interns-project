# E-Commerce Microservices

## Overview

This directory contains the microservices architecture implementation of the e-commerce application.

## Microservices

1. **Eureka Server** (Port: 8761) - Service Discovery
2. **Config Server** (Port: 8888) - Centralized Configuration
3. **API Gateway** (Port: 9090) - Single Entry Point
4. **Auth Service** (Port: 8081) - Authentication & Authorization
5. **Product Service** (Port: 8083) - Product Catalog Management
6. **Cart Service** (Port: 8084) - Shopping Cart Management
7. **Order Service** (Port: 8085) - Order Processing
8. **Payment Service** (Port: 8086) - Payment Processing
9. **User Service** (Port: 8082) - User Profile Management
10. **Notification Service** (Port: 8087) - Email/SMS Notifications

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- Git

## Quick Start

### 1. Start Infrastructure Services

```bash
# From ecom-backend directory
docker-compose -f docker-compose-microservices.yml up -d
```

This will start:
- PostgreSQL (with multiple databases)
- Redis
- Kafka & Zookeeper
- Elasticsearch & Kibana
- Zipkin (Distributed Tracing)
- Prometheus & Grafana (Monitoring)

### 2. Start Microservices in Order

#### Step 1: Start Eureka Server
```bash
cd microservices/eureka-server
mvn spring-boot:run
```
Wait for Eureka to start (http://localhost:8761)

#### Step 2: Start Config Server
```bash
cd microservices/config-server
mvn spring-boot:run
```
Verify config server (http://localhost:8888/auth-service/default)

#### Step 3: Start Auth Service
```bash
cd microservices/auth-service
mvn spring-boot:run
```

#### Step 4: Start API Gateway
```bash
cd ../api-gateway
mvn spring-boot:run
```

### 3. Verify Services

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway Health**: http://localhost:9090/actuator/health
- **Kafka UI**: http://localhost:8090
- **Kibana**: http://localhost:5601
- **Zipkin**: http://localhost:9411
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## Service URLs

All services are accessed through the API Gateway:

- Auth Service: http://localhost:9090/auth-service/api/auth/*
- Product Service: http://localhost:9090/product-service/api/products/*
- Cart Service: http://localhost:9090/cart-service/api/carts/*
- Order Service: http://localhost:9090/order-service/api/orders/*

## Development

### Adding a New Microservice

1. Create service directory structure:
```bash
mkdir -p microservices/{service-name}/src/main/java/com/ecommerce/{service}
mkdir -p microservices/{service-name}/src/main/resources
```

2. Create `pom.xml` with standard dependencies
3. Create main application class with `@EnableEurekaClient`
4. Create `bootstrap.yml` for config server connection
5. Add service configuration to config server
6. Update API Gateway routes

### Common Issues & Solutions

#### Service Can't Register with Eureka
- Check if Eureka server is running
- Verify eureka URL in configuration
- Check network connectivity

#### Config Server Connection Failed
- Ensure config server is running before starting services
- Check `bootstrap.yml` configuration
- Verify config file exists in config server

#### Database Connection Issues
- Check if PostgreSQL is running
- Verify database exists (run init script)
- Check database credentials

## Monitoring

### Distributed Tracing with Zipkin
- All requests are automatically traced
- View traces at http://localhost:9411
- Search by service name or trace ID

### Metrics with Prometheus & Grafana
1. Access Grafana at http://localhost:3000
2. Login with admin/admin
3. Import dashboard for Spring Boot metrics
4. Monitor service health, response times, and more

### Logs
- Each service logs to `logs/{service-name}.log`
- Centralized logging can be configured with ELK stack

## Testing

### Unit Tests
```bash
cd microservices/{service-name}
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Load Testing
Use Apache JMeter or K6 for load testing through API Gateway

## Production Deployment

### Building Docker Images
```bash
# Build all services
cd microservices
./build-all.sh
```

### Kubernetes Deployment
```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/
```

## Architecture Decisions

1. **Database per Service**: Each service has its own database for true independence
2. **Event-Driven**: Kafka for asynchronous communication between services
3. **API Gateway**: Single entry point for all client requests
4. **Service Discovery**: Eureka for dynamic service registration
5. **Centralized Configuration**: Spring Cloud Config for environment-specific configs
6. **Circuit Breaker**: Resilience4j for fault tolerance
7. **Distributed Tracing**: Zipkin for request tracking across services

## Next Steps

1. Complete remaining service implementations
2. Add Circuit Breaker patterns
3. Implement Saga pattern for distributed transactions
4. Add API versioning
5. Implement blue-green deployments
6. Add service mesh (Istio) for advanced traffic management 