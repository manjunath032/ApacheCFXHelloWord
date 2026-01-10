# Test Cache Hit/Miss Logging
Write-Host "=== Testing Redis Cache Hit/Miss Logging ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: First multiply request (Cache MISS expected)
Write-Host "Test 1: First request 10 * 5 (Cache MISS expected)" -ForegroundColor Yellow
$result1 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=10&b=5" -Method Get
Write-Host "Result: $result1" -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 2: Second multiply request (Cache HIT expected)
Write-Host "`nTest 2: Second request 10 * 5 (Cache HIT expected)" -ForegroundColor Yellow
$result2 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=10&b=5" -Method Get
Write-Host "Result: $result2" -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 3: Third multiply request (Cache HIT expected)
Write-Host "`nTest 3: Third request 10 * 5 (Cache HIT expected)" -ForegroundColor Yellow
$result3 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=10&b=5" -Method Get
Write-Host "Result: $result3" -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 4: Different multiply request (Cache MISS expected)
Write-Host "`nTest 4: New request 20 * 3 (Cache MISS expected)" -ForegroundColor Yellow
$result4 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=20&b=3" -Method Get
Write-Host "Result: $result4" -ForegroundColor Green
Start-Sleep -Seconds 2

# Test 5: Repeat new request (Cache HIT expected)
Write-Host "`nTest 5: Repeat request 20 * 3 (Cache HIT expected)" -ForegroundColor Yellow
$result5 = Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/multiply?a=20&b=3" -Method Get
Write-Host "Result: $result5" -ForegroundColor Green

Write-Host "`n=== Check the application logs for Cache HIT/MISS messages ===" -ForegroundColor Cyan
Write-Host "Look for lines containing 'Cache HIT' or 'Cache MISS'" -ForegroundColor Cyan
