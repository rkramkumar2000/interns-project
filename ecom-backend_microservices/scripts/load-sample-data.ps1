# Load Sample Data for All Microservices (PowerShell version)
# This script loads sample data into all microservice databases
# Prerequisites: Docker containers must be running with databases created

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Loading Sample Data for Microservices" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan

# Configuration
$POSTGRES_CONTAINER = "ecom-backend-postgres-1"  # Adjust if your container name is different
$POSTGRES_USER = "postgres"
$POSTGRES_PASSWORD = "postgres"

# Function to load SQL file into a specific database
function Load-SqlFile {
    param(
        [string]$DbName,
        [string]$SqlFile,
        [string]$ServiceName
    )
    
    Write-Host "`nLoading data for $ServiceName..." -ForegroundColor Yellow
    
    # Check if SQL file exists
    if (-not (Test-Path $SqlFile)) {
        Write-Host "Error: SQL file not found: $SqlFile" -ForegroundColor Red
        return $false
    }
    
    # Load the SQL file
    Get-Content $SqlFile | docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $DbName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Successfully loaded data for $ServiceName" -ForegroundColor Green
        return $true
    } else {
        Write-Host "✗ Failed to load data for $ServiceName" -ForegroundColor Red
        return $false
    }
}

# Check if postgres container is running
$containerRunning = docker ps --format "table {{.Names}}" | Select-String -Pattern $POSTGRES_CONTAINER -Quiet

if (-not $containerRunning) {
    Write-Host "Error: PostgreSQL container '$POSTGRES_CONTAINER' is not running" -ForegroundColor Red
    Write-Host "Please run: docker-compose -f docker-compose-microservices.yml up -d"
    exit 1
}

# Get the directory where this script is located
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$SAMPLE_DATA_DIR = Join-Path $SCRIPT_DIR "microservices-sample-data"

Write-Host "`nLoading sample data from: $SAMPLE_DATA_DIR" -ForegroundColor Yellow

# Load data for each service
Write-Host "`n1. Auth Service" -ForegroundColor Cyan
Load-SqlFile -DbName "auth_db" -SqlFile (Join-Path $SAMPLE_DATA_DIR "auth-service-data.sql") -ServiceName "Auth Service"

Write-Host "`n2. Product Service" -ForegroundColor Cyan
Load-SqlFile -DbName "product_db" -SqlFile (Join-Path $SAMPLE_DATA_DIR "product-service-data.sql") -ServiceName "Product Service"

Write-Host "`n3. Cart Service" -ForegroundColor Cyan
Load-SqlFile -DbName "cart_db" -SqlFile (Join-Path $SAMPLE_DATA_DIR "cart-service-data.sql") -ServiceName "Cart Service"

Write-Host "`n4. Order Service" -ForegroundColor Cyan
Load-SqlFile -DbName "order_db" -SqlFile (Join-Path $SAMPLE_DATA_DIR "order-service-data.sql") -ServiceName "Order Service"

Write-Host "`n5. Payment Service" -ForegroundColor Cyan
Load-SqlFile -DbName "payment_db" -SqlFile (Join-Path $SAMPLE_DATA_DIR "payment-service-data.sql") -ServiceName "Payment Service"

Write-Host "`n===================================" -ForegroundColor Green
Write-Host "Sample data loading complete!" -ForegroundColor Green
Write-Host "===================================`n" -ForegroundColor Green

Write-Host "Summary of loaded data:" -ForegroundColor Cyan
Write-Host "- 8 Users (including 1 admin, 2 sellers)"
Write-Host "- 28 Products across 14 categories"
Write-Host "- 11 Shopping carts (3 with items, 3 anonymous)"
Write-Host "- 6 Orders in various statuses"
Write-Host "- 8 Payments with transaction history"
Write-Host ""
Write-Host "Default password for all users: " -NoNewline
Write-Host "password123" -ForegroundColor Yellow
Write-Host ""
Write-Host "Test users:" -ForegroundColor Cyan
Write-Host "- admin@ecommerce.com (ADMIN)"
Write-Host "- seller1@example.com (SELLER)"
Write-Host "- john.doe@example.com (USER)"
Write-Host ""
Write-Host "You can now start all microservices and test the complete flow!" -ForegroundColor Yellow 