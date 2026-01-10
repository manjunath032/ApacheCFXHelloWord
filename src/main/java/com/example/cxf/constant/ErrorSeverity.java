package com.example.cxf.constant;

/**
 * Error severity levels for categorizing application errors.
 * Used in conjunction with ErrorCode for comprehensive error tracking.
 */
public enum ErrorSeverity {
    
    /**
     * Informational - No error, successful operation
     */
    INFO,
    
    /**
     * Warning - Potential issue, but operation can continue
     * Examples: Division by zero (handled), invalid input (with fallback)
     */
    WARNING,
    
    /**
     * Error - Operation failed, but system can continue
     * Examples: Calculation error, request parsing error
     */
    ERROR,
    
    /**
     * Critical - Serious error affecting system functionality
     * Examples: Database connection lost, service unavailable
     */
    CRITICAL,
    
    /**
     * Fatal - System cannot continue, immediate attention required
     * Examples: Configuration error at startup, unrecoverable system error
     */
    FATAL;
    
    /**
     * Check if this severity requires immediate attention
     */
    public boolean isHighSeverity() {
        return this == CRITICAL || this == FATAL;
    }
    
    /**
     * Check if this is an error condition
     */
    public boolean isError() {
        return this == ERROR || isHighSeverity();
    }
}
