package com.example.cxf.logging;

/**
 * Error codes for application-wide error tracking and logging.
 * These codes help identify error types quickly in logs and monitoring systems.
 */
public enum ErrorCode {
    
    // Success
    SUCCESS("00", "Success", ErrorSeverity.INFO),
    
    // Business Logic Errors (01-19)
    DIVISION_BY_ZERO("01", "Division by zero attempted", ErrorSeverity.WARNING),
    INVALID_OPERATION("02", "Invalid operation specified", ErrorSeverity.WARNING),
    INVALID_INPUT("03", "Invalid input parameters", ErrorSeverity.WARNING),
    CALCULATION_ERROR("04", "Error during calculation", ErrorSeverity.ERROR),
    
    // System Errors (20-39)
    INTERNAL_ERROR("20", "Internal system error", ErrorSeverity.FATAL),
    DATABASE_ERROR("21", "Database connection error", ErrorSeverity.CRITICAL),
    SERVICE_UNAVAILABLE("22", "Service temporarily unavailable", ErrorSeverity.CRITICAL),
    TIMEOUT_ERROR("23", "Operation timed out", ErrorSeverity.ERROR),
    
    // Integration Errors (40-59)
    SOAP_PARSING_ERROR("40", "SOAP message parsing failed", ErrorSeverity.ERROR),
    REST_REQUEST_ERROR("41", "REST request processing failed", ErrorSeverity.ERROR),
    INVALID_REQUEST_FORMAT("42", "Invalid request format", ErrorSeverity.WARNING),
    
    // Configuration Errors (60-79)
    CONFIGURATION_ERROR("60", "Configuration error", ErrorSeverity.CRITICAL),
    MISSING_REQUIRED_PARAM("61", "Missing required parameter", ErrorSeverity.WARNING),
    
    // Unknown/Generic
    UNKNOWN_ERROR("99", "Unknown error occurred", ErrorSeverity.ERROR);
    
    private final String code;
    private final String message;
    private final ErrorSeverity severity;
    
    ErrorCode(String code, String message, ErrorSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    /**
     * Get formatted error code for logging: [CODE:SEVERITY]
     * Example: [01:WARNING], [20:FATAL]
     */
    public String getFormattedCode() {
        return String.format("[%s:%s]", code, severity.name());
    }
    
    /**
     * Get full error details for logging
     */
    public String getFullMessage() {
        return String.format("[ErrorCode:%s] [Severity:%s] %s", code, severity.name(), message);
    }
    
    @Override
    public String toString() {
        return getFormattedCode() + " " + message;
    }
}
