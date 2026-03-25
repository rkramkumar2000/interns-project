#!/bin/bash

# Docker Stop Script for E-Commerce Microservices
# This script stops all services using Docker Compose

echo "🛑 Stopping E-Commerce Microservices with Docker..."
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Base directory
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Ask for confirmation
echo -e "${YELLOW}⚠️  This will stop all microservices containers.${NC}"
read -p "Are you sure you want to continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operation cancelled."
    exit 0
fi

# Stop all services
echo ""
echo "🛑 Stopping all services..."
docker-compose -f docker-compose-microservices.yml down

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ All services stopped successfully${NC}"
else
    echo -e "${RED}❌ Error stopping services${NC}"
    exit 1
fi

# Ask if user wants to remove volumes
echo ""
echo -e "${YELLOW}Do you want to remove data volumes as well?${NC}"
echo "This will delete all data including databases!"
read -p "Remove volumes? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  Removing volumes..."
    docker-compose -f docker-compose-microservices.yml down -v
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Volumes removed successfully${NC}"
    else
        echo -e "${RED}❌ Error removing volumes${NC}"
    fi
fi

# Clean up dangling images
echo ""
echo -e "${YELLOW}Do you want to clean up dangling images?${NC}"
read -p "Clean up images? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧹 Cleaning up dangling images..."
    docker image prune -f
    echo -e "${GREEN}✅ Cleanup completed${NC}"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Docker shutdown complete!${NC}"
echo "=========================================="
echo ""
echo "To start services again, run:"
echo "   ./scripts/docker-start.sh"
echo "" 