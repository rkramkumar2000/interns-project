# Prerequisites Check Script for E-Commerce Platform (Windows)
# This script verifies all required software is installed

Write-Host "🔍 E-Commerce Platform - Prerequisites Check" -ForegroundColor Cyan
Write-Host "==========================================="
Write-Host ""

# Track overall status
$allGood = $true

# Function to check if command exists
function Test-Command {
    param(
        [string]$Command,
        [string]$Name,
        [string]$MinVersion,
        [string]$InstallUrl
    )
    
    try {
        $result = & $Command --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ $Name found" -ForegroundColor Green -NoNewline
            Write-Host ": $($result[0])"
            
            if ($MinVersion) {
                Write-Host "   Required: $MinVersion or higher"
            }
            return $true
        }
    }
    catch {
        # Command not found
    }
    
    Write-Host "❌ $Name not found" -ForegroundColor Red
    Write-Host "   Install from: $InstallUrl"
    $script:allGood = $false
    Write-Host ""
    return $false
}

# Function to check port availability
function Test-Port {
    param(
        [int]$Port,
        [string]$Service
    )
    
    $connection = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    if ($connection) {
        Write-Host "❌ Port $Port is in use" -ForegroundColor Red -NoNewline
        Write-Host " (needed for $Service)"
        Write-Host "   Run: netstat -ano | findstr :$Port to see what's using it"
        $script:allGood = $false
    }
    else {
        Write-Host "✅ Port $Port is available" -ForegroundColor Green -NoNewline
        Write-Host " (for $Service)"
    }
}

Write-Host "📋 Checking Required Software..." -ForegroundColor Yellow
Write-Host "--------------------------------"

# Docker check
Test-Command -Command "docker" -Name "Docker" -MinVersion "20.10" -InstallUrl "https://docker.com"

# Docker Compose check (try both v1 and v2)
$dockerComposeFound = $false
if (Test-Command -Command "docker-compose" -Name "Docker Compose" -MinVersion "2.0" -InstallUrl "Included with Docker Desktop") {
    $dockerComposeFound = $true
}
else {
    try {
        $dcVersion = docker compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Docker Compose V2 found" -ForegroundColor Green
            Write-Host "   $dcVersion"
            $dockerComposeFound = $true
        }
    }
    catch {
        # Docker compose v2 not found
    }
}

if (-not $dockerComposeFound) {
    Write-Host "❌ Docker Compose not found" -ForegroundColor Red
    Write-Host "   Install Docker Desktop from: https://docker.com"
    $allGood = $false
}
Write-Host ""

# Git check
Test-Command -Command "git" -Name "Git" -MinVersion "2.30" -InstallUrl "https://git-scm.com"

# Optional development tools
Write-Host "📋 Checking Optional Development Tools..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

# Java check
try {
    $javaVersion = java -version 2>&1
    if ($javaVersion -match "version `"(\d+)") {
        $majorVersion = $matches[1]
        if ($majorVersion -ge 17) {
            Write-Host "✅ Java found" -ForegroundColor Green -NoNewline
            Write-Host ": Java $majorVersion"
        }
        else {
            Write-Host "⚠️  Java found but version 17+ recommended" -ForegroundColor Yellow
            Write-Host "   Current: Java $majorVersion"
        }
    }
}
catch {
    Write-Host "⚠️  Java not found" -ForegroundColor Yellow -NoNewline
    Write-Host " (only needed for local development)"
    Write-Host "   Install from: https://adoptium.net/"
}
Write-Host ""

# Maven check
try {
    $mvnVersion = mvn -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Maven found" -ForegroundColor Green
        $mvnVersionLine = $mvnVersion | Select-String "Apache Maven"
        Write-Host "   $mvnVersionLine"
    }
}
catch {
    Write-Host "⚠️  Maven not found" -ForegroundColor Yellow -NoNewline
    Write-Host " (only needed for local development)"
    Write-Host "   Install from: https://maven.apache.org"
}
Write-Host ""

# Node.js check
try {
    $nodeVersion = node --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Node.js found" -ForegroundColor Green -NoNewline
        Write-Host ": $nodeVersion"
    }
}
catch {
    Write-Host "⚠️  Node.js not found" -ForegroundColor Yellow -NoNewline
    Write-Host " (only needed for frontend development)"
    Write-Host "   Install from: https://nodejs.org"
}
Write-Host ""

# System resources check
Write-Host "💻 Checking System Resources..." -ForegroundColor Yellow
Write-Host "-------------------------------"

$memory = Get-CimInstance Win32_PhysicalMemory | Measure-Object -Property capacity -Sum
$memoryGB = [Math]::Round($memory.Sum / 1GB, 2)
Write-Host "Memory: ${memoryGB}GB total"

if ($memoryGB -lt 4) {
    Write-Host "⚠️  Less than 4GB RAM detected" -ForegroundColor Red
    Write-Host "   Monolithic needs 4GB, Microservices needs 8GB"
    $allGood = $false
}
else {
    Write-Host "✅ Sufficient memory" -ForegroundColor Green
}
Write-Host ""

# Port availability check
Write-Host "🔌 Checking Port Availability..." -ForegroundColor Yellow
Write-Host "--------------------------------"

Test-Port -Port 3000 -Service "Frontend"
Test-Port -Port 8080 -Service "Backend/API Gateway"
Test-Port -Port 5432 -Service "PostgreSQL"
Test-Port -Port 6379 -Service "Redis"
Write-Host ""

# Environment check
Write-Host "🔐 Checking Environment Setup..." -ForegroundColor Yellow
Write-Host "--------------------------------"

# Check for .env file
if (Test-Path ".env") {
    Write-Host "✅ .env file found" -ForegroundColor Green
    
    $envContent = Get-Content ".env" -Raw
    if ($envContent -match "STRIPE_SECRET_KEY=") {
        if ($envContent -match "STRIPE_SECRET_KEY=your_stripe") {
            Write-Host "⚠️  STRIPE_SECRET_KEY needs to be updated" -ForegroundColor Yellow
            Write-Host "   Get your key from: https://stripe.com/dashboard"
        }
        else {
            Write-Host "✅ STRIPE_SECRET_KEY is configured" -ForegroundColor Green
        }
    }
    else {
        Write-Host "⚠️  STRIPE_SECRET_KEY not found in .env" -ForegroundColor Yellow
        Write-Host "   Add: STRIPE_SECRET_KEY=your_key_here"
    }
}
else {
    Write-Host "⚠️  .env file not found" -ForegroundColor Yellow
    Write-Host "   Create it with: echo 'STRIPE_SECRET_KEY=your_key' > .env"
}
Write-Host ""

# Docker daemon check
Write-Host "🐳 Checking Docker Status..." -ForegroundColor Yellow
Write-Host "----------------------------"

try {
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Docker daemon is running" -ForegroundColor Green
        
        Write-Host ""
        Write-Host "📝 Docker Desktop Resource Recommendations:"
        Write-Host "   - Memory: 8GB minimum"
        Write-Host "   - CPUs: 4 cores"
        Write-Host "   - Disk: 20GB"
        Write-Host "   Configure in Docker Desktop settings"
    }
    else {
        Write-Host "❌ Docker daemon is not running" -ForegroundColor Red
        Write-Host "   Start Docker Desktop"
        $allGood = $false
    }
}
catch {
    Write-Host "❌ Docker not found or not running" -ForegroundColor Red
    Write-Host "   Install and start Docker Desktop"
    $allGood = $false
}
Write-Host ""

# WSL2 check for Windows
Write-Host "🖥️  Checking WSL2 (Recommended for Windows)..." -ForegroundColor Yellow
Write-Host "----------------------------------------------"

try {
    $wslVersion = wsl --list --verbose 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ WSL2 is installed" -ForegroundColor Green
        Write-Host "   For best performance, run Linux scripts inside WSL2"
    }
    else {
        Write-Host "⚠️  WSL2 not found" -ForegroundColor Yellow
        Write-Host "   Install WSL2 for better Docker performance"
        Write-Host "   Run: wsl --install"
    }
}
catch {
    Write-Host "⚠️  WSL2 not found" -ForegroundColor Yellow
    Write-Host "   Recommended for better Docker performance"
}
Write-Host ""

# Summary
Write-Host "==========================================="
if ($allGood) {
    Write-Host "✅ All required prerequisites are met!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Ready to start! Run:" -ForegroundColor Cyan
    Write-Host "   .\scripts\docker-start.ps1" -ForegroundColor White
    Write-Host "   OR use Git Bash: ./scripts/docker-start.sh" -ForegroundColor White
}
else {
    Write-Host "❌ Some prerequisites are missing" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install missing components before proceeding."
    Write-Host "See PROJECT_MASTER_GUIDE.md for detailed instructions."
}
Write-Host "===========================================" 