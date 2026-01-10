# Simple Redis Sentinel Test Script
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Redis Sentinel Quick Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check containers
Write-Host "[1/7] Checking Docker containers..." -ForegroundColor Yellow
$containers = docker ps --format "{{.Names}}" | Where-Object { $_ -match "redis" }
Write-Host "Running containers: $($containers.Count)" -ForegroundColor Green
$containers | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }

# Test 2: Check Sentinel master
Write-Host ""
Write-Host "[2/7] Checking Sentinel configuration..." -ForegroundColor Yellow
$masterInfo = docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>$null
if ($masterInfo) {
    $initialMaster = "$($masterInfo[0]):$($masterInfo[1])"
    Write-Host "Current master: $initialMaster" -ForegroundColor Green
} else {
    Write-Host "ERROR: Cannot get master info" -ForegroundColor Red
    exit 1
}

# Test 3: Test application health
Write-Host ""
Write-Host "[3/7] Testing application health..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -ErrorAction Stop
    Write-Host "Application status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Application not responding" -ForegroundColor Red
    exit 1
}

# Test 4: Test SOAP request (populate cache)
Write-Host ""
Write-Host "[4/7] Testing SOAP request..." -ForegroundColor Yellow
$soapRequest = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:cal="http://example.com/generated/calculator">
   <soapenv:Body>
      <cal:multiply>
         <cal:a>10</cal:a>
         <cal:b>5</cal:b>
      </cal:multiply>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" -Method POST -ContentType "text/xml" -Body $soapRequest -ErrorAction Stop
    Write-Host "First request: SUCCESS (Status $($response.StatusCode))" -ForegroundColor Green
    
    Start-Sleep -Seconds 1
    
    $response2 = Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" -Method POST -ContentType "text/xml" -Body $soapRequest -ErrorAction Stop
    Write-Host "Second request: SUCCESS (Status $($response2.StatusCode)) - Should be cached" -ForegroundColor Green
} catch {
    Write-Host "ERROR: SOAP request failed - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Simulate master failure
Write-Host ""
Write-Host "[5/7] Simulating master failure..." -ForegroundColor Yellow
Write-Host "Stopping redis-master container..." -ForegroundColor White
docker stop redis-master >$null 2>&1
Write-Host "Master stopped" -ForegroundColor Green

Write-Host "Waiting for failover (15 seconds)..." -ForegroundColor White
for ($i = 15; $i -gt 0; $i--) {
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 1
}
Write-Host ""

# Test 6: Check new master
Write-Host ""
Write-Host "[6/7] Checking failover result..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
$newMasterInfo = docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>$null
if ($newMasterInfo) {
    $newMaster = "$($newMasterInfo[0]):$($newMasterInfo[1])"
    Write-Host "New master: $newMaster" -ForegroundColor Green
    
    if ($newMaster -ne $initialMaster) {
        Write-Host "FAILOVER SUCCESSFUL!" -ForegroundColor Green -BackgroundColor DarkGreen
    } else {
        Write-Host "FAILOVER FAILED - Master unchanged" -ForegroundColor Red
    }
} else {
    Write-Host "ERROR: Cannot get new master info" -ForegroundColor Red
}

# Test after failover
Write-Host ""
Write-Host "Testing application after failover..." -ForegroundColor White
Start-Sleep -Seconds 5
try {
    $response3 = Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" -Method POST -ContentType "text/xml" -Body $soapRequest -ErrorAction Stop
    Write-Host "Request after failover: SUCCESS (Status $($response3.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "WARNING: Request failed after failover - $($_.Exception.Message)" -ForegroundColor Yellow
}

# Test 7: Restore original master
Write-Host ""
Write-Host "[7/7] Restoring original master..." -ForegroundColor Yellow
docker start redis-master >$null 2>&1
Write-Host "Original master restarted (now a replica)" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "- Initial master: $initialMaster" -ForegroundColor White
Write-Host "- New master: $newMaster" -ForegroundColor White
Write-Host "- Failover: $(if ($newMaster -ne $initialMaster) { 'SUCCESS' } else { 'FAILED' })" -ForegroundColor $(if ($newMaster -ne $initialMaster) { 'Green' } else { 'Red' })
Write-Host ""
Write-Host "Check logs:" -ForegroundColor Yellow
Write-Host "  Application: logs\application.log" -ForegroundColor Gray
Write-Host "  Sentinel: docker logs redis-sentinel-1" -ForegroundColor Gray
Write-Host ""
