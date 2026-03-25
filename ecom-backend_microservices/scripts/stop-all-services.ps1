# E-Commerce Microservices Shutdown Script for Windows
# This script stops all running microservices

Write-Host "🛑 Stopping E-Commerce Microservices Platform..." -ForegroundColor Red
Write-Host "================================================" -ForegroundColor Red

# Base directory
$BaseDir = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location $BaseDir

# Function to stop a service
function Stop-MicroService {
    param(
        [string]$ServiceName
    )
    
    $pidFile = "$BaseDir\logs\$ServiceName.pid"
    
    if (Test-Path $pidFile) {
        $processId = Get-Content $pidFile
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        
        if ($process) {
            Write-Host "🛑 Stopping $ServiceName (PID: $processId)..." -ForegroundColor Yellow
            Stop-Process -Id $processId -Force
            Start-Sleep -Seconds 2
            Write-Host "✅ $ServiceName stopped" -ForegroundColor Green
        } else {
            Write-Host "ℹ️  $ServiceName is not running" -ForegroundColor Yellow
        }
        Remove-Item $pidFile -Force
    } else {
        Write-Host "ℹ️  No PID file found for $ServiceName" -ForegroundColor Yellow
    }
}

# Stop all services in reverse order
Write-Host "📋 Stopping services in reverse dependency order..." -ForegroundColor Cyan
Write-Host ""

# Business services
Stop-MicroService -ServiceName "Notification Service"
Stop-MicroService -ServiceName "Payment Service"
Stop-MicroService -ServiceName "Order Service"
Stop-MicroService -ServiceName "Cart Service"
Stop-MicroService -ServiceName "Product Service"
Stop-MicroService -ServiceName "Auth Service"

# Core services
Stop-MicroService -ServiceName "API Gateway"
Stop-MicroService -ServiceName "Eureka Server"
Stop-MicroService -ServiceName "Config Server"

Write-Host ""

# Alternative method: Kill all Maven/Java processes related to Spring Boot
Write-Host "🔍 Checking for any remaining Spring Boot processes..." -ForegroundColor Cyan

# Find all Java processes running Spring Boot
$springBootProcesses = Get-Process | Where-Object {
    $_.ProcessName -eq "java" -and 
    $_.CommandLine -like "*spring-boot:run*"
}

if ($springBootProcesses.Count -gt 0) {
    Write-Host "⚠️  Found $($springBootProcesses.Count) Spring Boot process(es) still running" -ForegroundColor Yellow
    Write-Host "Stopping all Spring Boot processes..." -ForegroundColor Yellow
    
    foreach ($process in $springBootProcesses) {
        Stop-Process -Id $process.Id -Force
    }
    
    Start-Sleep -Seconds 2
    Write-Host "✅ All Spring Boot processes stopped" -ForegroundColor Green
} else {
    Write-Host "✅ No remaining Spring Boot processes found" -ForegroundColor Green
}

# Clean up any orphaned background jobs
Get-Job | Where-Object { $_.State -eq 'Running' } | Stop-Job
Get-Job | Remove-Job

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "🎉 All services have been stopped!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Service logs are preserved in: $BaseDir\logs\" -ForegroundColor Yellow
Write-Host ""
Write-Host "To stop infrastructure services, run:" -ForegroundColor Cyan
Write-Host "  docker-compose -f docker-compose-microservices.yml down" -ForegroundColor Yellow
Write-Host ""
Write-Host "To clean up everything (including data), run:" -ForegroundColor Cyan
Write-Host "  docker-compose -f docker-compose-microservices.yml down -v" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Green 