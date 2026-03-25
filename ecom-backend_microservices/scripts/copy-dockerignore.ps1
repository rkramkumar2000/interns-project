# Script to copy .dockerignore template to all microservices

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$MicroservicesDir = Join-Path $ScriptDir "..\microservices"
$TemplateFile = Join-Path $MicroservicesDir ".dockerignore.template"

# Check if template exists
if (!(Test-Path $TemplateFile)) {
    Write-Error "Template file not found at $TemplateFile"
    exit 1
}

# List of services
$Services = @(
    "auth-service",
    "product-service",
    "cart-service",
    "order-service",
    "payment-service",
    "notification-service",
    "eureka-server",
    "config-server"
)

# Copy template to each service
foreach ($service in $Services) {
    $ServiceDir = Join-Path $MicroservicesDir $service
    if (Test-Path $ServiceDir) {
        Copy-Item $TemplateFile -Destination (Join-Path $ServiceDir ".dockerignore") -Force
        Write-Host "✅ Copied .dockerignore to $service" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Directory not found: $ServiceDir" -ForegroundColor Yellow
    }
}

# Also copy to API Gateway
$ApiGatewayDir = Join-Path $ScriptDir "..\api-gateway"
if (Test-Path $ApiGatewayDir) {
    Copy-Item $TemplateFile -Destination (Join-Path $ApiGatewayDir ".dockerignore") -Force
    Write-Host "✅ Copied .dockerignore to api-gateway" -ForegroundColor Green
} else {
    Write-Host "⚠️  Directory not found: $ApiGatewayDir" -ForegroundColor Yellow
}

Write-Host "✅ Done! All services now have .dockerignore files" -ForegroundColor Green 