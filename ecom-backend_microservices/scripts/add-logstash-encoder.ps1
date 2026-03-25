# Script to add Logstash encoder dependency to all microservices

$microservices = @(
    "auth-service",
    "product-service",
    "cart-service", 
    "order-service",
    "payment-service",
    "notification-service"
)

$dependencyToAdd = @'
        
        <!-- Logstash Logback Encoder for ELK Stack -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>
'@

foreach ($service in $microservices) {
    $pomPath = "microservices/$service/pom.xml"
    
    if (Test-Path $pomPath) {
        Write-Host "Processing $service..." -ForegroundColor Green
        
        $pomContent = Get-Content $pomPath -Raw
        
        # Check if dependency already exists
        if ($pomContent -notmatch "logstash-logback-encoder") {
            # Find the position before test dependencies
            $pattern = '(\s*)<!-- Test Dependencies -->'
            
            if ($pomContent -match $pattern) {
                $indentation = $matches[1]
                $replacement = "$dependencyToAdd`n$indentation<!-- Test Dependencies -->"
                $pomContent = $pomContent -replace $pattern, $replacement
                
                # Write back to file
                Set-Content -Path $pomPath -Value $pomContent
                Write-Host "  Added Logstash encoder to $service" -ForegroundColor Yellow
            } else {
                Write-Host "  Could not find insertion point in $service" -ForegroundColor Red
            }
        } else {
            Write-Host "  Logstash encoder already exists in $service" -ForegroundColor Cyan
        }
    } else {
        Write-Host "  POM file not found for $service" -ForegroundColor Red
    }
}

Write-Host "`nDone!" -ForegroundColor Green 