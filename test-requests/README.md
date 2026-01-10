# Testing SOAP Services

Sample SOAP request files for testing the services.

## Running the Application

```bash
mvn spring-boot:run
```

Or:

```bash
java -jar target/ApacheCFXHelloWord.jar
```

## Available Services

### 1. HelloWorld Service
- **WSDL**: http://localhost:8080/services/HelloWorld?wsdl
- **Endpoint**: http://localhost:8080/services/HelloWorld

### 2. Calculator Service
- **WSDL**: http://localhost:8080/services/Calculator?wsdl
- **Endpoint**: http://localhost:8080/services/Calculator

### 3. Service List
- **Info Page**: http://localhost:8080/services/info

## Testing with curl (PowerShell)

### Calculator - Add
```powershell
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -d "@test-requests/calculator-add.xml"
```

### Calculator - Subtract
```powershell
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -d "@test-requests/calculator-subtract.xml"
```

### Calculator - Multiply
```powershell
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -d "@test-requests/calculator-multiply.xml"
```

### Calculator - Divide
```powershell
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -d "@test-requests/calculator-divide.xml"
```

### HelloWorld - Say Hello
```powershell
curl -X POST http://localhost:8080/services/HelloWorld `
  -H "Content-Type: text/xml" `
  -d "@test-requests/helloworld-sayhello.xml"
```

## Testing with Invoke-WebRequest (PowerShell)

```powershell
$headers = @{
    "Content-Type" = "text/xml"
}

$body = Get-Content -Path "test-requests/calculator-add.xml" -Raw

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/services/Calculator" `
    -Method POST `
    -Headers $headers `
    -Body $body

$response.Content
```

## Expected Responses

### Add (10 + 5 = 15)
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:addResponse xmlns:ns2="http://example.com/calculator">
         <result>15</result>
      </ns2:addResponse>
   </soap:Body>
</soap:Envelope>
```

### Subtract (20 - 8 = 12)
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:subtractResponse xmlns:ns2="http://example.com/calculator">
         <result>12</result>
      </ns2:subtractResponse>
   </soap:Body>
</soap:Envelope>
```

### Multiply (7 * 6 = 42)
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:multiplyResponse xmlns:ns2="http://example.com/calculator">
         <result>42</result>
      </ns2:multiplyResponse>
   </soap:Body>
</soap:Envelope>
```

### Divide (100 / 4 = 25.0)
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:divideResponse xmlns:ns2="http://example.com/calculator">
         <result>25.0</result>
      </ns2:divideResponse>
   </soap:Body>
</soap:Envelope>
```

## Testing with SoapUI

1. Download [SoapUI](https://www.soapui.org/downloads/soapui/)
2. Create New SOAP Project
3. Enter WSDL URL: `http://localhost:8080/services/Calculator?wsdl`
4. SoapUI generates sample requests for all operations
5. Execute and test

## Testing with Postman

1. Create new HTTP POST request
2. Set URL to service endpoint (e.g., `http://localhost:8080/services/Calculator`)
3. Set Headers:
   - `Content-Type: text/xml`
4. Set Body to raw XML (paste content from test-requests/*.xml files)
5. Send request

## Viewing WSDL

Open in browser:
- Calculator: http://localhost:8080/services/Calculator?wsdl
- HelloWorld: http://localhost:8080/services/HelloWorld?wsdl

## Service Information

View all available services:
http://localhost:8080/services/info
