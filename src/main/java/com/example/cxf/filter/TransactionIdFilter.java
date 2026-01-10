package com.example.cxf.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to add transaction/correlation ID to all requests.
 * - Checks for X-Transaction-ID header from upstream services
 * - Generates new UUID if not provided
 * - Adds to MDC for automatic inclusion in all logs
 * - Adds to response header for downstream tracking
 */
@Component
@Order(1)
public class TransactionIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionIdFilter.class);
    private static final String TRANSACTION_ID_HEADER = "X-Transaction-ID";
    private static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Get or generate transaction ID
        String transactionId = httpRequest.getHeader(TRANSACTION_ID_HEADER);
        if (transactionId == null || transactionId.trim().isEmpty()) {
            transactionId = UUID.randomUUID().toString();
            logger.debug("Generated new transaction ID: {}", transactionId);
        } else {
            logger.debug("Received transaction ID from upstream: {}", transactionId);
        }

        try {
            // Add transaction ID to MDC (will be included in all logs automatically)
            MDC.put(TRANSACTION_ID_MDC_KEY, transactionId);

            // Add to response header for downstream services
            httpResponse.setHeader(TRANSACTION_ID_HEADER, transactionId);

            logger.debug("Processing request: {} {} with transaction ID: {}", 
                        httpRequest.getMethod(), 
                        httpRequest.getRequestURI(), 
                        transactionId);

            // Continue with the request
            chain.doFilter(request, response);

            // Get the final transaction ID from MDC (may have been updated by SOAP interceptor)
            String finalTransactionId = MDC.get(TRANSACTION_ID_MDC_KEY);
            if (finalTransactionId == null) {
                finalTransactionId = transactionId; // Fallback to original
            }

            // Log if transaction ID was changed by interceptor
            if (!transactionId.equals(finalTransactionId)) {
                logger.debug("Transaction ID was updated during processing: {} -> {}", transactionId, finalTransactionId);
            }

            // Add final transaction ID to response header
            httpResponse.setHeader(TRANSACTION_ID_HEADER, finalTransactionId);

            logger.debug("Completed request: {} {} with transaction ID: {}", 
                        httpRequest.getMethod(), 
                        httpRequest.getRequestURI(), 
                        finalTransactionId);

        } finally {
            // Always clear MDC to avoid memory leaks in thread pools
            MDC.clear();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("TransactionIdFilter initialized - will track all requests with correlation IDs");
    }

    @Override
    public void destroy() {
        logger.info("TransactionIdFilter destroyed");
    }
}
