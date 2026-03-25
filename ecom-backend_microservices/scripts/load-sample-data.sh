#!/bin/bash

# Load Sample Data for All Microservices
# This script loads sample data into all microservice databases
# Prerequisites: Docker containers must be running with databases created

echo "==================================="
echo "Loading Sample Data for Microservices"
echo "==================================="

# Configuration
POSTGRES_CONTAINER="ecom-backend-postgres-1"  # Adjust if your container name is different
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="postgres"

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to load SQL file into a specific database
load_sql_file() {
    local db_name=$1
    local sql_file=$2
    local service_name=$3
    
    echo -e "${YELLOW}Loading data for ${service_name}...${NC}"
    
    # Check if SQL file exists
    if [ ! -f "$sql_file" ]; then
        echo -e "${RED}Error: SQL file not found: $sql_file${NC}"
        return 1
    fi
    
    # Load the SQL file
    docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $db_name < "$sql_file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Successfully loaded data for ${service_name}${NC}"
    else
        echo -e "${RED}✗ Failed to load data for ${service_name}${NC}"
        return 1
    fi
}

# Check if postgres container is running
if ! docker ps | grep -q $POSTGRES_CONTAINER; then
    echo -e "${RED}Error: PostgreSQL container '$POSTGRES_CONTAINER' is not running${NC}"
    echo "Please run: docker-compose -f docker-compose-microservices.yml up -d"
    exit 1
fi

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
SAMPLE_DATA_DIR="$SCRIPT_DIR/microservices-sample-data"

echo -e "\n${YELLOW}Loading sample data from: $SAMPLE_DATA_DIR${NC}\n"

# Load data for each service
echo "1. Auth Service"
load_sql_file "auth_db" "$SAMPLE_DATA_DIR/auth-service-data.sql" "Auth Service"

echo -e "\n2. Product Service"
load_sql_file "product_db" "$SAMPLE_DATA_DIR/product-service-data.sql" "Product Service"

echo -e "\n3. Cart Service"
load_sql_file "cart_db" "$SAMPLE_DATA_DIR/cart-service-data.sql" "Cart Service"

echo -e "\n4. Order Service"
load_sql_file "order_db" "$SAMPLE_DATA_DIR/order-service-data.sql" "Order Service"

echo -e "\n5. Payment Service"
load_sql_file "payment_db" "$SAMPLE_DATA_DIR/payment-service-data.sql" "Payment Service"

echo -e "\n==================================="
echo -e "${GREEN}Sample data loading complete!${NC}"
echo -e "===================================\n"

echo "Summary of loaded data:"
echo "- 8 Users (including 1 admin, 2 sellers)"
echo "- 28 Products across 14 categories"
echo "- 11 Shopping carts (3 with items, 3 anonymous)"
echo "- 6 Orders in various statuses"
echo "- 8 Payments with transaction history"
echo ""
echo "Default password for all users: password123"
echo ""
echo "Test users:"
echo "- admin@ecommerce.com (ADMIN)"
echo "- seller1@example.com (SELLER)"
echo "- john.doe@example.com (USER)"
echo ""
echo -e "${YELLOW}You can now start all microservices and test the complete flow!${NC}" 