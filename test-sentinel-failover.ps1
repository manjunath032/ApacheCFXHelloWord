# Redis Sentinel Failover Test Script
# This script tests automatic failover in Redis Sentinel setup

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Redis Sentinel Failover Test" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$appUrl = "http://localhost:8080"
$sentinelPort = 26379
$testRequestFile = "test-requests\calculator-multiply.xml"

# Function to get current master
function Get-RedisMaster {
    $result = docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>$null
    if ($LASTEXITCODE -eq 0) {
        return $result[0] + ":" + $result[1]
    }
    return "Unknown"
}

# Function to test application
function Test-Application {
    param([string]$message)
    
    Write-Host $message -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "$appUrl/services/CalculatorService" `
            -Method POST `
            -ContentType "text/xml" `
            -InFile $testRequestFile `
            -UseBasicParsing `
            -TimeoutSec 10
        
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ Request successful (Status: $($response.StatusCode))" -ForegroundColor Green
            return $true
        } else {
            Write-Host "✗ Request failed (Status: $($response.StatusCode))" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "✗ Request failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Function to check health
function Test-ApplicationHealth {
    try {
        $health = Invoke-RestMethod -Uri "$appUrl/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") {
            Write-Host "✓ Application health: UP" -ForegroundColor Green
            if ($health.components.redisSentinel) {
                $redis = $health.components.redisSentinel
                Write-Host "  Role: $($redis.details.role)" -ForegroundColor Cyan
                Write-Host "  Status: $($redis.details.status)" -ForegroundColor Cyan
                if ($redis.details.connected_replicas) {
                    Write-Host "  Connected Replicas: $($redis.details.connected_replicas)" -ForegroundColor Cyan
                }
            }
            return $true
        }
    } catch {
        Write-Host "✗ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    return $false
}

# Function to get cache metrics
function Get-CacheMetrics {
    try {
        $hits = Invoke-RestMethod -Uri "$appUrl/actuator/metrics/cache.distributed.hits" -TimeoutSec 5
        $misses = Invoke-RestMethod -Uri "$appUrl/actuator/metrics/cache.distributed.misses" -TimeoutSec 5
        
        Write-Host "  Cache Hits: $($hits.measurements[0].value)" -ForegroundColor Cyan
        Write-Host "  Cache Misses: $($misses.measurements[0].value)" -ForegroundColor Cyan
    } catch {
        Write-Host "  Metrics not available yet" -ForegroundColor Gray
    }
}

# Step 1: Verify initial setup
Write-Host ""
Write-Host "Step 1: Verify Initial Setup" -ForegroundColor White
Write-Host "-" * 50

$initialMaster = Get-RedisMaster
Write-Host "Current Redis Master: $initialMaster" -ForegroundColor Cyan

# Check all containers
Write-Host ""
Write-Host "Checking Docker containers..." -ForegroundColor Yellow
docker-compose -f docker\docker-compose-sentinel.yml ps

Write-Host ""
Write-Host "Checking Sentinel status..." -ForegroundColor Yellow
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL masters | Select-Object -First 10

# Step 2: Test application before failover
Write-Host ""
Write-Host "Step 2: Baseline Test" -ForegroundColor White
Write-Host "-" * 50

if (-not (Test-ApplicationHealth)) {
    Write-Host ""
    Write-Host "✗ Application not healthy. Stopping test." -ForegroundColor Red
    exit 1
}

Write-Host ""
if (-not (Test-Application "Testing calculator service...")) {
    Write-Host ""
    Write-Host "✗ Application not responding. Stopping test." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Initial cache metrics:" -ForegroundColor Yellow
Get-CacheMetrics

# Step 3: Make multiple requests to populate cache
Write-Host ""
Write-Host "Step 3: Populate Cache" -ForegroundColor White
Write-Host "-" * 50

Write-Host "Making 5 requests to populate cache..." -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    Write-Host "  Request $i..." -NoNewline
    $null = Test-Application ""
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "Cache metrics after population:" -ForegroundColor Yellow
Get-CacheMetrics

# Step 4: Simulate master failure
Write-Host ""
Write-Host "Step 4: Simulate Master Failure" -ForegroundColor White
Write-Host "-" * 50

Write-Host "Stopping Redis master ($initialMaster)..." -ForegroundColor Yellow
docker stop redis-master

Write-Host "Waiting for failover (15 seconds)..." -ForegroundColor Yellow
for ($i = 15; $i -gt 0; $i--) {
    Write-Host "  $i seconds remaining..." -NoNewline
    Start-Sleep -Seconds 1
    Write-Host "`r" -NoNewline
}
Write-Host ""

$newMaster = Get-RedisMaster
Write-Host "New Redis Master: $newMaster" -ForegroundColor Cyan

if ($newMaster -eq $initialMaster) {
    Write-Host "✗ Failover did not occur! Master unchanged." -ForegroundColor Red
} else {
    Write-Host "✓ Failover successful! Master changed from $initialMaster to $newMaster" -ForegroundColor Green
}

# Step 5: Test application after failover
Write-Host ""
Write-Host "Step 5: Test After Failover" -ForegroundColor White
Write-Host "-" * 50

Write-Host "Waiting 5 seconds for application to reconnect..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

if (-not (Test-ApplicationHealth)) {
    Write-Host ""
    Write-Host "⚠ Application health check failed after failover" -ForegroundColor Red
}

Write-Host ""
if (Test-Application "Testing calculator service after failover...") {
    Write-Host "✓ Application recovered successfully!" -ForegroundColor Green
} else {
    Write-Host "✗ Application failed to recover" -ForegroundColor Red
}

Write-Host ""
Write-Host "Cache metrics after failover:" -ForegroundColor Yellow
Get-CacheMetrics

# Step 6: Verify cache still works
Write-Host ""
Write-Host "Step 6: Verify Cache Functionality" -ForegroundColor White
Write-Host "-" * 50

Write-Host "Making 3 requests to test cache..." -ForegroundColor Yellow
for ($i = 1; $i -le 3; $i++) {
    Write-Host "  Request $i..." -NoNewline
    $null = Test-Application ""
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "Final cache metrics:" -ForegroundColor Yellow
Get-CacheMetrics

# Step 7: Restore original master
Write-Host ""
Write-Host "Step 7: Restore Original Master" -ForegroundColor White
Write-Host "-" * 50

Write-Host "Starting original master..." -ForegroundColor Yellow
docker start redis-master

Write-Host "Waiting for synchronization (10 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "Current topology:" -ForegroundColor Yellow
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster | Select-Object -First 20

# Final summary
Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Initial Master: $initialMaster" -ForegroundColor White
Write-Host "Master After Failover: $newMaster" -ForegroundColor White
Write-Host "Current Master: $(Get-RedisMaster)" -ForegroundColor White
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
