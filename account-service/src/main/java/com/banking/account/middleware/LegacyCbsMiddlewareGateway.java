package com.banking.account.middleware;

import com.banking.account.middleware.LegacyCbsDtos.*;

/**
 * SOA Integration Middleware Contract:
 * Mediates between modern Spring Boot microservices and the Legacy Core Banking System (CBS).
 * Standardizes communication, converts canonical JSON payloads to SOAP XML, handles network faults,
 * and maintains continuous synchronization between legacy mainframes and modern digital channels.
 */
public interface LegacyCbsMiddlewareGateway {

    CbsAccountInquiryResponse queryAccount(String accountNumber);

    CbsPostingResponse postTransaction(CbsPostingRequest request);

    CbsHoldResponse placeHold(CbsHoldRequest request);

    CbsHealthCheckResponse checkConnectivity();
}
