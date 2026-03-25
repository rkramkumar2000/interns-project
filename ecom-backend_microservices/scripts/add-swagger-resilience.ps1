# Script to add Swagger/OpenAPI and Resilience4j to all microservices

$microservices = @(
    "product-service",
    "cart-service", 
    "order-service",
    "payment-service",
    "notification-service"
)

$dependenciesToAdd = @'
        
        <!-- Swagger/OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
        
        <!-- Resilience4j -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.1.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-feign</artifactId>
            <version>2.1.0</version>
        </dependency>
'@

foreach ($service in $microservices) {
    $pomPath = "microservices/$service/pom.xml"
    
    if (Test-Path $pomPath) {
        Write-Host "Processing $service..." -ForegroundColor Green
        
        $pomContent = Get-Content $pomPath -Raw
        
        # Check if dependencies already exist
        if ($pomContent -notmatch "springdoc-openapi-starter-webmvc-ui") {
            # Find the position before test dependencies
            $pattern = '(\s*)<!-- Test Dependencies -->'
            
            if ($pomContent -match $pattern) {
                $indentation = $matches[1]
                $replacement = "$dependenciesToAdd`n$indentation<!-- Test Dependencies -->"
                $pomContent = $pomContent -replace $pattern, $replacement
                
                # Write back to file
                Set-Content -Path $pomPath -Value $pomContent
                Write-Host "  Added dependencies to $service" -ForegroundColor Yellow
            } else {
                Write-Host "  Could not find insertion point in $service" -ForegroundColor Red
            }
        } else {
            Write-Host "  Dependencies already exist in $service" -ForegroundColor Cyan
        }
    } else {
        Write-Host "  POM file not found for $service" -ForegroundColor Red
    }
}

Write-Host "`nDone!" -ForegroundColor Green 