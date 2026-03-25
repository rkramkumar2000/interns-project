# Docker Stop Script for E-Commerce Microservices (Windows)
# This script stops all services using Docker Compose

Write-Host "🛑 Stopping E-Commerce Microservices with Docker..." -ForegroundColor Red
Write-Host "==================================================" -ForegroundColor Red

# Base directory
$BaseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $BaseDir

# Ask for confirmation
Write-Host "⚠️  This will stop all microservices containers." -ForegroundColor Yellow
$confirmation = Read-Host "Are you sure you want to continue? (y/N)"
if ($confirmation -notmatch '^[Yy]$') {
    Write-Host "Operation cancelled."
    exit 0
}

# Stop all services
Write-Host ""
Write-Host "🛑 Stopping all services..." -ForegroundColor Yellow
$result = docker-compose -f docker-compose-microservices.yml down 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ All services stopped successfully" -ForegroundColor Green
} else {
    Write-Host "❌ Error stopping services" -ForegroundColor Red
    Write-Host $result
    exit 1
}

# Ask if user wants to remove volumes
Write-Host ""
Write-Host "Do you want to remove data volumes as well?" -ForegroundColor Yellow
Write-Host "This will delete all data including databases!" -ForegroundColor Red
$removeVolumes = Read-Host "Remove volumes? (y/N)"
if ($removeVolumes -match '^[Yy]$') {
    Write-Host "🗑️  Removing volumes..." -ForegroundColor Yellow
    $result = docker-compose -f docker-compose-microservices.yml down -v 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Volumes removed successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ Error removing volumes" -ForegroundColor Red
        Write-Host $result
    }
}

# Clean up dangling images
Write-Host ""
Write-Host "Do you want to clean up dangling images?" -ForegroundColor Yellow
$cleanImages = Read-Host "Clean up images? (y/N)"
if ($cleanImages -match '^[Yy]$') {
    Write-Host "🧹 Cleaning up dangling images..." -ForegroundColor Yellow
    docker image prune -f
    Write-Host "✅ Cleanup completed" -ForegroundColor Green
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "✅ Docker shutdown complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "To start services again, run:" -ForegroundColor Cyan
Write-Host "   .\scripts\docker-start.ps1"
Write-Host "" 