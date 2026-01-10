package com.example.cxf.service.impl;

import com.example.cxf.constant.ErrorCode;
import com.example.cxf.util.LoggerUtil;
import com.example.generated.calculator.CalculatorPortType;
import com.example.generated.calculator.ResponseDetails;
import com.example.generated.calculator.ServiceFailoverFault_Exception;
import jakarta.jws.WebService;
import jakarta.xml.ws.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.InetAddress;
import java.util.GregorianCalendar;

/**
 * Implementation of Calculator service using generated interface with holder classes.
 * CXF automatically wraps/unwraps the request/response objects using the
 * @RequestWrapper and @ResponseWrapper annotations on the interface.
 * 
 * This demonstrates:
 * - Simple request/response holders (add, subtract, multiply, divide)
 * - Advanced holders with multiple fields (calculate)
 * - IN/OUT/INOUT parameter patterns using Holder<T>
 * - SOAP output headers (ResponseDetails)
 * - SOAP faults (ServiceFailoverFault)
 */
@Service
@WebService(
    serviceName = "CalculatorService",
    portName = "CalculatorPort",
    targetNamespace = "http://example.com/calculator",
    endpointInterface = "com.example.generated.calculator.CalculatorPortType"
)
public class CalculatorServiceImpl implements CalculatorPortType {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);

    @Override
    public int add(int a, int b) throws ServiceFailoverFault_Exception {
        logger.debug("Entering add() method with parameters: a={}, b={}", a, b);
        try {
            int result = a + b;
            logger.info("Add operation: {} + {} = {}", a, b, result);
            logger.debug("Exiting add() method with result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error in add() method: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int subtract(int a, int b) throws ServiceFailoverFault_Exception {
        logger.debug("Entering subtract() method with parameters: a={}, b={}", a, b);
        try {
            int result = a - b;
            logger.info("Subtract operation: {} - {} = {}", a, b, result);
            logger.debug("Exiting subtract() method with result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error in subtract() method: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int multiply(int a, int b) throws ServiceFailoverFault_Exception {
        logger.debug("Entering multiply() method with parameters: a={}, b={}", a, b);
        try {
            int result = a * b;
            logger.info("Multiply operation: {} * {} = {}", a, b, result);
            logger.debug("Exiting multiply() method with result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error in multiply() method: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public double divide(int a, int b) throws ServiceFailoverFault_Exception {
        logger.debug("Entering divide() method with parameters: a={}, b={}", a, b);
        try {
            if (b == 0) {
                LoggerUtil.logWarn(logger, ErrorCode.DIVISION_BY_ZERO, "Division by zero attempted: {} / {}", a, b);
                throw new IllegalArgumentException("Division by zero is not allowed");
            }
            double result = (double) a / b;
            LoggerUtil.logInfo(logger, ErrorCode.SUCCESS, "Divide operation: {} / {} = {}", a, b, result);
            logger.debug("Exiting divide() method with result: {}", result);
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LoggerUtil.logError(logger, ErrorCode.CALCULATION_ERROR, "Error in divide() method: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void calculate(int operand1, int operand2, 
                         Holder<String> operation, 
                         Holder<Double> result, 
                         Holder<ResponseDetails> responseDetails) throws ServiceFailoverFault_Exception {
        
        logger.debug("Entering calculate() method with parameters: operand1={}, operand2={}, operation={}", 
                     operand1, operand2, operation.value);
        
        long startTime = System.currentTimeMillis();
        String op = operation.value.toUpperCase();
        double calcResult;
        String symbol;
        
        try {
            switch (op) {
                case "ADD":
                    calcResult = operand1 + operand2;
                    symbol = "+";
                    break;
                case "SUBTRACT":
                    calcResult = operand1 - operand2;
                    symbol = "-";
                    break;
                case "MULTIPLY":
                    calcResult = operand1 * operand2;
                    symbol = "*";
                    break;
                case "DIVIDE":
                    if (operand2 == 0) {
                        LoggerUtil.logError(logger, ErrorCode.DIVISION_BY_ZERO, 
                            "Division by zero attempted in calculate(): {} / {}", operand1, operand2);
                        throw new IllegalArgumentException("Division by zero is not allowed");
                    }
                    calcResult = (double) operand1 / operand2;
                    symbol = "/";
                    break;
                default:
                    LoggerUtil.logWarn(logger, ErrorCode.INVALID_OPERATION, 
                        "Invalid operation attempted: {}", op);
                    throw new IllegalArgumentException("Unknown operation: " + op + 
                        ". Supported operations: ADD, SUBTRACT, MULTIPLY, DIVIDE");
            }
        
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Set OUT parameters using Holder objects
            result.value = calcResult;
            
            // Create and populate ResponseDetails
            ResponseDetails details = new ResponseDetails();
            try {
                // Set timestamp
                GregorianCalendar gcal = new GregorianCalendar();
                XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
                details.setTimestamp(xmlDate);
                
                // Set server ID
                String serverId = InetAddress.getLocalHost().getHostName() + "-" + Thread.currentThread().threadId();
                details.setServerId(serverId);
                
                // Set processing time
                details.setProcessingTime(processingTime);
                
                responseDetails.value = details;
                
                LoggerUtil.logInfo(logger, ErrorCode.SUCCESS, 
                    "Calculate operation: {} {} {} = {}", operand1, symbol, operand2, calcResult);
                logger.debug("Processing details - Time: {}ms, Server: {}", processingTime, serverId);
                logger.debug("Exiting calculate() method with result: {}", calcResult);
                
            } catch (Exception e) {
                LoggerUtil.logError(logger, ErrorCode.INTERNAL_ERROR, 
                    "Error creating response details in calculate(): " + e.getMessage(), e);
                throw new RuntimeException("Error creating response details", e);
            }
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors (already logged with error code)
            throw e;
        } catch (Exception e) {
            LoggerUtil.logError(logger, ErrorCode.CALCULATION_ERROR, 
                "Error in calculate() method: " + e.getMessage(), e);
            throw e;
        }
    }
}
