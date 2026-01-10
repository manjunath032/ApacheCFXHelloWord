# Transaction ID Tracking

This document explains how correlation/transaction IDs work in this application for end-to-end request tracking.

## Overview

The application implements **MDC (Mapped Diagnostic Context)** based transaction tracking that automatically includes a unique transaction ID in all log entries for each request.

## How It Works

### 1. REST Requests (HTTP)
- **Header**: `X-Transaction-ID`
- If provided, the same ID is used throughout the request
- If not provided, a new UUID is generated
- The transaction ID is added to the response header for downstream services

### 2. SOAP Requests
- **SOAP Header**: `<TransactionId>` in namespace `http://example.com/headers`
- If provided in SOAP header, the same ID is used
- If not provided, a new UUID is generated
- Transaction ID is tracked through all internal calls

### 3. Automatic Logging
All log entries automatically include the transaction ID in the format:
```
[transactionId] 
```

## Example Log Output

```log
2026-01-10 12:42:06.830 [http-nio-8080-exec-1] [a3f2e1c0-b234-4567-89ab-cdef01234567] DEBUG c.e.c.c.CalculatorRestController - Entering REST calculate()
2026-01-10 12:42:06.831 [http-nio-8080-exec-1] [a3f2e1c0-b234-4567-89ab-cdef01234567] DEBUG c.e.c.s.impl.CalculatorServiceImpl - Entering calculate() method
2026-01-10 12:42:06.836 [http-nio-8080-exec-1] [a3f2e1c0-b234-4567-89ab-cdef01234567] INFO  c.e.c.s.impl.CalculatorServiceImpl - Calculate operation: 15 + 10 = 25.0
2026-01-10 12:42:06.837 [http-nio-8080-exec-1] [a3f2e1c0-b234-4567-89ab-cdef01234567] DEBUG c.e.c.c.CalculatorRestController - Exiting REST calculate()
```

## Testing with Transaction IDs

### REST API with curl
```bash
# Without transaction ID (will be auto-generated)
curl http://localhost:8080/api/calculator/add?a=10&b=5

# With transaction ID from upstream service
curl -H "X-Transaction-ID: my-custom-id-12345" \
     http://localhost:8080/api/calculator/add?a=10&b=5
```

### REST API with PowerShell
```powershell
# Without transaction ID
Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5"

# With transaction ID
$headers = @{ "X-Transaction-ID" = "my-custom-id-12345" }
Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5" -Headers $headers
```

### SOAP Request with Transaction ID

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" 
               xmlns:cal="http://example.com/calculator"
               xmlns:hdr="http://example.com/headers">
   <soap:Header>
      <hdr:TransactionId>my-custom-transaction-id</hdr:TransactionId>
   </soap:Header>
   <soap:Body>
      <cal:add>
         <cal:a>10</cal:a>
         <cal:b>5</cal:b>
      </cal:add>
   </soap:Body>
</soap:Envelope>
```

## Implementation Details

### Components

1. **TransactionIdFilter** (`com.example.cxf.filter`)
   - Servlet filter for REST requests
   - Extracts or generates transaction ID
   - Adds to MDC and response headers
   - Order: 1 (executes first)

2. **SoapTransactionIdInterceptor** (`com.example.cxf.interceptor`)
   - CXF interceptor for SOAP requests
   - Extracts transaction ID from SOAP headers
   - Adds to MDC for logging
   - Phase: PRE_PROTOCOL

3. **MDC Configuration** (`application.yml`)
   - Log pattern includes `%X{transactionId}`
   - Automatic inclusion in all log entries
   - Works for both console and file logging

### Benefits

✅ **End-to-End Tracing**: Track a request across multiple services
✅ **Debugging**: Quickly find all logs related to a specific request
✅ **Performance Analysis**: Measure request duration by filtering on transaction ID
✅ **Error Investigation**: Trace error context through the entire request flow
✅ **Distributed Systems**: Correlate logs across microservices

## Best Practices

1. **Upstream Services**: Always pass `X-Transaction-ID` header when calling this service
2. **Downstream Calls**: Include the transaction ID when calling other services
3. **Log Aggregation**: Use transaction ID for filtering in log management tools (ELK, Splunk, etc.)
4. **Monitoring**: Create dashboards based on transaction ID patterns
5. **Testing**: Use meaningful transaction IDs in tests for easy identification

## Configuration

The transaction ID key in MDC is: `transactionId`

You can customize this in:
- `TransactionIdFilter.TRANSACTION_ID_MDC_KEY`
- `SoapTransactionIdInterceptor.TRANSACTION_ID_MDC_KEY`
- `application.yml` log pattern: `%X{transactionId}`

## Thread Safety

MDC is thread-local, so each request thread has its own transaction ID. The filter automatically clears MDC after each request to prevent memory leaks in thread pools.
