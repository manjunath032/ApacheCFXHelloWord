# Error Code System Documentation

This document explains how to use the error code system for structured logging and error tracking.

## 📋 Overview

The application uses a standardized error code system with:
- **Error Codes**: 2-digit codes (00-99) identifying specific error types
- **Severity Levels**: INFO, WARNING, ERROR, CRITICAL, FATAL
- **MDC Integration**: Error codes automatically appear in all logs

## 🎯 Error Code Categories

### Success (00)
| Code | Description | Severity |
|------|-------------|----------|
| 00 | Success | INFO |

### Business Logic Errors (01-19)
| Code | Description | Severity |
|------|-------------|----------|
| 01 | Division by zero attempted | WARNING |
| 02 | Invalid operation specified | WARNING |
| 03 | Invalid input parameters | WARNING |
| 04 | Error during calculation | ERROR |

### System Errors (20-39)
| Code | Description | Severity |
|------|-------------|----------|
| 20 | Internal system error | FATAL |
| 21 | Database connection error | CRITICAL |
| 22 | Service temporarily unavailable | CRITICAL |
| 23 | Operation timed out | ERROR |

### Integration Errors (40-59)
| Code | Description | Severity |
|------|-------------|----------|
| 40 | SOAP message parsing failed | ERROR |
| 41 | REST request processing failed | ERROR |
| 42 | Invalid request format | WARNING |

### Configuration Errors (60-79)
| Code | Description | Severity |
|------|-------------|----------|
| 60 | Configuration error | CRITICAL |
| 61 | Missing required parameter | WARNING |

### Unknown/Generic (99)
| Code | Description | Severity |
|------|-------------|----------|
| 99 | Unknown error occurred | ERROR |

## 🔧 Severity Levels

| Severity | Description | Use Case |
|----------|-------------|----------|
| **INFO** | Informational, successful operation | Normal operations, success states |
| **WARNING** | Potential issue, operation continues | Validation errors with fallback, recoverable issues |
| **ERROR** | Operation failed, system can continue | Calculation errors, request failures |
| **CRITICAL** | Serious error affecting functionality | Database issues, service unavailable |
| **FATAL** | System cannot continue | Configuration errors, unrecoverable failures |

## 💻 Usage Examples

### Basic Usage with MDC

```java
import com.example.cxf.constant.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void someMethod() {
        try {
            // Add error code to MDC
            MDC.put("errorCode", ErrorCode.SUCCESS.getCode());
            
            // Log with formatted error code in message
            logger.info("{} Operation completed successfully", 
                       ErrorCode.SUCCESS.getFormattedCode());
            
            // Remove from MDC when done
            MDC.remove("errorCode");
            
        } catch (Exception e) {
            MDC.put("errorCode", ErrorCode.CALCULATION_ERROR.getCode());
            logger.error("{} Error occurred: {}", 
                        ErrorCode.CALCULATION_ERROR.getFormattedCode(), 
                        e.getMessage(), e);
            MDC.remove("errorCode");
        }
    }
}
```

### Example from CalculatorServiceImpl

```java
@Override
public double divide(int a, int b) {
    logger.debug("Entering divide() method with parameters: a={}, b={}", a, b);
    try {
        if (b == 0) {
            // Log warning with error code
            MDC.put("errorCode", ErrorCode.DIVISION_BY_ZERO.getCode());
            logger.warn("{} Division by zero attempted: {} / {}", 
                       ErrorCode.DIVISION_BY_ZERO.getFormattedCode(), a, b);
            MDC.remove("errorCode");
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        
        double result = (double) a / b;
        
        // Log success with error code
        MDC.put("errorCode", ErrorCode.SUCCESS.getCode());
        logger.info("{} Divide operation: {} / {} = {}", 
                   ErrorCode.SUCCESS.getFormattedCode(), a, b, result);
        MDC.remove("errorCode");
        
        return result;
    } catch (Exception e) {
        // Log error with error code
        MDC.put("errorCode", ErrorCode.CALCULATION_ERROR.getCode());
        logger.error("{} Error in divide(): {}", 
                    ErrorCode.CALCULATION_ERROR.getFormattedCode(), 
                    e.getMessage(), e);
        MDC.remove("errorCode");
        throw e;
    }
}
```

## 📊 Log Output Format

With error codes, your logs will look like this:

```log
2026-01-10 13:45:12.123 [http-nio-8080-exec-1] [txn-123] [00] INFO  c.e.c.s.impl.CalculatorServiceImpl - [00:INFO] Divide operation: 10 / 2 = 5.0
2026-01-10 13:45:15.456 [http-nio-8080-exec-2] [txn-456] [01] WARN  c.e.c.s.impl.CalculatorServiceImpl - [01:WARNING] Division by zero attempted: 10 / 0
2026-01-10 13:45:18.789 [http-nio-8080-exec-3] [txn-789] [04] ERROR c.e.c.s.impl.CalculatorServiceImpl - [04:ERROR] Error in divide(): Calculation failed
```

**Format breakdown:**
```
[timestamp] [thread] [transactionId] [errorCode] [level] [logger] - [CODE:SEVERITY] message
```

## 🎨 Error Code Methods

### ErrorCode Enum Methods

```java
ErrorCode errorCode = ErrorCode.DIVISION_BY_ZERO;

// Get numeric code
String code = errorCode.getCode();  // "01"

// Get description
String message = errorCode.getMessage();  // "Division by zero attempted"

// Get severity
ErrorSeverity severity = errorCode.getSeverity();  // WARNING

// Get formatted for logging
String formatted = errorCode.getFormattedCode();  // "[01:WARNING]"

// Get full message
String full = errorCode.getFullMessage();  
// "[ErrorCode:01] [Severity:WARNING] Division by zero attempted"

// ToString
String str = errorCode.toString();  // "[01:WARNING] Division by zero attempted"
```

### ErrorSeverity Methods

```java
ErrorSeverity severity = ErrorSeverity.CRITICAL;

// Check if high severity (CRITICAL or FATAL)
boolean isHigh = severity.isHighSeverity();  // true

// Check if any error level
boolean isError = severity.isError();  // true (ERROR, CRITICAL, or FATAL)
```

## 🔍 Searching Logs by Error Code

### PowerShell
```powershell
# Find all warnings
Get-Content logs/application.log | Select-String "\[01\]"

# Find all critical/fatal errors
Get-Content logs/application.log | Select-String "\[(20|21|22|60)\]"

# Find specific transaction with error
Get-Content logs/application.log | Select-String "txn-123" | Select-String "\[01\]"
```

### grep (Linux/Mac)
```bash
# Find division by zero errors
grep "\[01\]" logs/application.log

# Find all errors (not INFO or WARNING)
grep -E "\[(04|2[0-9]|4[0-9]|99)\]" logs/application.log
```

## 📈 Best Practices

### 1. Always Use Try-Finally for MDC
```java
try {
    MDC.put("errorCode", ErrorCode.SUCCESS.getCode());
    // ... your code ...
} finally {
    MDC.remove("errorCode");  // Always clean up!
}
```

### 2. Match Log Level to Severity
```java
// INFO severity → logger.info()
MDC.put("errorCode", ErrorCode.SUCCESS.getCode());
logger.info("{} Success", ErrorCode.SUCCESS.getFormattedCode());

// WARNING severity → logger.warn()
MDC.put("errorCode", ErrorCode.DIVISION_BY_ZERO.getCode());
logger.warn("{} Warning", ErrorCode.DIVISION_BY_ZERO.getFormattedCode());

// ERROR severity → logger.error()
MDC.put("errorCode", ErrorCode.CALCULATION_ERROR.getCode());
logger.error("{} Error", ErrorCode.CALCULATION_ERROR.getFormattedCode());
```

### 3. Include Error Code in Message
```java
// Good - Error code visible in both MDC and message
MDC.put("errorCode", ErrorCode.DIVISION_BY_ZERO.getCode());
logger.warn("{} Division by zero: {} / {}", 
           ErrorCode.DIVISION_BY_ZERO.getFormattedCode(), a, b);

// Also good - Use full message
logger.error(ErrorCode.CALCULATION_ERROR.getFullMessage());
```

### 4. Remove Error Code After Logging
```java
// Pattern 1: Immediate removal
MDC.put("errorCode", "01");
logger.warn("...");
MDC.remove("errorCode");

// Pattern 2: Try-finally
try {
    MDC.put("errorCode", "01");
    logger.warn("...");
} finally {
    MDC.remove("errorCode");
}
```

## 🚀 Adding New Error Codes

To add a new error code, edit `ErrorCode.java`:

```java
public enum ErrorCode {
    // ... existing codes ...
    
    // New error code
    YOUR_NEW_ERROR("05", "Description of error", ErrorSeverity.ERROR),
    
    // ... rest of enum ...
}
```

**Naming conventions:**
- Use descriptive UPPER_SNAKE_CASE names
- Group by category (business/system/integration/config)
- Assign codes in ranges (01-19, 20-39, etc.)

## 🔗 Integration with Transaction IDs

Error codes work seamlessly with transaction IDs:

```log
[timestamp] [thread] [txn-abc-123] [01] WARN - [01:WARNING] Division by zero
[timestamp] [thread] [txn-abc-123] [04] ERROR - [04:ERROR] Calculation failed
```

Both appear in logs, allowing you to:
- Track all errors for a specific transaction
- Find all instances of a specific error type
- Correlate errors across microservices

## 📚 Related Documentation

- [TRANSACTION_ID_GUIDE.md](./TRANSACTION_ID_GUIDE.md) - Transaction ID tracking
- [TRANSACTION_ID_REST_VS_SOAP.md](./TRANSACTION_ID_REST_VS_SOAP.md) - Protocol differences
- [TRANSACTION_ID_FIX.md](./TRANSACTION_ID_FIX.md) - Bug fixes

---

**Error codes + Transaction IDs = Complete observability! 🎯**
