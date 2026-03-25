#!/bin/bash

# Docker Startup Script for E-Commerce Microservices
# This script starts all services using Docker Compose

echo "🐳 Starting E-Commerce Microservices with Docker..."
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Base directory
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Check if .env file exists for Stripe key
if [ ! -f "$BASE_DIR/.env" ]; then
    echo -e "${YELLOW}⚠️  Warning: .env file not found${NC}"
    echo "Creating .env file with placeholder..."
    echo "STRIPE_SECRET_KEY=your_stripe_secret_key_here" > "$BASE_DIR/.env"
    echo -e "${YELLOW}Please update the STRIPE_SECRET_KEY in .env file${NC}"
    echo ""
fi

# Function to check if service is healthy
check_service_health() {
    local service=$1
    local container_name=$2
    
    # Check if container is running
    if docker ps --format "table {{.Names}}" | grep -q "^${container_name}$"; then
        # Check health status
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")
        
        if [ "$health_status" == "healthy" ]; then
            echo -e "${GREEN}✅ $service is healthy${NC}"
            return 0
        elif [ "$health_status" == "none" ]; then
            # No health check defined, just check if running
            if docker ps | grep -q "$container_name"; then
                echo -e "${GREEN}✅ $service is running${NC}"
                return 0
            fi
        else
            echo -e "${YELLOW}⏳ $service health status: $health_status${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ $service is not running${NC}"
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local service=$1
    local container_name=$2
    local max_attempts=60
    local attempt=0
    
    echo -e "${YELLOW}⏳ Waiting for $service to be ready...${NC}"
    
    while [ $attempt -lt $max_attempts ]; do
        if check_service_health "$service" "$container_name"; then
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}❌ $service failed to become healthy after $max_attempts attempts${NC}"
    return 1
}

# Start all services
echo "🚀 Starting all services with Docker Compose..."
docker-compose -f docker-compose-microservices.yml up -d --build

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to start services with Docker Compose${NC}"
    exit 1
fi

echo ""
echo "⏳ Waiting for services to initialize..."
echo ""

# Wait for infrastructure services first
echo "📦 Checking infrastructure services..."
wait_for_service "PostgreSQL" "ecom-postgres"
wait_for_service "Redis" "ecom-redis"
wait_for_service "Kafka" "ecom-kafka"
wait_for_service "Elasticsearch" "elasticsearch"

echo ""
echo "🔧 Checking microservices infrastructure..."
wait_for_service "Config Server" "ecom-config-server"
wait_for_service "Eureka Server" "ecom-eureka-server"

echo ""
echo "📡 Checking API Gateway..."
wait_for_service "API Gateway" "ecom-api-gateway"

echo ""
echo "🎯 Checking business services..."
wait_for_service "Auth Service" "ecom-auth-service"
wait_for_service "Product Service" "ecom-product-service"
wait_for_service "Cart Service" "ecom-cart-service"
wait_for_service "Order Service" "ecom-order-service"
wait_for_service "Payment Service" "ecom-payment-service"
wait_for_service "Notification Service" "ecom-notification-service"

echo ""
echo "🌐 Checking frontend..."
wait_for_service "Frontend" "ecom-frontend"

echo ""
echo "📊 Checking monitoring services..."
wait_for_service "Prometheus" "ecom-prometheus"
wait_for_service "Grafana" "ecom-grafana"
wait_for_service "Zipkin" "ecom-zipkin"

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}✅ All services are up and running!${NC}"
echo "=========================================="
echo ""
echo "🌐 Access Points:"
echo "   - Frontend: http://localhost:3000"
echo "   - API Gateway: http://localhost:8080"
echo "   - Eureka Dashboard: http://localhost:8761"
echo "   - Grafana: http://localhost:3001 (admin/admin)"
echo "   - Zipkin: http://localhost:9411"
echo "   - Kibana: http://localhost:5601"
echo ""
echo "📚 API Documentation:"
echo "   - Gateway Swagger: http://localhost:8080/swagger-ui.html"
echo "   - Individual service docs available through gateway"
echo ""
echo "🛑 To stop all services:"
echo "   ./scripts/docker-stop.sh"
echo "" 