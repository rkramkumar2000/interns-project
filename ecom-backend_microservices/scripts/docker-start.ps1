# Docker Startup Script for E-Commerce Microservices (Windows)
# This script starts all services using Docker Compose

Write-Host "🐳 Starting E-Commerce Microservices with Docker..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Base directory
$BaseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $BaseDir

# Check if .env file exists for Stripe key
if (!(Test-Path "$BaseDir\.env")) {
    Write-Host "⚠️  Warning: .env file not found" -ForegroundColor Yellow
    Write-Host "Creating .env file with placeholder..."
    "STRIPE_SECRET_KEY=your_stripe_secret_key_here" | Out-File -FilePath "$BaseDir\.env" -Encoding UTF8
    Write-Host "Please update the STRIPE_SECRET_KEY in .env file" -ForegroundColor Yellow
    Write-Host ""
}

# Function to check if service is healthy
function Test-ServiceHealth {
    param(
        [string]$ServiceName,
        [string]$ContainerName
    )
    
    # Check if container is running
    $running = docker ps --format "table {{.Names}}" | Select-String -Pattern "^$ContainerName$"
    
    if ($running) {
        # Check health status
        $healthStatus = docker inspect --format='{{.State.Health.Status}}' $ContainerName 2>$null
        
        if ($healthStatus -eq "healthy") {
            Write-Host "✅ $ServiceName is healthy" -ForegroundColor Green
            return $true
        } elseif ($healthStatus -eq "" -or $null -eq $healthStatus) {
            # No health check defined, just check if running
            $isRunning = docker ps | Select-String -Pattern $ContainerName
            if ($isRunning) {
                Write-Host "✅ $ServiceName is running" -ForegroundColor Green
                return $true
            }
        } else {
            Write-Host "⏳ $ServiceName health status: $healthStatus" -ForegroundColor Yellow
            return $false
        }
    } else {
        Write-Host "❌ $ServiceName is not running" -ForegroundColor Red
        return $false
    }
}

# Function to wait for service
function Wait-ForService {
    param(
        [string]$ServiceName,
        [string]$ContainerName,
        [int]$MaxAttempts = 60
    )
    
    Write-Host "⏳ Waiting for $ServiceName to be ready..." -ForegroundColor Yellow
    
    $attempt = 0
    while ($attempt -lt $MaxAttempts) {
        if (Test-ServiceHealth -ServiceName $ServiceName -ContainerName $ContainerName) {
            return $true
        }
        Start-Sleep -Seconds 2
        $attempt++
    }
    
    Write-Host "❌ $ServiceName failed to become healthy after $MaxAttempts attempts" -ForegroundColor Red
    return $false
}

# Start all services
Write-Host "🚀 Starting all services with Docker Compose..." -ForegroundColor Green
$result = docker-compose -f docker-compose-microservices.yml up -d --build 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start services with Docker Compose" -ForegroundColor Red
    Write-Host $result
    exit 1
}

Write-Host ""
Write-Host "⏳ Waiting for services to initialize..." -ForegroundColor Yellow
Write-Host ""

# Wait for infrastructure services first
Write-Host "📦 Checking infrastructure services..." -ForegroundColor Cyan
Wait-ForService -ServiceName "PostgreSQL" -ContainerName "ecom-postgres"
Wait-ForService -ServiceName "Redis" -ContainerName "ecom-redis"
Wait-ForService -ServiceName "Kafka" -ContainerName "ecom-kafka"
Wait-ForService -ServiceName "Elasticsearch" -ContainerName "elasticsearch"

Write-Host ""
Write-Host "🔧 Checking microservices infrastructure..." -ForegroundColor Cyan
Wait-ForService -ServiceName "Config Server" -ContainerName "ecom-config-server"
Wait-ForService -ServiceName "Eureka Server" -ContainerName "ecom-eureka-server"

Write-Host ""
Write-Host "📡 Checking API Gateway..." -ForegroundColor Cyan
Wait-ForService -ServiceName "API Gateway" -ContainerName "ecom-api-gateway"

Write-Host ""
Write-Host "🎯 Checking business services..." -ForegroundColor Cyan
Wait-ForService -ServiceName "Auth Service" -ContainerName "ecom-auth-service"
Wait-ForService -ServiceName "Product Service" -ContainerName "ecom-product-service"
Wait-ForService -ServiceName "Cart Service" -ContainerName "ecom-cart-service"
Wait-ForService -ServiceName "Order Service" -ContainerName "ecom-order-service"
Wait-ForService -ServiceName "Payment Service" -ContainerName "ecom-payment-service"
Wait-ForService -ServiceName "Notification Service" -ContainerName "ecom-notification-service"

Write-Host ""
Write-Host "🌐 Checking frontend..." -ForegroundColor Cyan
Wait-ForService -ServiceName "Frontend" -ContainerName "ecom-frontend"

Write-Host ""
Write-Host "📊 Checking monitoring services..." -ForegroundColor Cyan
Wait-ForService -ServiceName "Prometheus" -ContainerName "ecom-prometheus"
Wait-ForService -ServiceName "Grafana" -ContainerName "ecom-grafana"
Wait-ForService -ServiceName "Zipkin" -ContainerName "ecom-zipkin"

# Summary
Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "✅ All services are up and running!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 Access Points:" -ForegroundColor Cyan
Write-Host "   - Frontend: http://localhost:3000"
Write-Host "   - API Gateway: http://localhost:8080"
Write-Host "   - Eureka Dashboard: http://localhost:8761"
Write-Host "   - Grafana: http://localhost:3001 (admin/admin)"
Write-Host "   - Zipkin: http://localhost:9411"
Write-Host "   - Kibana: http://localhost:5601"
Write-Host ""
Write-Host "📚 API Documentation:" -ForegroundColor Cyan
Write-Host "   - Gateway Swagger: http://localhost:8080/swagger-ui.html"
Write-Host "   - Individual service docs available through gateway"
Write-Host ""
Write-Host "🛑 To stop all services:" -ForegroundColor Yellow
Write-Host "   .\scripts\docker-stop.ps1"
Write-Host "" 