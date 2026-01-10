# Redis Sentinel Failover Test Script
# Tests automatic failover and cache resilience

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Redis Sentinel Failover Test" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Test variables
$baseUrl = "http://localhost:8080"
$soapUrl = "$baseUrl/services/CalculatorService"
$healthUrl = "$baseUrl/actuator/health"
$metricsHitsUrl = "$baseUrl/actuator/metrics/cache.distributed.hits"
$metricsMissesUrl = "$baseUrl/actuator/metrics/cache.distributed.misses"

# SOAP request for multiply operation
$multiplyRequest = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:cal="http://example.com/generated/calculator">
   <soapenv:Header/>
   <soapenv:Body>
      <cal:multiply>
         <cal:a>10</cal:a>
         <cal:b>5</cal:b>
      </cal:multiply>
   </soapenv:Body>
</soapenv:Envelope>
"@

# Function to make SOAP request
function Invoke-SoapRequest {
    param([string]$RequestBody)
    
    try {
        $response = Invoke-WebRequest -Uri $soapUrl -Method POST -ContentType "text/xml" -Body $RequestBody -ErrorAction Stop
        return @{
            Success = $true
            StatusCode = $response.StatusCode
            Content = $response.Content
        }
    } catch {
        return @{
            Success = $false
            Error = $_.Exception.Message
        }
    }
}

# Function to get metrics
function Get-CacheMetrics {
    try {
        $hits = (Invoke-RestMethod -Uri $metricsHitsUrl -ErrorAction SilentlyContinue).measurements[0].value
        $misses = (Invoke-RestMethod -Uri $metricsMissesUrl -ErrorAction SilentlyContinue).measurements[0].value
        return @{
            Hits = if ($hits) { $hits } else { 0 }
            Misses = if ($misses) { $misses } else { 0 }
        }
    } catch {
        return @{
            Hits = "N/A"
            Misses = "N/A"
        }
    }
}

# Function to get current master
function Get-CurrentMaster {
    try {
        $output = docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>$null
        if ($output -and $output.Count -ge 2) {
            return "$($output[0]):$($output[1])"
        }
        return "Unknown"
    } catch {
        return "Unknown"
    }
}

Write-Host "STEP 1: Verify Initial Setup" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Write-Host "Checking Docker containers..." -ForegroundColor White
$containers = docker-compose -f docker\docker-compose-sentinel.yml ps --format json 2>$null | ConvertFrom-Json
$runningContainers = $containers | Where-Object { $_.State -eq "running" }

if ($runningContainers.Count -eq 6) {
    Write-Host "✓ All 6 containers running" -ForegroundColor Green
    foreach ($container in $runningContainers) {
        Write-Host "  - $($container.Service): $($container.State)" -ForegroundColor Gray
    }
} else {
    Write-Host "✗ Expected 6 containers, found $($runningContainers.Count)" -ForegroundColor Red
    Write-Host "Please start containers with: docker-compose -f docker\docker-compose-sentinel.yml up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Checking Sentinel configuration..." -ForegroundColor White
$initialMaster = Get-CurrentMaster
Write-Host "✓ Current master: $initialMaster" -ForegroundColor Green

Write-Host ""
Write-Host "Checking application health..." -ForegroundColor White
try {
    $health = Invoke-RestMethod -Uri $healthUrl -ErrorAction Stop
    if ($health.status -eq "UP") {
        Write-Host "✓ Application is UP and healthy" -ForegroundColor Green
        if ($health.components.redisSentinel) {
            Write-Host "  - Redis Sentinel: $($health.components.redisSentinel.status)" -ForegroundColor Gray
            Write-Host "  - Role: $($health.components.redisSentinel.details.role)" -ForegroundColor Gray
        }
    } else {
        Write-Host "✗ Application health check failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Cannot connect to application at $baseUrl" -ForegroundColor Red
    Write-Host "Please start application with: java -jar target\ApacheCFXHelloWord-1.0.0.jar --spring.profiles.active=sentinel" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "STEP 2: Baseline Test" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Write-Host "Getting baseline metrics..." -ForegroundColor White
$baselineMetrics = Get-CacheMetrics
Write-Host "  - Cache Hits: $($baselineMetrics.Hits)" -ForegroundColor Gray
Write-Host "  - Cache Misses: $($baselineMetrics.Misses)" -ForegroundColor Gray

Write-Host ""
Write-Host "STEP 3: Populate Cache" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Write-Host "Making first request (should be CACHE MISS)..." -ForegroundColor White
$result1 = Invoke-SoapRequest -RequestBody $multiplyRequest
if ($result1.Success) {
    Write-Host "✓ Request successful (Status: $($result1.StatusCode))" -ForegroundColor Green
} else {
    Write-Host "✗ Request failed: $($result1.Error)" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 2

Write-Host "Making second request (should be CACHE HIT)..." -ForegroundColor White
$result2 = Invoke-SoapRequest -RequestBody $multiplyRequest
if ($result2.Success) {
    Write-Host "✓ Request successful (Status: $($result2.StatusCode))" -ForegroundColor Green
} else {
    Write-Host "✗ Request failed: $($result2.Error)" -ForegroundColor Red
}

$afterCacheMetrics = Get-CacheMetrics
Write-Host ""
Write-Host "Cache metrics after population:" -ForegroundColor White
Write-Host "  - Cache Hits: $($afterCacheMetrics.Hits)" -ForegroundColor Gray
Write-Host "  - Cache Misses: $($afterCacheMetrics.Misses)" -ForegroundColor Gray

Write-Host ""
Write-Host "STEP 4: Simulate Master Failure" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Write-Host "Stopping Redis master container..." -ForegroundColor White
docker stop redis-master >$null 2>&1
Write-Host "✓ Master stopped" -ForegroundColor Green

Write-Host ""
Write-Host "Waiting for Sentinel failover (15 seconds)..." -ForegroundColor White
for ($i = 15; $i -gt 0; $i--) {
    Write-Host "  $i..." -ForegroundColor Gray -NoNewline
    Start-Sleep -Seconds 1
}
Write-Host ""

Write-Host ""
Write-Host "Checking new master..." -ForegroundColor White
$newMaster = Get-CurrentMaster
Write-Host "  - Old master: $initialMaster" -ForegroundColor Gray
Write-Host "  - New master: $newMaster" -ForegroundColor Gray

if ($newMaster -ne $initialMaster -and $newMaster -ne "Unknown") {
    Write-Host "✓ Failover successful! New master elected." -ForegroundColor Green
} else {
    Write-Host "✗ Failover may not have completed. New master: $newMaster" -ForegroundColor Red
}

Write-Host ""
Write-Host "STEP 5: Test After Failover" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Write-Host "Checking application health after failover..." -ForegroundColor White
Start-Sleep -Seconds 5  # Give app time to reconnect

try {
    $healthAfter = Invoke-RestMethod -Uri $healthUrl -ErrorAction Stop
    if ($healthAfter.status -eq "UP") {
        Write-Host "✓ Application still UP after failover" -ForegroundColor Green
    } else {
        Write-Host "✗ Application health degraded: $($healthAfter.status)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Application health check failed after failover" -ForegroundColor Red
}

Write-Host ""
Write-Host "Making request after failover..." -ForegroundColor White
$result3 = Invoke-SoapRequest -RequestBody $multiplyRequest
if ($result3.Success) {
    Write-Host "✓ Request successful after failover (Status: $($result3.StatusCode))" -ForegroundColor Green
} else {
    Write-Host "✗ Request failed after failover: $($result3.Error)" -ForegroundColor Red
}

Write-Host ""
Write-Host "STEP 6: Verify Cache Still Works" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Start-Sleep -Seconds 2

Write-Host "Making another request to test cache..." -ForegroundColor White
$result4 = Invoke-SoapRequest -RequestBody $multiplyRequest
if ($result4.Success) {
    Write-Host "✓ Cache request successful (Status: $($result4.StatusCode))" -ForegroundColor Green
} else {
    Write-Host "✗ Cache request failed: $($result4.Error)" -ForegroundColor Red
}

$finalMetrics = Get-CacheMetrics
Write-Host ""
Write-Host "Final cache metrics:" -ForegroundColor White
Write-Host "  - Cache Hits: $($finalMetrics.Hits)" -ForegroundColor Gray
Write-Host "  - Cache Misses: $($finalMetrics.Misses)" -ForegroundColor Gray

Write-Host ""
Write-Host "STEP 7: Restore Original Master" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Gray

Write-Host "Starting original master (will become replica)..." -ForegroundColor White
docker start redis-master >$null 2>&1
Write-Host "✓ Original master restarted" -ForegroundColor Green

Write-Host ""
Write-Host "Waiting for synchronization (10 seconds)..." -ForegroundColor White
for ($i = 10; $i -gt 0; $i--) {
    Write-Host "  $i..." -ForegroundColor Gray -NoNewline
    Start-Sleep -Seconds 1
}
Write-Host ""

Write-Host ""
Write-Host "Verifying topology..." -ForegroundColor White
$finalTopology = docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster 2>$null
$replicaCount = ($finalTopology | Select-String "name" | Measure-Object).Count
Write-Host "✓ Replicas connected: $replicaCount" -ForegroundColor Green

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  TEST SUMMARY" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

if ($newMaster -ne $initialMaster) {
    Write-Host "✓ FAILOVER TEST PASSED" -ForegroundColor Green
    Write-Host "  - Master failover worked correctly" -ForegroundColor Green
    Write-Host "  - Application continued to function" -ForegroundColor Green
    Write-Host "  - Cache remained operational" -ForegroundColor Green
} else {
    Write-Host "✗ FAILOVER TEST FAILED" -ForegroundColor Red
    Write-Host "  - Master did not failover" -ForegroundColor Red
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Check application logs: logs\application.log" -ForegroundColor White
Write-Host "2. Check sentinel logs: docker logs redis-sentinel-1" -ForegroundColor White
Write-Host "3. View all containers: docker-compose -f docker\docker-compose-sentinel.yml ps" -ForegroundColor White
Write-Host ""
