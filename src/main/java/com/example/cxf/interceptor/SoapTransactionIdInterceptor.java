package com.example.cxf.interceptor;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.UUID;

/**
 * SOAP Interceptor to extract/add transaction ID from/to SOAP headers.
 * Handles correlation ID for SOAP requests to track end-to-end flow.
 */
@Component
public class SoapTransactionIdInterceptor extends AbstractSoapInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SoapTransactionIdInterceptor.class);
    private static final String TRANSACTION_ID_HEADER = "TransactionId";
    private static final String NAMESPACE = "http://example.com/headers";
    private static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    public SoapTransactionIdInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        List<Header> headers = message.getHeaders();
        String transactionId = null;

        // Try to extract transaction ID from SOAP header
        for (Header header : headers) {
            QName qName = header.getName();
            if (TRANSACTION_ID_HEADER.equals(qName.getLocalPart())) {
                Object headerObject = header.getObject();
                if (headerObject instanceof Element) {
                    Element element = (Element) headerObject;
                    transactionId = element.getTextContent();
                    logger.debug("Extracted transaction ID from SOAP header: {}", transactionId);
                }
                break;
            }
        }

        // Generate new transaction ID if not found
        if (transactionId == null || transactionId.trim().isEmpty()) {
            transactionId = UUID.randomUUID().toString();
            logger.debug("Generated new transaction ID for SOAP request: {}", transactionId);
        }

        // Add to MDC for logging
        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId);

        logger.debug("SOAP request processing with transaction ID: {}", transactionId);
    }
}
