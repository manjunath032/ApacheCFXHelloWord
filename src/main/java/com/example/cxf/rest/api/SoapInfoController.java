package com.example.cxf.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller to provide information about SOAP services.
 * This helps document SOAP endpoints in Swagger UI.
 */
@RestController
@RequestMapping("/api/soap")
@Tag(name = "SOAP Service Info", description = "Information about available SOAP web services")
public class SoapInfoController {

    private static final Logger logger = LoggerFactory.getLogger(SoapInfoController.class);

    @Value("${server.port:8080}")
    private String serverPort;

    @GetMapping("/services")
    @Operation(
        summary = "List all SOAP services",
        description = "Returns information about all available SOAP web services, including WSDL URLs and operations"
    )
    public ResponseEntity<Map<String, Object>> listSoapServices() {
        logger.debug("Entering listSoapServices()");
        Map<String, Object> response = new HashMap<>();
        
        String baseUrl = "http://localhost:" + serverPort;
        
        // Calculator SOAP Service Info
        Map<String, Object> calculatorService = new HashMap<>();
        calculatorService.put("serviceName", "CalculatorService");
        calculatorService.put("namespace", "http://example.com/calculator");
        calculatorService.put("wsdlUrl", baseUrl + "/services/Calculator?wsdl");
        calculatorService.put("soapEndpoint", baseUrl + "/services/Calculator");
        calculatorService.put("binding", "Document/Literal");
        calculatorService.put("operations", List.of(
            createOperation("add", "Add two integers", 
                List.of(param("a", "int", "First number"), param("b", "int", "Second number")),
                "int"),
            createOperation("subtract", "Subtract two integers", 
                List.of(param("a", "int", "First number"), param("b", "int", "Second number")),
                "int"),
            createOperation("multiply", "Multiply two integers", 
                List.of(param("a", "int", "First number"), param("b", "int", "Second number")),
                "int"),
            createOperation("divide", "Divide two integers", 
                List.of(param("a", "int", "Numerator"), param("b", "int", "Denominator")),
                "double"),
            createOperation("calculate", "Dynamic calculation with metadata", 
                List.of(
                    param("operand1", "int", "First operand"),
                    param("operand2", "int", "Second operand"),
                    param("operation", "string (INOUT)", "Operation: ADD, SUBTRACT, MULTIPLY, DIVIDE")
                ),
                Map.of(
                    "result", "double (OUT)",
                    "responseDetails", "ResponseDetails (OUT) - Contains timestamp, serverId, processingTime"
                ))
        ));
        
        Map<String, Object> soapHeaders = new HashMap<>();
        soapHeaders.put("ResponseDetails", Map.of(
            "type", "Output Header",
            "fields", Map.of(
                "timestamp", "dateTime - Processing timestamp",
                "serverId", "string - Server identifier",
                "processingTime", "long - Processing time in milliseconds"
            )
        ));
        calculatorService.put("soapHeaders", soapHeaders);
        
        Map<String, Object> soapFaults = new HashMap<>();
        soapFaults.put("ServiceFailoverFault", Map.of(
            "fields", Map.of(
                "faultCode", "string - Error code",
                "faultMessage", "string - Error description",
                "failoverNode", "string - Failover node information",
                "retryAfter", "int - Retry delay in seconds"
            )
        ));
        calculatorService.put("soapFaults", soapFaults);
        
        response.put("calculatorService", calculatorService);
        response.put("totalServices", 1);
        response.put("documentation", Map.of(
            "wsdlStandard", "WSDL 1.1",
            "soapVersion", "SOAP 1.1/1.2",
            "framework", "Apache CXF 4.0.3",
            "holderPattern", "Uses JAX-WS Holder<T> for IN/OUT/INOUT parameters"
        ));
        
        logger.info("SOAP services list generated successfully");
        logger.debug("Exiting listSoapServices()");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calculator/wsdl-url")
    @Operation(
        summary = "Get Calculator WSDL URL",
        description = "Returns the direct URL to access Calculator service WSDL"
    )
    public ResponseEntity<Map<String, String>> getCalculatorWsdlUrl() {
        logger.debug("Calculator WSDL URL requested");
        String baseUrl = "http://localhost:" + serverPort;
        return ResponseEntity.ok(Map.of(
            "wsdlUrl", baseUrl + "/services/Calculator?wsdl",
            "soapEndpoint", baseUrl + "/services/Calculator",
            "instructions", "Copy the WSDL URL and import it into SoapUI, Postman, or any SOAP client"
        ));
    }

    @GetMapping("/calculator/operations")
    @Operation(
        summary = "List Calculator SOAP operations",
        description = "Returns detailed information about all SOAP operations available in Calculator service"
    )
    public ResponseEntity<List<Map<String, Object>>> getCalculatorOperations() {
        logger.debug("Calculator operations list requested");
        return ResponseEntity.ok(List.of(
            createOperationDetail("add", "POST", 
                "Add two integers and return the sum",
                "<cal:add><a>10</a><b>5</b></cal:add>",
                "<cal:addResponse><result>15</result></cal:addResponse>"),
            
            createOperationDetail("subtract", "POST",
                "Subtract second number from first",
                "<cal:subtract><a>10</a><b>5</b></cal:subtract>",
                "<cal:subtractResponse><result>5</result></cal:subtractResponse>"),
            
            createOperationDetail("multiply", "POST",
                "Multiply two integers",
                "<cal:multiply><a>6</a><b>7</b></cal:multiply>",
                "<cal:multiplyResponse><result>42</result></cal:multiplyResponse>"),
            
            createOperationDetail("divide", "POST",
                "Divide first number by second (returns double)",
                "<cal:divide><a>10</a><b>3</b></cal:divide>",
                "<cal:divideResponse><result>3.3333333333333335</result></cal:divideResponse>"),
            
            createOperationDetail("calculate", "POST",
                "Dynamic calculation with operation type and metadata",
                "<cal:calculate><operand1>15</operand1><operand2>10</operand2><operation>ADD</operation></cal:calculate>",
                "<cal:calculateResponse><result>25.0</result><operation>ADD</operation><responseDetails><timestamp>2026-01-10T12:00:00</timestamp><serverId>Server-1</serverId><processingTime>2</processingTime></responseDetails></cal:calculateResponse>")
        ));
    }

    @GetMapping("/calculator/test-requests")
    @Operation(
        summary = "Get sample SOAP test requests",
        description = "Returns complete SOAP envelope examples for testing Calculator service"
    )
    public ResponseEntity<Map<String, String>> getTestRequests() {
        logger.debug("Test requests examples requested");
        String baseUrl = "http://localhost:" + serverPort;
        return ResponseEntity.ok(Map.of(
            "soapEndpoint", baseUrl + "/services/Calculator",
            "contentType", "text/xml",
            "soapAction", "Operation name (e.g., 'add', 'calculate')",
            "addExample", """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" 
                               xmlns:cal="http://example.com/calculator">
                   <soap:Body>
                      <cal:add>
                         <cal:a>10</cal:a>
                         <cal:b>5</cal:b>
                      </cal:add>
                   </soap:Body>
                </soap:Envelope>
                """,
            "calculateExample", """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" 
                               xmlns:cal="http://example.com/calculator">
                   <soap:Body>
                      <cal:calculate>
                         <cal:operand1>15</cal:operand1>
                         <cal:operand2>10</cal:operand2>
                         <cal:operation>ADD</cal:operation>
                      </cal:calculate>
                   </soap:Body>
                </soap:Envelope>
                """
        ));
    }

    private Map<String, Object> createOperation(String name, String description, 
                                                 List<Map<String, String>> params, 
                                                 Object returnType) {
        Map<String, Object> op = new HashMap<>();
        op.put("name", name);
        op.put("description", description);
        op.put("parameters", params);
        op.put("returnType", returnType);
        return op;
    }

    private Map<String, String> param(String name, String type, String description) {
        return Map.of("name", name, "type", type, "description", description);
    }

    private Map<String, Object> createOperationDetail(String name, String method, 
                                                       String description,
                                                       String requestExample, 
                                                       String responseExample) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("operation", name);
        detail.put("method", method);
        detail.put("description", description);
        detail.put("namespace", "http://example.com/calculator");
        detail.put("requestExample", requestExample);
        detail.put("responseExample", responseExample);
        return detail;
    }
}
