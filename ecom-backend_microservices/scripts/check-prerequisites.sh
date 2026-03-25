#!/bin/bash

# Prerequisites Check Script for E-Commerce Platform
# This script verifies all required software is installed

echo "🔍 E-Commerce Platform - Prerequisites Check"
echo "==========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Track overall status
ALL_GOOD=true

# Function to check command exists
check_command() {
    local cmd=$1
    local name=$2
    local min_version=$3
    local install_url=$4
    
    if command -v $cmd &> /dev/null; then
        version=$($cmd --version 2>&1 | head -n1)
        echo -e "${GREEN}✅ $name found${NC}: $version"
        
        # Version check if provided
        if [ ! -z "$min_version" ]; then
            # Extract version number (basic check)
            current_version=$($cmd --version 2>&1 | grep -oE '[0-9]+\.[0-9]+' | head -n1)
            if [ ! -z "$current_version" ]; then
                echo "   Required: $min_version or higher"
            fi
        fi
    else
        echo -e "${RED}❌ $name not found${NC}"
        echo "   Install from: $install_url"
        ALL_GOOD=false
    fi
    echo ""
}

# Function to check port availability
check_port() {
    local port=$1
    local service=$2
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${RED}❌ Port $port is in use${NC} (needed for $service)"
        echo "   Run: lsof -i :$port to see what's using it"
        ALL_GOOD=false
    else
        echo -e "${GREEN}✅ Port $port is available${NC} (for $service)"
    fi
}

# Function to check environment variable
check_env() {
    local var=$1
    local description=$2
    
    if [ ! -z "${!var}" ]; then
        echo -e "${GREEN}✅ $var is set${NC}"
    else
        echo -e "${YELLOW}⚠️  $var not set${NC} - $description"
    fi
}

echo "📋 Checking Required Software..."
echo "--------------------------------"

# Docker checks
check_command "docker" "Docker" "20.10" "https://docker.com"

# Docker Compose check (try both v1 and v2 syntax)
if command -v docker-compose &> /dev/null; then
    check_command "docker-compose" "Docker Compose" "2.0" "Included with Docker Desktop"
elif docker compose version &> /dev/null 2>&1; then
    echo -e "${GREEN}✅ Docker Compose V2 found${NC}"
    docker compose version
    echo ""
else
    echo -e "${RED}❌ Docker Compose not found${NC}"
    echo "   Install Docker Desktop from: https://docker.com"
    ALL_GOOD=false
    echo ""
fi

# Git check
check_command "git" "Git" "2.30" "https://git-scm.com"

# Optional development tools
echo "📋 Checking Optional Development Tools..."
echo "----------------------------------------"

# Java check
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n1)
    if [[ $java_version == *"17"* ]] || [[ $java_version == *"18"* ]] || [[ $java_version == *"19"* ]] || [[ $java_version == *"20"* ]] || [[ $java_version == *"21"* ]]; then
        echo -e "${GREEN}✅ Java found${NC}: $java_version"
    else
        echo -e "${YELLOW}⚠️  Java found but version 17+ recommended${NC}"
        echo "   Current: $java_version"
    fi
else
    echo -e "${YELLOW}⚠️  Java not found${NC} (only needed for local development)"
    echo "   Install from: https://adoptium.net/"
fi
echo ""

# Maven check
if command -v mvn &> /dev/null; then
    check_command "mvn" "Maven" "3.8" "https://maven.apache.org"
else
    echo -e "${YELLOW}⚠️  Maven not found${NC} (only needed for local development)"
    echo "   Install from: https://maven.apache.org"
    echo ""
fi

# Node.js check
if command -v node &> /dev/null; then
    check_command "node" "Node.js" "18" "https://nodejs.org"
else
    echo -e "${YELLOW}⚠️  Node.js not found${NC} (only needed for frontend development)"
    echo "   Install from: https://nodejs.org"
    echo ""
fi

# Check system resources
echo "💻 Checking System Resources..."
echo "-------------------------------"

# Memory check (Linux/Mac)
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    total_mem=$(free -g | awk '/^Mem:/{print $2}')
    echo "Memory: ${total_mem}GB total"
    if [ $total_mem -lt 4 ]; then
        echo -e "${RED}⚠️  Less than 4GB RAM detected${NC}"
        echo "   Monolithic needs 4GB, Microservices needs 8GB"
        ALL_GOOD=false
    else
        echo -e "${GREEN}✅ Sufficient memory${NC}"
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    total_mem=$(( $(sysctl -n hw.memsize) / 1024 / 1024 / 1024 ))
    echo "Memory: ${total_mem}GB total"
    if [ $total_mem -lt 4 ]; then
        echo -e "${RED}⚠️  Less than 4GB RAM detected${NC}"
        echo "   Monolithic needs 4GB, Microservices needs 8GB"
        ALL_GOOD=false
    else
        echo -e "${GREEN}✅ Sufficient memory${NC}"
    fi
else
    echo "⚠️  Cannot check memory on this system"
fi
echo ""

# Port availability check
echo "🔌 Checking Port Availability..."
echo "--------------------------------"

# Check if lsof is available
if command -v lsof &> /dev/null; then
    check_port 3000 "Frontend"
    check_port 8080 "Backend/API Gateway"
    check_port 5432 "PostgreSQL"
    check_port 6379 "Redis"
else
    echo -e "${YELLOW}⚠️  'lsof' command not found${NC}"
    echo "   Cannot check port availability"
    echo "   Ensure these ports are free: 3000, 8080, 5432, 6379"
fi
echo ""

# Environment check
echo "🔐 Checking Environment Setup..."
echo "--------------------------------"

# Check for .env file
if [ -f ".env" ]; then
    echo -e "${GREEN}✅ .env file found${NC}"
    
    # Check for Stripe key
    if grep -q "STRIPE_SECRET_KEY=" .env; then
        if grep -q "STRIPE_SECRET_KEY=your_stripe" .env; then
            echo -e "${YELLOW}⚠️  STRIPE_SECRET_KEY needs to be updated${NC}"
            echo "   Get your key from: https://stripe.com/dashboard"
        else
            echo -e "${GREEN}✅ STRIPE_SECRET_KEY is configured${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  STRIPE_SECRET_KEY not found in .env${NC}"
        echo "   Add: STRIPE_SECRET_KEY=your_key_here"
    fi
else
    echo -e "${YELLOW}⚠️  .env file not found${NC}"
    echo "   Create it with: echo 'STRIPE_SECRET_KEY=your_key' > .env"
fi
echo ""

# Docker daemon check
echo "🐳 Checking Docker Status..."
echo "----------------------------"

if docker info &> /dev/null; then
    echo -e "${GREEN}✅ Docker daemon is running${NC}"
    
    # Check Docker resources
    if [[ "$OSTYPE" == "darwin"* ]] || [[ "$OSTYPE" == "msys" ]]; then
        echo ""
        echo "📝 Docker Desktop Resource Recommendations:"
        echo "   - Memory: 8GB minimum"
        echo "   - CPUs: 4 cores"
        echo "   - Disk: 20GB"
        echo "   Configure in Docker Desktop settings"
    fi
else
    echo -e "${RED}❌ Docker daemon is not running${NC}"
    echo "   Start Docker Desktop or run: sudo systemctl start docker"
    ALL_GOOD=false
fi
echo ""

# Summary
echo "==========================================="
if [ "$ALL_GOOD" = true ]; then
    echo -e "${GREEN}✅ All required prerequisites are met!${NC}"
    echo ""
    echo "🚀 Ready to start! Run:"
    echo "   ./scripts/docker-start.sh"
else
    echo -e "${RED}❌ Some prerequisites are missing${NC}"
    echo ""
    echo "Please install missing components before proceeding."
    echo "See PROJECT_MASTER_GUIDE.md for detailed instructions."
fi
echo "===========================================" 