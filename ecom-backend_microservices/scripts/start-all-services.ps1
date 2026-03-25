# E-Commerce Microservices Startup Script for Windows
# This script starts all microservices in the correct order

Write-Host "🚀 Starting E-Commerce Microservices Platform..." -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

# Base directory
$BaseDir = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location $BaseDir

# Function to check if service is running
function Test-ServiceHealth {
    param(
        [int]$Port,
        [string]$ServiceName,
        [int]$MaxAttempts = 30
    )
    
    Write-Host "⏳ Waiting for $ServiceName to start on port $Port..." -ForegroundColor Yellow
    
    for ($i = 0; $i -lt $MaxAttempts; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" -UseBasicParsing -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Host "✅ $ServiceName is running!" -ForegroundColor Green
                return $true
            }
        }
        catch {
            # Service not ready yet
        }
        Start-Sleep -Seconds 1
    }
    
    Write-Host "❌ $ServiceName failed to start on port $Port" -ForegroundColor Red
    return $false
}

# Function to start a service
function Start-MicroService {
    param(
        [string]$ServiceDir,
        [string]$ServiceName,
        [int]$Port
    )
    
    Write-Host "🔄 Starting $ServiceName..." -ForegroundColor Yellow
    Set-Location "$BaseDir\$ServiceDir"
    
    # Create logs directory if it doesn't exist
    $LogsDir = "$BaseDir\logs"
    if (!(Test-Path $LogsDir)) {
        New-Item -ItemType Directory -Path $LogsDir | Out-Null
    }
    
    # Start service in background
    $process = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" `
        -RedirectStandardOutput "$LogsDir\$ServiceName.log" `
        -RedirectStandardError "$LogsDir\$ServiceName.error.log" `
        -PassThru -WindowStyle Hidden
    
    # Save PID
    $process.Id | Out-File "$LogsDir\$ServiceName.pid"
    
    # Check if service started successfully
    if (Test-ServiceHealth -Port $Port -ServiceName $ServiceName) {
        Write-Host "✅ $ServiceName started successfully (PID: $($process.Id))" -ForegroundColor Green
        Write-Host ""
        return $true
    } else {
        Write-Host "❌ Failed to start $ServiceName" -ForegroundColor Red
        return $false
    }
}

# Check if infrastructure is running
Write-Host "🔍 Checking infrastructure services..." -ForegroundColor Cyan
$postgresRunning = docker ps | Select-String "ecommerce-postgres"
if (!$postgresRunning) {
    Write-Host "❌ PostgreSQL is not running. Please start infrastructure first:" -ForegroundColor Red
    Write-Host "   docker-compose -f docker-compose-microservices.yml up -d" -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ Infrastructure services are running" -ForegroundColor Green
Write-Host ""

# Start services in order
Write-Host "🏁 Starting microservices in dependency order..." -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

# 1. Config Server
if (!(Start-MicroService -ServiceDir "microservices\config-server" -ServiceName "Config Server" -Port 8888)) {
    Write-Host "Cannot proceed without Config Server" -ForegroundColor Red
    exit 1
}

# 2. Eureka Server
if (!(Start-MicroService -ServiceDir "microservices\eureka-server" -ServiceName "Eureka Server" -Port 8761)) {
    Write-Host "Cannot proceed without Eureka Server" -ForegroundColor Red
    exit 1
}

# 3. API Gateway
if (!(Start-MicroService -ServiceDir "api-gateway" -ServiceName "API Gateway" -Port 8080)) {
    Write-Host "Cannot proceed without API Gateway" -ForegroundColor Red
    exit 1
}

# 4. Business Services
Write-Host "🚀 Starting business services..." -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Start all business services in parallel
$jobs = @()
$jobs += Start-Job -ScriptBlock {
    param($BaseDir)
    Set-Location "$BaseDir\microservices\auth-service"
    & mvn spring-boot:run
} -ArgumentList $BaseDir

$jobs += Start-Job -ScriptBlock {
    param($BaseDir)
    Set-Location "$BaseDir\microservices\product-service"
    & mvn spring-boot:run
} -ArgumentList $BaseDir

$jobs += Start-Job -ScriptBlock {
    param($BaseDir)
    Set-Location "$BaseDir\microservices\cart-service"
    & mvn spring-boot:run
} -ArgumentList $BaseDir

$jobs += Start-Job -ScriptBlock {
    param($BaseDir)
    Set-Location "$BaseDir\microservices\order-service"
    & mvn spring-boot:run
} -ArgumentList $BaseDir

$jobs += Start-Job -ScriptBlock {
    param($BaseDir)
    Set-Location "$BaseDir\microservices\payment-service"
    & mvn spring-boot:run
} -ArgumentList $BaseDir

$jobs += Start-Job -ScriptBlock {
    param($BaseDir)
    Set-Location "$BaseDir\microservices\notification-service"
    & mvn spring-boot:run
} -ArgumentList $BaseDir

# Wait for services to start
Write-Host "⏳ Waiting for all business services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check each service
$services = @(
    @{Name="Auth Service"; Port=8081},
    @{Name="Product Service"; Port=8083},
    @{Name="Cart Service"; Port=8084},
    @{Name="Order Service"; Port=8085},
    @{Name="Payment Service"; Port=8086},
    @{Name="Notification Service"; Port=8087}
)

foreach ($service in $services) {
    Test-ServiceHealth -Port $service.Port -ServiceName $service.Name | Out-Null
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "🎉 All services have been started!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Service Status:" -ForegroundColor Cyan
Write-Host "  - Config Server:        http://localhost:8888"
Write-Host "  - Eureka Dashboard:     http://localhost:8761"
Write-Host "  - API Gateway:          http://localhost:8080"
Write-Host "  - Auth Service:         http://localhost:8081"
Write-Host "  - Product Service:      http://localhost:8083"
Write-Host "  - Cart Service:         http://localhost:8084"
Write-Host "  - Order Service:        http://localhost:8085"
Write-Host "  - Payment Service:      http://localhost:8086"
Write-Host "  - Notification Service: http://localhost:8087"
Write-Host ""
Write-Host "📊 Monitoring & Documentation:" -ForegroundColor Cyan
Write-Host "  - Swagger UI:          http://localhost:{service-port}/swagger-ui.html"
Write-Host "  - Kibana:              http://localhost:5601"
Write-Host "  - Prometheus:          http://localhost:9090"
Write-Host "  - Grafana:             http://localhost:3000"
Write-Host "  - Zipkin:              http://localhost:9411"
Write-Host "  - Kafka UI:            http://localhost:9000"
Write-Host ""
Write-Host "📝 Logs are available in: $BaseDir\logs\" -ForegroundColor Yellow
Write-Host ""
Write-Host "To stop all services, run: .\scripts\stop-all-services.ps1" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Green 