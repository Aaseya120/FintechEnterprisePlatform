package com.banking.payment.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.common.idempotency.Idempotent;
import com.banking.payment.dto.TransferRequestDto;
import com.banking.payment.dto.TransferResponseDto;
import com.banking.payment.saga.TransferSagaOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments API", description = "High-throughput Financial Fund Transfers with Saga Orchestration & Idempotency")
public class PaymentController {

    private final TransferSagaOrchestrator sagaOrchestrator;
    private final com.banking.payment.gateway.PaymentGatewayManager gatewayManager;

    public PaymentController(TransferSagaOrchestrator sagaOrchestrator,
                             com.banking.payment.gateway.PaymentGatewayManager gatewayManager) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.gatewayManager = gatewayManager;
    }

    @PostMapping("/process")
    @Idempotent(headerName = "Idempotency-Key", ttl = 24)
    @Operation(summary = "Process Multi-Channel Payment",
               description = "Routes and processes payment through UPI, Card (Luhn/3DS), NetBanking (IMPS, NEFT, RTGS), or PayPal")
    public ResponseEntity<ApiResponse<com.banking.payment.gateway.GatewayDtos.PaymentGatewayResponse>> processGatewayPayment(
            @Valid @RequestBody com.banking.payment.gateway.GatewayDtos.PaymentGatewayRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        com.banking.payment.gateway.GatewayDtos.PaymentGatewayResponse response =
                gatewayManager.dispatchPayment(request, corrId);

        return ResponseEntity.ok(ApiResponse.success(response, "Payment gateway transaction authorized", corrId));
    }

    @PostMapping("/transfer")
    @Idempotent(headerName = "Idempotency-Key", ttl = 24)
    @Operation(
            summary = "Execute financial fund transfer",
            description = "Transfers money between accounts with distributed Saga Orchestration, atomic outbox publishing, and strict Idempotency protection against duplicate network requests"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transfer executed or returned from idempotency cache",
                    headers = {
                            @Header(name = "X-Correlation-ID", description = "Distributed trace correlation ID", schema = @Schema(type = "string")),
                            @Header(name = "Idempotency-Key", description = "Echoed idempotency key", schema = @Schema(type = "string"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or missing mandatory Idempotency-Key"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate transaction currently in-flight"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation failure or insufficient funds")
    })
    public ResponseEntity<ApiResponse<TransferResponseDto>> transfer(
            @Valid @RequestBody TransferRequestDto request,
            @Parameter(description = "UUID v4 Idempotency Key ensuring exactly-once processing", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "X-Channel", required = false, defaultValue = "WEB") String channel) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();

        TransferResponseDto result = sagaOrchestrator.executeTransferSaga(
                request,
                idempotencyKey,
                channel,
                corrId
        );

        return ResponseEntity.ok(ApiResponse.success(result, "Transfer processed successfully", corrId));
    }
}
