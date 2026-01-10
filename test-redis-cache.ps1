# Redis Cache Testing Script
# Tests cache HIT/MISS behavior for Calculator API

Write-Host "=== Redis Cache Testing ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: First call - Cache MISS
Write-Host "Test 1: First call to add(10, 5) - Expected: Cache MISS" -ForegroundColor Yellow
$response1 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5" -Method GET
Write-Host "Result:" ($response1 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 2: Second call - Cache HIT
Write-Host "`nTest 2: Second call to add(10, 5) - Expected: Cache HIT (faster)" -ForegroundColor Yellow
$response2 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5" -Method GET
Write-Host "Result:" ($response2 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 3: Different parameters - Cache MISS
Write-Host "`nTest 3: Different parameters add(20, 30) - Expected: Cache MISS" -ForegroundColor Yellow
$response3 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=20&b=30" -Method GET
Write-Host "Result:" ($response3 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 4: Multiply operation - Cache MISS
Write-Host "`nTest 4: Multiply(5, 6) - Expected: Cache MISS" -ForegroundColor Yellow
$response4 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=5&b=6" -Method GET
Write-Host "Result:" ($response4 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 5: Multiply again - Cache HIT
Write-Host "`nTest 5: Multiply(5, 6) again - Expected: Cache HIT" -ForegroundColor Yellow
$response5 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=5&b=6" -Method GET
Write-Host "Result:" ($response5 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 6: Get Cache Info
Write-Host "`nTest 6: Get cache information" -ForegroundColor Yellow
$cacheInfo = Invoke-RestMethod -Uri "http://localhost:8080/api/cache/info" -Method GET
Write-Host "Cache Info:" ($cacheInfo | ConvertTo-Json) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 7: Divide operation - Cache MISS
Write-Host "`nTest 7: Divide(100, 5) - Expected: Cache MISS" -ForegroundColor Yellow
$response7 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/divide?a=100&b=5" -Method GET
Write-Host "Result:" ($response7 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 8: Divide again - Cache HIT
Write-Host "`nTest 8: Divide(100, 5) again - Expected: Cache HIT" -ForegroundColor Yellow
$response8 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/divide?a=100&b=5" -Method GET
Write-Host "Result:" ($response8 | ConvertTo-Json -Compress) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 9: Clear specific cache
Write-Host "`nTest 9: Clear operations cache" -ForegroundColor Yellow
$clearResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/cache/clear/operations" -Method DELETE
Write-Host "Clear Result:" ($clearResponse | ConvertTo-Json) -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 10: After cache clear - Cache MISS again
Write-Host "`nTest 10: add(10, 5) after cache clear - Expected: Cache MISS" -ForegroundColor Yellow
$response10 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5" -Method GET
Write-Host "Result:" ($response10 | ConvertTo-Json -Compress) -ForegroundColor Green

Write-Host "`n=== Testing Complete ===" -ForegroundColor Cyan
Write-Host "Check application logs to see 'Cache MISS - Computing' messages" -ForegroundColor Magenta
Write-Host "Cache HITs will NOT show the 'Computing' message" -ForegroundColor Magenta
