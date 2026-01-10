package com.example.cxf.service.impl;

import com.example.generated.calculator.CalculatorPortType;
import com.example.generated.calculator.ResponseDetails;
import com.example.generated.calculator.ServiceFailoverFault_Exception;
import jakarta.jws.WebService;
import jakarta.xml.ws.Holder;
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

    @Override
    public int add(int a, int b) throws ServiceFailoverFault_Exception {
        System.out.println("Add operation called: " + a + " + " + b);
        return a + b;
    }

    @Override
    public int subtract(int a, int b) throws ServiceFailoverFault_Exception {
        System.out.println("Subtract operation called: " + a + " - " + b);
        return a - b;
    }

    @Override
    public int multiply(int a, int b) throws ServiceFailoverFault_Exception {
        System.out.println("Multiply operation called: " + a + " * " + b);
        return a * b;
    }

    @Override
    public double divide(int a, int b) throws ServiceFailoverFault_Exception {
        System.out.println("Divide operation called: " + a + " / " + b);
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return (double) a / b;
    }

    @Override
    public void calculate(int operand1, int operand2, 
                         Holder<String> operation, 
                         Holder<Double> result, 
                         Holder<ResponseDetails> responseDetails) throws ServiceFailoverFault_Exception {
        
        long startTime = System.currentTimeMillis();
        String op = operation.value.toUpperCase();
        double calcResult;
        String symbol;
        
        System.out.println("Calculate operation called: " + operand1 + " " + op + " " + operand2);
        
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
                    throw new IllegalArgumentException("Division by zero is not allowed");
                }
                calcResult = (double) operand1 / operand2;
                symbol = "/";
                break;
            default:
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
            
            System.out.println("Result: " + operand1 + " " + symbol + " " + operand2 + " = " + calcResult);
            System.out.println("Processing time: " + processingTime + "ms, Server: " + serverId);
            
        } catch (Exception e) {
            System.err.println("Error creating response details: " + e.getMessage());
            throw new RuntimeException("Error creating response details", e);
        }
    }
}
