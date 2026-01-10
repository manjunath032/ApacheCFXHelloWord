# Swagger UI with SOAP Service Documentation

## 🎯 Quick Access URLs

Once the application is running (`mvn spring-boot:run`), access these URLs:

### 📘 Swagger UI (Interactive Documentation)
```
http://localhost:8080/swagger-ui.html
```
or
```
http://localhost:8080/swagger-ui/index.html
```

### 📄 OpenAPI JSON Specification
```
http://localhost:8080/v3/api-docs
```

### 🔷 SOAP WSDL (Primary Service)
```
http://localhost:8080/services/Calculator?wsdl
```

---

## 🔍 What's Available in Swagger UI

### 1. **Calculator API** (REST Wrapper)
REST endpoints that wrap the SOAP service operations:
- `GET /api/calculator/add` - Add two numbers
- `GET /api/calculator/subtract` - Subtract two numbers
- `GET /api/calculator/multiply` - Multiply two numbers
- `GET /api/calculator/divide` - Divide two numbers
- `POST /api/calculator/calculate` - Dynamic calculation with metadata
- `GET /api/calculator/health` - Service health check

### 2. **SOAP Service Info** (NEW!)
REST endpoints that document the SOAP interface:
- `GET /api/soap/services` - Complete SOAP service documentation with all operations
- `GET /api/soap/calculator/wsdl-url` - Direct WSDL URL link
- `GET /api/soap/calculator/operations` - Detailed operation list with examples
- `GET /api/soap/calculator/test-requests` - Sample SOAP envelope requests

---

## 📋 SOAP Service Information Available

The `/api/soap/services` endpoint provides comprehensive SOAP documentation:

### Service Details
```json
{
  "calculatorService": {
    "serviceName": "CalculatorService",
    "namespace": "http://example.com/calculator",
    "wsdlUrl": "http://localhost:8080/services/Calculator?wsdl",
    "soapEndpoint": "http://localhost:8080/services/Calculator",
    "binding": "Document/Literal",
    "operations": [...]
  }
}
```

### Operations Documented
1. **add** - Add two integers
   - Parameters: `a` (int), `b` (int)
   - Returns: `int`

2. **subtract** - Subtract two integers
   - Parameters: `a` (int), `b` (int)
   - Returns: `int`

3. **multiply** - Multiply two integers
   - Parameters: `a` (int), `b` (int)
   - Returns: `int`

4. **divide** - Divide two integers
   - Parameters: `a` (int), `b` (int)
   - Returns: `double`

5. **calculate** - Dynamic calculation with ResponseDetails
   - Parameters: `operand1` (int), `operand2` (int), `operation` (string INOUT)
   - Returns: `result` (double OUT), `responseDetails` (ResponseDetails OUT)

### SOAP Headers
- **ResponseDetails** (Output Header)
  - `timestamp` - Processing timestamp (dateTime)
  - `serverId` - Server identifier (string)
  - `processingTime` - Processing time in milliseconds (long)

### SOAP Faults
- **ServiceFailoverFault**
  - `faultCode` - Error code
  - `faultMessage` - Error description
  - `failoverNode` - Failover node information
  - `retryAfter` - Retry delay in seconds

---

## 🚀 How to Test

### Option 1: Test REST API in Swagger UI
1. Start application: `mvn spring-boot:run`
2. Open browser: http://localhost:8080/swagger-ui.html
3. Click on "Calculator API" section
4. Try any operation (e.g., "GET /api/calculator/add")
5. Click "Try it out", enter parameters, click "Execute"

### Option 2: Get SOAP Information
1. Open Swagger UI: http://localhost:8080/swagger-ui.html
2. Click on "SOAP Service Info" section
3. Try "GET /api/soap/services" to see complete SOAP documentation
4. Try "GET /api/soap/calculator/test-requests" for SOAP envelope examples

### Option 3: Test SOAP Directly
1. Get WSDL URL from Swagger: `/api/soap/calculator/wsdl-url`
2. Copy WSDL URL: http://localhost:8080/services/Calculator?wsdl
3. Import into SoapUI or Postman
4. Or use PowerShell with test-requests/*.xml files

---

## 📝 Sample REST API Calls via Swagger

### Example 1: Add Operation
```
GET /api/calculator/add?a=10&b=5

Response:
{
  "operation": "ADD",
  "operand1": 10,
  "operand2": 5,
  "result": 15
}
```

### Example 2: Calculate with Metadata
```
POST /api/calculator/calculate
Body: {
  "operand1": 15,
  "operand2": 10,
  "operation": "ADD"
}

Response:
{
  "operand1": 15,
  "operand2": 10,
  "operation": "ADD",
  "result": 25.0,
  "responseDetails": {
    "timestamp": "2026-01-10T12:00:00.123+05:30",
    "serverId": "HOSTNAME-54",
    "processingTime": 2
  }
}
```

---

## 🔧 Sample SOAP Requests (from Swagger)

Get these from `/api/soap/calculator/test-requests`:

### Add Operation
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" 
               xmlns:cal="http://example.com/calculator">
   <soap:Body>
      <cal:add>
         <cal:a>10</cal:a>
         <cal:b>5</cal:b>
      </cal:add>
   </soap:Body>
</soap:Envelope>
```

### Calculate Operation
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" 
               xmlns:cal="http://example.com/calculator">
   <soap:Body>
      <cal:calculate>
         <cal:operand1>15</cal:operand1>
         <cal:operand2>10</cal:operand2>
         <cal:operation>ADD</cal:operation>
      </cal:calculate>
   </soap:Body>
</soap:Envelope>
```

---

## 🎨 Swagger UI Features

### Interactive Testing
- ✅ Try out REST endpoints directly in browser
- ✅ See request/response schemas
- ✅ View example values
- ✅ Get curl commands for each request

### SOAP Documentation
- ✅ Complete SOAP service metadata
- ✅ WSDL URL with direct link
- ✅ All operations with parameters
- ✅ Sample SOAP envelopes
- ✅ SOAP headers and faults documentation

### API Information
- ✅ Service description
- ✅ Contact information
- ✅ License details
- ✅ Server URLs
- ✅ OpenAPI 3.0 specification

---

## 📚 Additional Resources

### Swagger/OpenAPI
- Swagger UI: Interactive API documentation
- OpenAPI 3.0: Industry-standard API specification
- SpringDoc: Spring Boot integration for OpenAPI

### SOAP/WSDL
- WSDL: Web Services Description Language (XML-based)
- SOAP: Simple Object Access Protocol
- Apache CXF: JAX-WS implementation

### Testing Tools
- **SoapUI**: Full-featured SOAP testing tool
- **Postman**: Supports both REST and SOAP
- **Swagger UI**: Built-in REST API testing
- **PowerShell**: Command-line SOAP testing

---

## 🏆 Best Practices

### For REST API Consumers
1. Use Swagger UI for interactive exploration
2. Try operations before implementing clients
3. Check response schemas for data structures
4. Use the health endpoint for monitoring

### For SOAP API Consumers
1. Download WSDL from the link in Swagger
2. Import WSDL into your SOAP client (SoapUI/Postman)
3. Use sample requests from `/api/soap/calculator/test-requests`
4. Reference complete operation list from `/api/soap/services`

### For API Documentation
1. Swagger provides REST API documentation
2. Use `/api/soap/*` endpoints to document SOAP services
3. WSDL remains the authoritative SOAP specification
4. Keep both REST and SOAP documentation in sync

---

## 💡 Why Both REST and SOAP?

### REST API
- ✅ Easy to test in browser/Swagger
- ✅ JSON format (human-readable)
- ✅ HTTP status codes
- ✅ Modern API style

### SOAP API
- ✅ Contract-first (WSDL)
- ✅ Strong typing
- ✅ Enterprise standards
- ✅ Complex operations (Holders, Headers, Faults)
- ✅ Generated code from WSDL

**Both APIs call the same underlying service implementation!**

---

## 🎯 Summary

✅ **Swagger UI** documents the REST API  
✅ **/api/soap/*** endpoints document the SOAP interface in REST format  
✅ **WSDL** provides the authoritative SOAP contract  
✅ Both protocols available simultaneously  
✅ Choose REST for simplicity, SOAP for enterprise features  

**Start exploring:** http://localhost:8080/swagger-ui.html 🚀
