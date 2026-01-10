package com.example.cxf.rest.controller;

import com.example.cxf.soap.service.impl.CalculatorServiceImpl;
import com.example.generated.calculator.ResponseDetails;
import com.example.generated.calculator.ServiceFailoverFault_Exception;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.xml.ws.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API wrapper for Calculator SOAP service.
 * Provides REST endpoints that internally call the SOAP service implementation.
 * This makes the calculator operations accessible via both SOAP and REST.
 */
@RestController
@RequestMapping("/api/calculator")
@Tag(name = "Calculator API", description = "REST API for calculator operations (wraps SOAP service)")
public class CalculatorRestController {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorRestController.class);

    @Autowired
    private CalculatorServiceImpl calculatorService;

    @GetMapping("/add")
    @Operation(
        summary = "Add two numbers",
        description = "Adds two integers and returns the result"
    )
    public ResponseEntity<Map<String, Object>> add(
            @Parameter(description = "First number", example = "10")
            @RequestParam int a,
            @Parameter(description = "Second number", example = "5")
            @RequestParam int b
    ) {
        logger.debug("Entering REST add() - a={}, b={}", a, b);
        try {
            int result = calculatorService.add(a, b);
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "ADD");
            response.put("operand1", a);
            response.put("operand2", b);
            response.put("result", result);
            logger.info("REST add() completed: {} + {} = {}", a, b, result);
            logger.debug("Exiting REST add() with result: {}", result);
            return ResponseEntity.ok(response);
        } catch (ServiceFailoverFault_Exception e) {
            logger.error("Error in REST add(): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/subtract")
    @Operation(
        summary = "Subtract two numbers",
        description = "Subtracts second number from first number"
    )
    public ResponseEntity<Map<String, Object>> subtract(
            @Parameter(description = "First number", example = "10")
            @RequestParam int a,
            @Parameter(description = "Second number", example = "5")
            @RequestParam int b
    ) {
        logger.debug("Entering REST subtract() - a={}, b={}", a, b);
        try {
            int result = calculatorService.subtract(a, b);
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "SUBTRACT");
            response.put("operand1", a);
            response.put("operand2", b);
            response.put("result", result);
            logger.info("REST subtract() completed: {} - {} = {}", a, b, result);
            logger.debug("Exiting REST subtract() with result: {}", result);
            return ResponseEntity.ok(response);
        } catch (ServiceFailoverFault_Exception e) {
            logger.error("Error in REST subtract(): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/multiply")
    @Operation(
        summary = "Multiply two numbers",
        description = "Multiplies two integers and returns the result"
    )
    public ResponseEntity<Map<String, Object>> multiply(
            @Parameter(description = "First number", example = "6")
            @RequestParam int a,
            @Parameter(description = "Second number", example = "7")
            @RequestParam int b
    ) {
        logger.debug("Entering REST multiply() - a={}, b={}", a, b);
        try {
            int result = calculatorService.multiply(a, b);
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "MULTIPLY");
            response.put("operand1", a);
            response.put("operand2", b);
            response.put("result", result);
            logger.info("REST multiply() completed: {} * {} = {}", a, b, result);
            logger.debug("Exiting REST multiply() with result: {}", result);
            return ResponseEntity.ok(response);
        } catch (ServiceFailoverFault_Exception e) {
            logger.error("Error in REST multiply(): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/divide")
    @Operation(
        summary = "Divide two numbers",
        description = "Divides first number by second number. Returns error if dividing by zero."
    )
    @ApiResponse(responseCode = "200", description = "Successful division")
    @ApiResponse(responseCode = "400", description = "Division by zero error")
    public ResponseEntity<Map<String, Object>> divide(
            @Parameter(description = "Numerator", example = "100")
            @RequestParam int a,
            @Parameter(description = "Denominator (cannot be zero)", example = "20")
            @RequestParam int b
    ) {
        logger.debug("Entering REST divide() - a={}, b={}", a, b);
        try {
            double result = calculatorService.divide(a, b);
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "DIVIDE");
            response.put("operand1", a);
            response.put("operand2", b);
            response.put("result", result);
            logger.info("REST divide() completed: {} / {} = {}", a, b, result);
            logger.debug("Exiting REST divide() with result: {}", result);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("REST divide() - Division by zero: {} / {}", a, b);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (ServiceFailoverFault_Exception e) {
            logger.error("Error in REST divide(): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/calculate")
    @Operation(
        summary = "Perform dynamic calculation",
        description = "Performs ADD, SUBTRACT, MULTIPLY, or DIVIDE based on operation parameter. Returns result with processing metadata."
    )
    @ApiResponse(responseCode = "200", description = "Successful calculation")
    @ApiResponse(responseCode = "400", description = "Invalid operation or division by zero")
    public ResponseEntity<Map<String, Object>> calculate(
            @Parameter(description = "First operand", example = "15")
            @RequestParam int operand1,
            @Parameter(description = "Second operand", example = "10")
            @RequestParam int operand2,
            @Parameter(description = "Operation type: ADD, SUBTRACT, MULTIPLY, or DIVIDE", example = "ADD")
            @RequestParam String operation
    ) {
        logger.debug("Entering REST calculate() - operand1={}, operand2={}, operation={}", 
                     operand1, operand2, operation);
        try {
            Holder<String> operationHolder = new Holder<>(operation);
            Holder<Double> resultHolder = new Holder<>();
            Holder<ResponseDetails> responseDetailsHolder = new Holder<>();

            calculatorService.calculate(operand1, operand2, operationHolder, resultHolder, responseDetailsHolder);

            Map<String, Object> response = new HashMap<>();
            response.put("operand1", operand1);
            response.put("operand2", operand2);
            response.put("operation", operationHolder.value);
            response.put("result", resultHolder.value);
            
            if (responseDetailsHolder.value != null) {
                Map<String, Object> details = new HashMap<>();
                ResponseDetails rd = responseDetailsHolder.value;
                details.put("timestamp", rd.getTimestamp() != null ? rd.getTimestamp().toString() : null);
                details.put("serverId", rd.getServerId());
                details.put("processingTime", rd.getProcessingTime());
                response.put("responseDetails", details);
            }

            logger.info("REST calculate() completed: {} {} {} = {}", 
                       operand1, operation, operand2, resultHolder.value);
            logger.debug("Exiting REST calculate() with result: {}", resultHolder.value);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("REST calculate() - Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (ServiceFailoverFault_Exception e) {
            logger.error("Error in REST calculate(): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the calculator service is running"
    )
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Calculator API",
            "version", "1.0.0"
        ));
    }
}
