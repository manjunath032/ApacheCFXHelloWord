package com.example.cxf.config;

import com.example.cxf.service.impl.CalculatorServiceImpl;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalculatorConfig {
    
    @Autowired
    private Bus bus;
    
    @Autowired
    private CalculatorServiceImpl calculatorService;
    
    @Bean
    public Endpoint calculatorEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, calculatorService);
        endpoint.publish("/Calculator");
        return endpoint;
    }
}
