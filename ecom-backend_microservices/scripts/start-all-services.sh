#!/bin/bash

# E-Commerce Microservices Startup Script
# This script starts all microservices in the correct order

echo "🚀 Starting E-Commerce Microservices Platform..."
echo "================================================"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Base directory
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Function to check if service is running
check_service() {
    local port=$1
    local service_name=$2
    local max_attempts=30
    local attempt=0
    
    echo -e "${YELLOW}⏳ Waiting for $service_name to start on port $port...${NC}"
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is running!${NC}"
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}❌ $service_name failed to start on port $port${NC}"
    return 1
}

# Function to start a service
start_service() {
    local service_dir=$1
    local service_name=$2
    local port=$3
    
    echo -e "${YELLOW}🔄 Starting $service_name...${NC}"
    cd "$BASE_DIR/$service_dir"
    
    # Start service in background
    nohup mvn spring-boot:run > "$BASE_DIR/logs/${service_name}.log" 2>&1 &
    local pid=$!
    echo $pid > "$BASE_DIR/logs/${service_name}.pid"
    
    # Check if service started successfully
    if check_service $port "$service_name"; then
        echo -e "${GREEN}✅ $service_name started successfully (PID: $pid)${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}❌ Failed to start $service_name${NC}"
        return 1
    fi
}

# Create logs directory if it doesn't exist
mkdir -p logs

# Check if infrastructure is running
echo "🔍 Checking infrastructure services..."
missing_services=()

# Check required services
if ! docker ps | grep -q "ecom-postgres"; then
    missing_services+=("PostgreSQL")
fi
if ! docker ps | grep -q "ecom-redis"; then
    missing_services+=("Redis")
fi
if ! docker ps | grep -q "ecom-kafka"; then
    missing_services+=("Kafka")
fi
if ! docker ps | grep -q "ecom-elasticsearch"; then
    missing_services+=("Elasticsearch")
fi

if [ ${#missing_services[@]} -ne 0 ]; then
    echo -e "${RED}❌ The following infrastructure services are not running:${NC}"
    printf '%s\n' "${missing_services[@]}"
    echo ""
    echo "Please start infrastructure first:"
    echo "   docker-compose -f docker-compose-microservices.yml up -d"
    exit 1
fi

echo -e "${GREEN}✅ All infrastructure services are running${NC}"
echo ""

# Start services in order
echo "🏁 Starting microservices in dependency order..."
echo "================================================"

# 1. Config Server
if ! start_service "microservices/config-server" "Config Server" 8888; then
    echo "Cannot proceed without Config Server"
    exit 1
fi

# 2. Eureka Server
if ! start_service "microservices/eureka-server" "Eureka Server" 8761; then
    echo "Cannot proceed without Eureka Server"
    exit 1
fi

# 3. API Gateway
if ! start_service "api-gateway" "API Gateway" 8080; then
    echo "Cannot proceed without API Gateway"
    exit 1
fi

# 4. Business Services (can start in parallel after core services are up)
echo "🚀 Starting business services..."
echo "================================"

# Start all business services
start_service "microservices/auth-service" "Auth Service" 8081 &
start_service "microservices/product-service" "Product Service" 8083 &
start_service "microservices/cart-service" "Cart Service" 8084 &
start_service "microservices/order-service" "Order Service" 8085 &
start_service "microservices/payment-service" "Payment Service" 8086 &
start_service "microservices/notification-service" "Notification Service" 8087 &

# Wait for all background jobs to complete
wait

echo ""
echo "================================================"
echo -e "${GREEN}🎉 All services have been started!${NC}"
echo ""
echo "📋 Service Status:"
echo "  - Config Server:        http://localhost:8888"
echo "  - Eureka Dashboard:     http://localhost:8761"
echo "  - API Gateway:          http://localhost:8080"
echo "  - Auth Service:         http://localhost:8081"
echo "  - Product Service:      http://localhost:8083"
echo "  - Cart Service:         http://localhost:8084"
echo "  - Order Service:        http://localhost:8085"
echo "  - Payment Service:      http://localhost:8086"
echo "  - Notification Service: http://localhost:8087"
echo ""
echo "📊 Monitoring & Documentation:"
echo "  - Swagger UI:          http://localhost:{service-port}/swagger-ui.html"
echo "  - Kibana:              http://localhost:5601"
echo "  - Prometheus:          http://localhost:9090"
echo "  - Grafana:             http://localhost:3000"
echo "  - Zipkin:              http://localhost:9411"
echo "  - Kafka UI:            http://localhost:9000"
echo ""
echo "📝 Logs are available in: $BASE_DIR/logs/"
echo ""
echo "To stop all services, run: ./scripts/stop-all-services.sh"
echo "================================================" 