# SOAP Calculate Test Suite with Error Handling

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SOAP Calculate Method - Test Suite" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: ADD Operation (Success)
Write-Host "[TEST 1] ADD Operation (15 + 10)" -ForegroundColor Yellow
$body1 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-ADD-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>15</cal:operand1>
         <cal:operand2>10</cal:operand2>
         <cal:operation>ADD</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response1 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body1 -UseBasicParsing -ErrorAction Stop
    Write-Host "✅ Success - Result:" -ForegroundColor Green
    Write-Host $response1.Content -ForegroundColor Green
} catch {
    Write-Host "❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Test 2: SUBTRACT Operation (Success)
Write-Host "`n[TEST 2] SUBTRACT Operation (100 - 50)" -ForegroundColor Yellow
$body2 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-SUBTRACT-002</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>100</cal:operand1>
         <cal:operand2>50</cal:operand2>
         <cal:operation>SUBTRACT</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response2 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body2 -UseBasicParsing -ErrorAction Stop
    Write-Host "✅ Success - Result:" -ForegroundColor Green
    Write-Host $response2.Content -ForegroundColor Green
} catch {
    Write-Host "❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Test 3: MULTIPLY Operation (Success)
Write-Host "`n[TEST 3] MULTIPLY Operation (8 × 7)" -ForegroundColor Yellow
$body3 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-MULTIPLY-003</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>8</cal:operand1>
         <cal:operand2>7</cal:operand2>
         <cal:operation>MULTIPLY</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response3 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body3 -UseBasicParsing -ErrorAction Stop
    Write-Host "✅ Success - Result:" -ForegroundColor Green
    Write-Host $response3.Content -ForegroundColor Green
} catch {
    Write-Host "❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Test 4: DIVIDE Operation (Success)
Write-Host "`n[TEST 4] DIVIDE Operation (100 ÷ 4)" -ForegroundColor Yellow
$body4 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-DIVIDE-004</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>100</cal:operand1>
         <cal:operand2>4</cal:operand2>
         <cal:operation>DIVIDE</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response4 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body4 -UseBasicParsing -ErrorAction Stop
    Write-Host "✅ Success - Result:" -ForegroundColor Green
    Write-Host $response4.Content -ForegroundColor Green
} catch {
    Write-Host "❌ Error:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Test 5: DIVIDE by ZERO (Expected Error)
Write-Host "`n[TEST 5] DIVIDE by ZERO - Error Test (15 ÷ 0)" -ForegroundColor Yellow
$body5 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-ERROR-DIV-ZERO</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>15</cal:operand1>
         <cal:operand2>0</cal:operand2>
         <cal:operation>DIVIDE</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response5 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body5 -UseBasicParsing -ErrorAction Stop
    Write-Host "❌ Unexpected Success - Should have failed!" -ForegroundColor Red
    Write-Host $response5.Content -ForegroundColor Red
} catch {
    Write-Host "✅ Expected SOAP Fault Received:" -ForegroundColor Green
    # Extract SOAP Fault from error
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $soapFault = $reader.ReadToEnd()
        $reader.Close()
        Write-Host $soapFault -ForegroundColor Yellow
    } else {
        Write-Host $_.Exception.Message -ForegroundColor Yellow
    }
}

Start-Sleep -Seconds 1

# Test 6: Invalid Operation (Expected Error)
Write-Host "`n[TEST 6] Invalid Operation - Error Test (10 MODULO 3)" -ForegroundColor Yellow
$body6 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-ERROR-INVALID-OP</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>10</cal:operand1>
         <cal:operand2>3</cal:operand2>
         <cal:operation>MODULO</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

try {
    $response6 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body6 -UseBasicParsing -ErrorAction Stop
    Write-Host "❌ Unexpected Success - Should have failed!" -ForegroundColor Red
    Write-Host $response6.Content -ForegroundColor Red
} catch {
    Write-Host "✅ Expected SOAP Fault Received:" -ForegroundColor Green
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $soapFault = $reader.ReadToEnd()
        $reader.Close()
        Write-Host $soapFault -ForegroundColor Yellow
    } else {
        Write-Host $_.Exception.Message -ForegroundColor Yellow
    }
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Suite Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nExpected Results:" -ForegroundColor White
Write-Host "  ✅ Tests 1-4: Success (ADD, SUBTRACT, MULTIPLY, DIVIDE)" -ForegroundColor Green
Write-Host "  ✅ Test 5: SOAP Fault (Division by zero)" -ForegroundColor Green
Write-Host "  ✅ Test 6: SOAP Fault (Invalid operation)" -ForegroundColor Green

Write-Host "`nCheck Error Codes in Logs:" -ForegroundColor Yellow
Write-Host "  Get-Content logs/application.log -Tail 50" -ForegroundColor White
Write-Host "`nFilter for Error Codes:" -ForegroundColor Yellow
Write-Host "  Get-Content logs/application.log | Select-String '\[0[0-2]\]'" -ForegroundColor White
Write-Host "`nSearch Specific Transaction:" -ForegroundColor Yellow
Write-Host "  Get-Content logs/application.log | Select-String 'CALC-ERROR-DIV-ZERO'" -ForegroundColor White

Write-Host "`nExpected Error Code Logs:" -ForegroundColor Yellow
Write-Host "  [CALC-ADD-001] [00] INFO  - [00:INFO] Calculate operation: 15 + 10 = 25.0" -ForegroundColor White
Write-Host "  [CALC-SUBTRACT-002] [00] INFO  - [00:INFO] Calculate operation: 100 - 50 = 50.0" -ForegroundColor White
Write-Host "  [CALC-MULTIPLY-003] [00] INFO  - [00:INFO] Calculate operation: 8 * 7 = 56.0" -ForegroundColor White
Write-Host "  [CALC-DIVIDE-004] [00] INFO  - [00:INFO] Calculate operation: 100 / 4 = 25.0" -ForegroundColor White
Write-Host "  [CALC-ERROR-DIV-ZERO] [01] WARN  - [01:WARNING] Division by zero attempted: 15 / 0" -ForegroundColor White
Write-Host "  [CALC-ERROR-INVALID-OP] [02] WARN  - [02:WARNING] Invalid operation attempted: MODULO" -ForegroundColor White
