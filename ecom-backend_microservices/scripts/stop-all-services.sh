#!/bin/bash

# E-Commerce Microservices Shutdown Script
# This script stops all running microservices

echo "🛑 Stopping E-Commerce Microservices Platform..."
echo "================================================"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Base directory
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Function to stop a service
stop_service() {
    local service_name=$1
    local pid_file="$BASE_DIR/logs/${service_name}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}🛑 Stopping $service_name (PID: $pid)...${NC}"
            kill $pid
            sleep 2
            
            # Force kill if still running
            if ps -p $pid > /dev/null 2>&1; then
                echo -e "${YELLOW}⚠️  Force stopping $service_name...${NC}"
                kill -9 $pid
            fi
            
            echo -e "${GREEN}✅ $service_name stopped${NC}"
        else
            echo -e "${YELLOW}ℹ️  $service_name is not running${NC}"
        fi
        rm -f "$pid_file"
    else
        echo -e "${YELLOW}ℹ️  No PID file found for $service_name${NC}"
    fi
}

# Stop all services in reverse order
echo "📋 Stopping services in reverse dependency order..."
echo ""

# Business services
stop_service "Notification Service"
stop_service "Payment Service"
stop_service "Order Service"
stop_service "Cart Service"
stop_service "Product Service"
stop_service "Auth Service"

# Core services
stop_service "API Gateway"
stop_service "Eureka Server"
stop_service "Config Server"

echo ""

# Alternative method: Kill all Spring Boot processes
echo "🔍 Checking for any remaining Spring Boot processes..."
remaining=$(pgrep -f "spring-boot:run" | wc -l)

if [ "$remaining" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Found $remaining Spring Boot process(es) still running${NC}"
    echo "Stopping all Spring Boot processes..."
    pkill -f "spring-boot:run"
    sleep 2
    
    # Force kill if necessary
    if pgrep -f "spring-boot:run" > /dev/null; then
        echo -e "${YELLOW}⚠️  Force stopping remaining processes...${NC}"
        pkill -9 -f "spring-boot:run"
    fi
    echo -e "${GREEN}✅ All Spring Boot processes stopped${NC}"
else
    echo -e "${GREEN}✅ No remaining Spring Boot processes found${NC}"
fi

echo ""
echo "================================================"
echo -e "${GREEN}🎉 All services have been stopped!${NC}"
echo ""
echo "📝 Service logs are preserved in: $BASE_DIR/logs/"
echo ""
echo "To stop infrastructure services, run:"
echo "  docker-compose -f docker-compose-microservices.yml down"
echo ""
echo "To clean up everything (including data), run:"
echo "  docker-compose -f docker-compose-microservices.yml down -v"
echo "================================================" 