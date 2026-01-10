# Calculate Method Test Cases

This directory contains comprehensive test cases for the `calculate` SOAP operation with ResponseDetails support.

## Test Case Overview

### Positive Test Cases (Success Scenarios)

1. **calculate-add.xml**
   - Operation: ADD
   - Operands: 15 + 10
   - Expected Result: 25.0
   - Tests: Basic addition with ResponseDetails

2. **calculate-subtract.xml**
   - Operation: SUBTRACT (lowercase input)
   - Operands: 100 - 50
   - Expected Result: 50.0
   - Tests: Subtraction with case normalization

3. **calculate-multiply.xml**
   - Operation: MULTIPLY
   - Operands: 8 * 9
   - Expected Result: 72.0
   - Tests: Multiplication operation

4. **calculate-divide.xml**
   - Operation: DIVIDE (lowercase input)
   - Operands: 100 / 20
   - Expected Result: 5.0
   - Tests: Clean division

5. **calculate-divide-decimal.xml**
   - Operation: DIVIDE
   - Operands: 10 / 3
   - Expected Result: 3.333...
   - Tests: Division with decimal result

6. **calculate-negative-numbers.xml**
   - Operation: ADD
   - Operands: -50 + 15
   - Expected Result: -35.0
   - Tests: Negative number handling

7. **calculate-case-insensitive.xml**
   - Operation: multiply (lowercase)
   - Operands: 6 * 8
   - Expected Result: 48.0
   - Tests: Case-insensitive operation names

### Negative Test Cases (Error Scenarios)

8. **calculate-divide-by-zero.xml**
   - Operation: DIVIDE
   - Operands: 100 / 0
   - Expected: SOAP Fault "Division by zero is not allowed"
   - Tests: Division by zero error handling

9. **calculate-invalid-operation.xml**
   - Operation: POWER (unsupported)
   - Operands: 2, 8
   - Expected: SOAP Fault "Unknown operation: POWER"
   - Tests: Invalid operation error handling

## Expected Response Structure

All successful calculate operations return:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:calculateResponse xmlns:ns2="http://example.com/calculator">
         <operation>ADD</operation>  <!-- Normalized to uppercase -->
         <result>25.0</result>
         <responseDetails>
            <timestamp>2026-01-10T11:45:30.123+05:30</timestamp>
            <serverId>HOSTNAME-12345</serverId>
            <processingTime>2</processingTime>  <!-- milliseconds -->
         </responseDetails>
      </ns2:calculateResponse>
   </soap:Body>
</soap:Envelope>
```

## ResponseDetails Fields

- **timestamp**: XMLGregorianCalendar - Time when the calculation was performed
- **serverId**: String - Server hostname + thread ID for distributed system tracking
- **processingTime**: long - Execution time in milliseconds

## How to Test

### Using cURL (Windows PowerShell)

```powershell
# Test Case 1: ADD operation
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -H "SOAPAction: calculate" `
  -d "@test-requests/calculate-add.xml"

# Test Case 4: DIVIDE operation
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -H "SOAPAction: calculate" `
  -d "@test-requests/calculate-divide.xml"

# Test Case 6: Division by zero (error)
curl -X POST http://localhost:8080/services/Calculator `
  -H "Content-Type: text/xml" `
  -H "SOAPAction: calculate" `
  -d "@test-requests/calculate-divide-by-zero.xml"
```

### Using SoapUI or Postman

1. Start the application: `mvn spring-boot:run`
2. WSDL URL: http://localhost:8080/services/Calculator?wsdl
3. Import WSDL into SoapUI/Postman
4. Copy test request XML from files above
5. Submit to endpoint: http://localhost:8080/services/Calculator

## Verification Points

✅ **Operation Parameter (INOUT)**
- Input operation is normalized to uppercase
- Returned in response unchanged

✅ **Result Parameter (OUT)**
- Correct calculation result as double
- Decimal precision maintained

✅ **ResponseDetails Parameter (OUT)**
- Timestamp is current server time
- ServerId contains hostname and thread identifier
- ProcessingTime is non-negative value in milliseconds

✅ **Error Handling**
- Division by zero throws IllegalArgumentException
- Invalid operation throws IllegalArgumentException
- Proper SOAP fault structure returned

## Implementation Features

- **Case-Insensitive Operations**: "add", "ADD", "Add" all work
- **Holder Pattern**: IN/OUT/INOUT parameters using `jakarta.xml.ws.Holder<T>`
- **Complex Type Response**: ResponseDetails with timestamp, serverId, processingTime
- **Performance Metrics**: Processing time tracked and returned
- **Distributed Tracing**: Server ID enables request tracking across services

## Expected Console Output

```
Calculate operation called: 15 ADD 10
Result: 15 + 10 = 25.0
Processing time: 2ms, Server: DESKTOP-ABC123-12345
```

## Testing Checklist

- [ ] Test all 4 operations (ADD, SUBTRACT, MULTIPLY, DIVIDE)
- [ ] Verify ResponseDetails contains valid timestamp
- [ ] Verify ResponseDetails contains serverId
- [ ] Verify ResponseDetails contains processingTime > 0
- [ ] Test case insensitivity (lowercase operation names)
- [ ] Test negative numbers
- [ ] Test decimal division results
- [ ] Test division by zero error
- [ ] Test invalid operation error
- [ ] Verify operation parameter normalization to uppercase
