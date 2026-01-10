package com.example.cxf.rest.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for REST API documentation.
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI calculatorOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Calculator Service API - REST & SOAP")
                .description("""
                    ## Dual Protocol Calculator Service
                    
                    This service provides calculator operations via both **SOAP** and **REST** protocols:
                    
                    ### 🔷 SOAP Web Service (Primary)
                    - **WSDL:** [http://localhost:8080/services/Calculator?wsdl](http://localhost:8080/services/Calculator?wsdl)
                    - **SOAP Endpoint:** http://localhost:8080/services/Calculator
                    - **Framework:** Apache CXF 4.0.3
                    - **Binding:** Document/Literal
                    - **Operations:** 5 operations (add, subtract, multiply, divide, calculate)
                    - **Features:** SOAP Headers (ResponseDetails), SOAP Faults, Holder Pattern
                    
                    ### 🔶 REST API (Wrapper)
                    - **Base Path:** /api/calculator
                    - **SOAP Info:** /api/soap/services (Lists SOAP operations in REST format)
                    - **Documentation:** This Swagger UI
                    
                    ### 📋 Available Operations
                    1. **add** - Add two integers
                    2. **subtract** - Subtract two integers
                    3. **multiply** - Multiply two integers
                    4. **divide** - Divide two integers (with decimal result)
                    5. **calculate** - Dynamic operation with ResponseDetails metadata
                    
                    ### 🔧 SOAP Features
                    - **Holder Pattern:** JAX-WS Holder<T> for IN/OUT/INOUT parameters
                    - **SOAP Headers:** ResponseDetails (timestamp, serverId, processingTime)
                    - **SOAP Faults:** ServiceFailoverFault with retry information
                    - **Complex Types:** ResponseDetails with multiple fields
                    
                    ### 📝 Testing SOAP Service
                    Use the **/api/soap/** endpoints below to get:
                    - WSDL URL and service information
                    - Complete SOAP operation details
                    - Sample SOAP request envelopes
                    - Import WSDL into SoapUI/Postman for full SOAP testing
                    
                    ### 🚀 Quick Start
                    - **REST:** Try operations in the "Calculator API" section below
                    - **SOAP Info:** Check "SOAP Service Info" section for WSDL details
                    - **Full SOAP Test:** Import WSDL into SoapUI or use test-requests/*.xml files
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Calculator Service Team")
                    .email("support@example.com")
                    .url("http://example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server")
            ));
    }
}
