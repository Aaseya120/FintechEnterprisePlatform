package com.banking.account.middleware;

import com.banking.account.middleware.LegacyCbsDtos.*;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/middleware/cbs")
@Tag(name = "Legacy Core Banking SOA Middleware API",
     description = "Enterprise SOA Middleware Gateway connecting modern microservices to Legacy CBS (Finacle, Flexcube, IBM CICS) via SOAP XML & Canonical Translation")
public class LegacyCbsMiddlewareController {

    private final LegacyCbsMiddlewareGateway cbsGateway;

    public LegacyCbsMiddlewareController(LegacyCbsMiddlewareGateway cbsGateway) {
        this.cbsGateway = cbsGateway;
    }

    @GetMapping("/accounts/{accountNumber}")
    @Operation(summary = "Query Account from Legacy CBS via SOA Middleware",
               description = "Dispatches SOAP XML inquiry to the legacy mainframe, parses response, and returns canonical JSON")
    public ResponseEntity<ApiResponse<CbsAccountInquiryResponse>> queryLegacyAccount(
            @PathVariable String accountNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CbsAccountInquiryResponse response = cbsGateway.queryAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @PostMapping("/post")
    @Operation(summary = "Post Financial Transaction into Legacy CBS General Ledger",
               description = "Performs double-entry ledger posting to legacy mainframe with circuit breaker and rollback safety")
    public ResponseEntity<ApiResponse<CbsPostingResponse>> postTransaction(
            @RequestBody CbsPostingRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CbsPostingResponse response = cbsGateway.postTransaction(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction posted to Legacy CBS", corrId));
    }

    @PostMapping("/holds")
    @Operation(summary = "Place Fund Reservation Hold on Legacy CBS")
    public ResponseEntity<ApiResponse<CbsHoldResponse>> placeHold(
            @RequestBody CbsHoldRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CbsHoldResponse response = cbsGateway.placeHold(request);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @GetMapping("/health")
    @Operation(summary = "Check Legacy CBS Mainframe Connectivity Status")
    public ResponseEntity<ApiResponse<CbsHealthCheckResponse>> checkHealth(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CbsHealthCheckResponse health = cbsGateway.checkConnectivity();
        return ResponseEntity.ok(ApiResponse.success(health, corrId));
    }
}
