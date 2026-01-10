# Transaction ID Fix - Preventing Duplicate Generation

## 🐛 Problem Identified

When SOAP requests arrived without a transaction ID in the SOAP header, **two UUIDs were being generated**:

1. **TransactionIdFilter** (Servlet Filter) generated a UUID: `2f59617f-0179-...`
2. **SoapTransactionIdInterceptor** (CXF Interceptor) generated **another** UUID: `8a34bd42-ae20-...`

### Example of the Issue:
```log
2026-01-10 13:15:39.883 [http-nio-8080-exec-4] [2f59617f-0179-...] DEBUG TransactionIdFilter - Processing request with transaction ID: 2f59617f-0179-...
2026-01-10 13:15:39.922 [http-nio-8080-exec-4] [2f59617f-0179-...] DEBUG SoapTransactionIdInterceptor - Generated new transaction ID: 8a34bd42-ae20-...
2026-01-10 13:15:39.922 [http-nio-8080-exec-4] [8a34bd42-ae20-...] DEBUG SoapTransactionIdInterceptor - SOAP request processing with transaction ID: 8a34bd42-ae20-...
2026-01-10 13:15:39.949 [http-nio-8080-exec-4] [8a34bd42-ae20-...] DEBUG TransactionIdFilter - Completed request with transaction ID: 8a34bd42-ae20-...
```

**Notice**: The filter started with one ID, but ended with a different one!

---

## 🔍 Root Cause

### Execution Order:
1. **TransactionIdFilter** runs first (Servlet Filter level)
   - Checks for `X-Transaction-ID` HTTP header
   - Not found → generates UUID and puts in MDC
   
2. **SoapTransactionIdInterceptor** runs later (CXF Interceptor level)
   - Checks for `<TransactionId>` in SOAP XML header
   - Not found → **generates another UUID** (ignoring MDC!)
   - Overwrites MDC with the new UUID

### The Bug:
`SoapTransactionIdInterceptor` was **not checking MDC** before generating a new UUID, so it would always create a new one if the SOAP header was missing, even though the Filter had already created one.

---

## ✅ Solution Implemented

Modified `SoapTransactionIdInterceptor` to follow this priority:

```
1. Check SOAP Header (<TransactionId> in XML)
   ↓ (if found)
   Use it and update MDC
   
   ↓ (if NOT found)
2. Check MDC (set by Filter)
   ↓ (if found)
   Use existing transaction ID from Filter
   
   ↓ (if NOT found)
3. Generate new UUID
   Only as last resort
```

### Code Change:

**Before:**
```java
// Generate new transaction ID if not found
if (transactionId == null || transactionId.trim().isEmpty()) {
    transactionId = UUID.randomUUID().toString();
    logger.debug("Generated new transaction ID for SOAP request: {}", transactionId);
}
```

**After:**
```java
// If not in SOAP header, check if Filter already set one in MDC
if (transactionId == null || transactionId.trim().isEmpty()) {
    transactionId = MDC.get(TRANSACTION_ID_MDC_KEY);
    if (transactionId != null && !transactionId.trim().isEmpty()) {
        logger.debug("Using transaction ID from Filter (MDC): {}", transactionId);
    } else {
        // Only generate new UUID if neither SOAP header nor MDC has one
        transactionId = UUID.randomUUID().toString();
        logger.debug("Generated new transaction ID for SOAP request: {}", transactionId);
    }
}
```

---

## 📊 Expected Behavior After Fix

### Scenario 1: SOAP Request WITH Transaction ID in SOAP Header
```log
[filter-uuid] Filter - Generated UUID
[CUSTOM-ID]   Interceptor - Extracted from SOAP header: CUSTOM-ID
[CUSTOM-ID]   Service - Processing...
[CUSTOM-ID]   Filter - Completed request
```
✅ **Result**: Uses the custom ID from SOAP header

---

### Scenario 2: SOAP Request WITHOUT Transaction ID in SOAP Header
```log
[filter-uuid] Filter - Generated UUID: abc-123
[abc-123]     Interceptor - Using transaction ID from Filter (MDC): abc-123
[abc-123]     Service - Processing...
[abc-123]     Filter - Completed request
```
✅ **Result**: Uses the same UUID throughout (no duplicate generation)

---

### Scenario 3: REST Request with HTTP Header
```log
[CUSTOM-ID]   Filter - Received from upstream: CUSTOM-ID
[CUSTOM-ID]   Controller - Processing...
[CUSTOM-ID]   Filter - Completed request
```
✅ **Result**: No change, REST continues to work as before

---

### Scenario 4: REST Request WITHOUT HTTP Header
```log
[filter-uuid] Filter - Generated UUID: xyz-789
[xyz-789]     Controller - Processing...
[xyz-789]     Filter - Completed request
```
✅ **Result**: No change, REST continues to work as before

---

## 🎯 Benefits

1. **Single Transaction ID** per request (no duplicates)
2. **Consistent tracking** from start to finish
3. **Better debugging** - all logs for one request have the same ID
4. **Proper MDC reuse** - avoids unnecessary UUID generation
5. **Backward compatible** - REST requests unaffected

---

## 🔧 Files Modified

- **File**: `src/main/java/com/example/cxf/interceptor/SoapTransactionIdInterceptor.java`
- **Method**: `handleMessage(SoapMessage message)`
- **Change**: Added MDC check before generating new UUID

---

## 🧪 Testing

To verify the fix works:

### Test 1: SOAP without Transaction ID
```powershell
curl -X POST http://localhost:8080/services/Calculator -H "Content-Type: text/xml" -d "@test-requests/calculator-add.xml"
```

**Expected**: Same UUID throughout the logs (from Filter generation)

### Test 2: SOAP with Transaction ID
```powershell
$body = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>MY-CUSTOM-ID</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:add><cal:a>10</cal:a><cal:b>5</cal:b></cal:add>
   </soapenv:Body>
</soapenv:Envelope>
"@

Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body
```

**Expected**: "MY-CUSTOM-ID" throughout the logs (from SOAP header)

### Test 3: REST without Transaction ID
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5"
```

**Expected**: Same UUID throughout (from Filter generation)

### Test 4: REST with Transaction ID
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/calculator/add?a=10&b=5" -Headers @{"X-Transaction-ID"="REST-TXN-123"}
```

**Expected**: "REST-TXN-123" throughout (from HTTP header)

---

## 📝 Related Files

- [TRANSACTION_ID_GUIDE.md](./TRANSACTION_ID_GUIDE.md) - Main transaction ID documentation
- [TRANSACTION_ID_REST_VS_SOAP.md](./TRANSACTION_ID_REST_VS_SOAP.md) - Differences between REST and SOAP
- [SOAP_TESTING_GUIDE.md](./SOAP_TESTING_GUIDE.md) - SOAP testing examples

---

**Fix completed and tested! ✅**
