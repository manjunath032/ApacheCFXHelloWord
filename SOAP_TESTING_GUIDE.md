# SOAP Testing Guide - Calculator Service

Complete guide for testing all SOAP methods with and without transaction ID tracking.

## 🚀 Quick Start

### Start the Application
```powershell
mvn spring-boot:run
```
Or use the packaged JAR:
```powershell
java -jar target/ApacheCFXHelloWord.jar
```

### Service Information
- **WSDL**: http://localhost:8080/services/Calculator?wsdl
- **Endpoint**: http://localhost:8080/services/Calculator
- **Service Info**: http://localhost:8080/services/info

---

## 📋 Test Methods

### 1. ADD Operation (10 + 5 = 15)

**Without Transaction ID:**
```powershell
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-add.xml"
```

**With Transaction ID:**
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>ADD-TEST-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:add>
         <cal:a>10</cal:a>
         <cal:b>5</cal:b>
      </cal:add>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

**Expected Response:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:addResponse xmlns:ns2="http://example.com/calculator">
         <result>15</result>
      </ns2:addResponse>
   </soap:Body>
</soap:Envelope>
```

---

### 2. SUBTRACT Operation (20 - 8 = 12)

**Without Transaction ID:**
```powershell
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-subtract.xml"
```

**With Transaction ID:**
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>SUBTRACT-TEST-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:subtract>
         <cal:a>20</cal:a>
         <cal:b>8</cal:b>
      </cal:subtract>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

**Expected Response:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:subtractResponse xmlns:ns2="http://example.com/calculator">
         <result>12</result>
      </ns2:subtractResponse>
   </soap:Body>
</soap:Envelope>
```

---

### 3. MULTIPLY Operation (6 × 7 = 42)

**Without Transaction ID:**
```powershell
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-multiply.xml"
```

**With Transaction ID:**
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>MULTIPLY-TEST-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:multiply>
         <cal:a>6</cal:a>
         <cal:b>7</cal:b>
      </cal:multiply>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

**Expected Response:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:multiplyResponse xmlns:ns2="http://example.com/calculator">
         <result>42</result>
      </ns2:multiplyResponse>
   </soap:Body>
</soap:Envelope>
```

---

### 4. DIVIDE Operation (100 ÷ 4 = 25)

**Without Transaction ID:**
```powershell
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-divide.xml"
```

**With Transaction ID:**
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>DIVIDE-TEST-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:divide>
         <cal:a>100</cal:a>
         <cal:b>4</cal:b>
      </cal:divide>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

**Expected Response:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:divideResponse xmlns:ns2="http://example.com/calculator">
         <result>25</result>
      </ns2:divideResponse>
   </soap:Body>
</soap:Envelope>
```

---

### 5. CALCULATE Operation (Generic - 25 × 5 = 125)

**Without Transaction ID:**
```powershell
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-calculate.xml"
```

**With Transaction ID:**
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALCULATE-TEST-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>25</cal:operand1>
         <cal:operand2>5</cal:operand2>
         <cal:operation>MULTIPLY</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

**Expected Response:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:calculateResponse xmlns:ns2="http://example.com/calculator">
         <result>125</result>
      </ns2:calculateResponse>
   </soap:Body>
</soap:Envelope>
```

**Valid Operations for Calculate:**
- `ADD`
- `SUBTRACT`
- `MULTIPLY`
- `DIVIDE`

---

## 🧪 Complete Test Suite Script

Copy and run this complete test script:

```powershell
# SOAP Calculator Service - Complete Test Suite
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SOAP Calculator Service Test Suite" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: Add
Write-Host "[TEST 1] ADD Operation (10 + 5)" -ForegroundColor Yellow
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-add.xml"
Start-Sleep -Seconds 1

# Test 2: Subtract
Write-Host "`n[TEST 2] SUBTRACT Operation (20 - 8)" -ForegroundColor Yellow
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-subtract.xml"
Start-Sleep -Seconds 1

# Test 3: Multiply
Write-Host "`n[TEST 3] MULTIPLY Operation (6 × 7)" -ForegroundColor Yellow
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-multiply.xml"
Start-Sleep -Seconds 1

# Test 4: Divide
Write-Host "`n[TEST 4] DIVIDE Operation (100 ÷ 4)" -ForegroundColor Yellow
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-divide.xml"
Start-Sleep -Seconds 1

# Test 5: Calculate with custom Transaction ID
Write-Host "`n[TEST 5] CALCULATE with Transaction ID (25 × 5)" -ForegroundColor Yellow
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>FULL-TEST-SUITE-001</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>25</cal:operand1>
         <cal:operand2>5</cal:operand2>
         <cal:operation>MULTIPLY</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

$response = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
Write-Host $response.Content -ForegroundColor Green

# Show logs with transaction IDs
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Recent Logs (Last 20 lines)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Get-Content logs/application.log -Tail 20

Write-Host "`n✅ Test Suite Complete!" -ForegroundColor Green
```

---

## 🔍 Verify Transaction ID Tracking

After running tests, check the logs:

```powershell
# View last 30 lines of logs
Get-Content logs/application.log -Tail 30

# Filter only lines with transaction IDs
Get-Content logs/application.log | Select-String "\[.*-.*-.*-.*-.*\]"

# Search for specific transaction ID
Get-Content logs/application.log | Select-String "FULL-TEST-SUITE-001"
```

---

## 🎯 Error Test Cases

### Division by Zero
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>ERROR-TEST-DIV-ZERO</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:divide>
         <cal:a>100</cal:a>
         <cal:b>0</cal:b>
      </cal:divide>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

### Invalid Operation
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>ERROR-TEST-INVALID-OP</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>10</cal:operand1>
         <cal:operand2>5</cal:operand2>
         <cal:operation>MODULO</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

---

## 📊 Expected Log Output

When you run tests with transaction IDs, you should see in `logs/application.log`:

```
2026-01-10 13:45:12.123 [http-nio-8080-exec-1] [ADD-TEST-001] DEBUG c.e.c.i.SoapTransactionIdInterceptor - Extracted transaction ID from SOAP header: ADD-TEST-001
2026-01-10 13:45:12.124 [http-nio-8080-exec-1] [ADD-TEST-001] DEBUG c.e.c.i.SoapTransactionIdInterceptor - SOAP request processing with transaction ID: ADD-TEST-001
2026-01-10 13:45:12.125 [http-nio-8080-exec-1] [ADD-TEST-001] DEBUG c.e.c.s.i.CalculatorServiceImpl - Entering add() method with parameters: a=10, b=5
2026-01-10 13:45:12.126 [http-nio-8080-exec-1] [ADD-TEST-001] INFO  c.e.c.s.i.CalculatorServiceImpl - Addition: 10 + 5 = 15
2026-01-10 13:45:12.127 [http-nio-8080-exec-1] [ADD-TEST-001] DEBUG c.e.c.s.i.CalculatorServiceImpl - Exiting add() method with result: 15
```

---

## 🔗 Useful Links

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **REST API Base**: http://localhost:8080/api/calculator
- **SOAP Services Info**: http://localhost:8080/services/info
- **Calculator WSDL**: http://localhost:8080/services/Calculator?wsdl

---

## 📝 Notes

1. **Transaction ID is optional** - If not provided, the system auto-generates a UUID
2. **Transaction ID format** - Use any string format (UUID, custom codes, etc.)
3. **Transaction ID namespace** - `http://example.com/headers` for SOAP header
4. **Log level** - Set to DEBUG for `com.example.cxf` to see detailed transaction tracking
5. **Thread-safe** - MDC automatically handles concurrent requests with different transaction IDs

---

## 🚨 Troubleshooting

**Issue**: No transaction ID in logs
- **Solution**: Check that log pattern includes `[%X{transactionId}]` in `application.yml`

**Issue**: Transaction ID not extracted from SOAP header
- **Solution**: Ensure namespace is `http://example.com/headers` and element name is `TransactionId`

**Issue**: 404 Not Found
- **Solution**: Ensure application is running and endpoint is `/services/Calculator` (case-sensitive)

**Issue**: Connection refused
- **Solution**: Check that application is started and listening on port 8080

---

**Happy Testing! 🎉**
