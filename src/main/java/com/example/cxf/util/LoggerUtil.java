package com.example.cxf.util;

import com.example.cxf.constant.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Utility class for logging with error codes.
 * Automatically manages MDC for error codes and provides convenient logging methods.
 */
public class LoggerUtil {

    private static final String ERROR_CODE_MDC_KEY = "errorCode";

    /**
     * Log with error code and automatically clean up MDC
     */
    public static void logInfo(Logger logger, ErrorCode errorCode, String message, Object... args) {
        try {
            MDC.put(ERROR_CODE_MDC_KEY, errorCode.getCode());
            logger.info(errorCode.getFormattedCode() + " " + message, args);
        } finally {
            MDC.remove(ERROR_CODE_MDC_KEY);
        }
    }

    /**
     * Log warning with error code and automatically clean up MDC
     */
    public static void logWarn(Logger logger, ErrorCode errorCode, String message, Object... args) {
        try {
            MDC.put(ERROR_CODE_MDC_KEY, errorCode.getCode());
            logger.warn(errorCode.getFormattedCode() + " " + message, args);
        } finally {
            MDC.remove(ERROR_CODE_MDC_KEY);
        }
    }

    /**
     * Log error with error code and automatically clean up MDC
     */
    public static void logError(Logger logger, ErrorCode errorCode, String message, Object... args) {
        try {
            MDC.put(ERROR_CODE_MDC_KEY, errorCode.getCode());
            logger.error(errorCode.getFormattedCode() + " " + message, args);
        } finally {
            MDC.remove(ERROR_CODE_MDC_KEY);
        }
    }

    /**
     * Log error with error code, exception, and automatically clean up MDC
     */
    public static void logError(Logger logger, ErrorCode errorCode, String message, Throwable throwable) {
        try {
            MDC.put(ERROR_CODE_MDC_KEY, errorCode.getCode());
            logger.error(errorCode.getFormattedCode() + " " + message, throwable);
        } finally {
            MDC.remove(ERROR_CODE_MDC_KEY);
        }
    }

    /**
     * Log debug (no error code needed for debug)
     */
    public static void logDebug(Logger logger, String message, Object... args) {
        logger.debug(message, args);
    }
}
