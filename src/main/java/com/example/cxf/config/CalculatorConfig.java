package com.example.cxf.config;

import com.example.cxf.service.impl.CalculatorServiceImpl;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalculatorConfig {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorConfig.class);
    
    @Autowired
    private Bus bus;
    
    @Autowired
    private CalculatorServiceImpl calculatorService;
    
    @Bean
    public Endpoint calculatorEndpoint() {
        logger.debug("Entering calculatorEndpoint() - Configuring SOAP endpoint");
        try {
            EndpointImpl endpoint = new EndpointImpl(bus, calculatorService);
            endpoint.publish("/Calculator");
            logger.info("SOAP endpoint published successfully at /Calculator");
            logger.debug("Exiting calculatorEndpoint() - Endpoint configured");
            return endpoint;
        } catch (Exception e) {
            logger.error("Error publishing SOAP endpoint: {}", e.getMessage(), e);
            throw e;
        }
    }
}
