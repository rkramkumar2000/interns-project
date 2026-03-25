#!/bin/bash

# Script to copy .dockerignore template to all microservices

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MICROSERVICES_DIR="$SCRIPT_DIR/../microservices"
TEMPLATE_FILE="$MICROSERVICES_DIR/.dockerignore.template"

# Check if template exists
if [ ! -f "$TEMPLATE_FILE" ]; then
    echo "Error: Template file not found at $TEMPLATE_FILE"
    exit 1
fi

# List of services
SERVICES=(
    "auth-service"
    "product-service"
    "cart-service"
    "order-service"
    "payment-service"
    "notification-service"
    "eureka-server"
    "config-server"
)

# Copy template to each service
for service in "${SERVICES[@]}"; do
    SERVICE_DIR="$MICROSERVICES_DIR/$service"
    if [ -d "$SERVICE_DIR" ]; then
        cp "$TEMPLATE_FILE" "$SERVICE_DIR/.dockerignore"
        echo "✅ Copied .dockerignore to $service"
    else
        echo "⚠️  Directory not found: $SERVICE_DIR"
    fi
done

# Also copy to API Gateway
API_GATEWAY_DIR="$SCRIPT_DIR/../api-gateway"
if [ -d "$API_GATEWAY_DIR" ]; then
    cp "$TEMPLATE_FILE" "$API_GATEWAY_DIR/.dockerignore"
    echo "✅ Copied .dockerignore to api-gateway"
else
    echo "⚠️  Directory not found: $API_GATEWAY_DIR"
fi

echo "✅ Done! All services now have .dockerignore files" 