package com.banking.account.middleware;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Enterprise SOAP XML Envelope Builder:
 * Marshals modern microservice requests into standardized SOAP XML with WS-Security headers
 * for communication with legacy Core Banking mainframe adapters (Finacle, Flexcube, IBM CICS).
 */
@Component
public class CbsSoapEnvelopeBuilder {

    private static final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String CBS_NS = "http://banking.com/cbs/soap";

    public String buildInquirySoapEnvelope(String accountNumber, String correlationId) {
        String msgId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="%s" xmlns:cbs="%s">
                <soapenv:Header>
                    <cbs:SecurityHeader>
                        <cbs:MessageId>%s</cbs:MessageId>
                        <cbs:Timestamp>%s</cbs:Timestamp>
                        <cbs:ChannelId>MICROSERVICES_SOA_BRIDGE</cbs:ChannelId>
                    </cbs:SecurityHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <cbs:GetAccountBalanceRequest>
                        <cbs:accountNumber>%s</cbs:accountNumber>
                    </cbs:GetAccountBalanceRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(SOAP_ENV_NS, CBS_NS, msgId, Instant.now().toString(), accountNumber);
    }

    public String buildPostingSoapEnvelope(String txnRef, String sourceAccount, String targetAccount,
                                           BigDecimal amount, String currency, String narration) {
        String msgId = UUID.randomUUID().toString();
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="%s" xmlns:cbs="%s">
                <soapenv:Header>
                    <cbs:SecurityHeader>
                        <cbs:MessageId>%s</cbs:MessageId>
                        <cbs:Timestamp>%s</cbs:Timestamp>
                        <cbs:ChannelId>MICROSERVICES_SOA_BRIDGE</cbs:ChannelId>
                    </cbs:SecurityHeader>
                </soapenv:Header>
                <soapenv:Body>
                    <cbs:PostTransactionRequest>
                        <cbs:transactionRef>%s</cbs:transactionRef>
                        <cbs:sourceAccount>%s</cbs:sourceAccount>
                        <cbs:targetAccount>%s</cbs:targetAccount>
                        <cbs:amount>%s</cbs:amount>
                        <cbs:currency>%s</cbs:currency>
                        <cbs:narration>%s</cbs:narration>
                    </cbs:PostTransactionRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(SOAP_ENV_NS, CBS_NS, msgId, Instant.now().toString(),
                txnRef, sourceAccount, targetAccount, amount.toPlainString(), currency, narration);
    }
}
