package com.banking.payment.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.common.idempotency.Idempotent;
import com.banking.payment.dto.StandingInstructionDtos.*;
import com.banking.payment.dto.TransferRequestDto;
import com.banking.payment.dto.TransferResponseDto;
import com.banking.payment.gateway.GatewayDtos.*;
import com.banking.payment.gateway.PaymentGatewayManager;
import com.banking.payment.saga.TransferSagaOrchestrator;
import com.banking.payment.service.StandingInstructionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for real-time payments, distributed Saga fund transfers,
 * multi-rail payment gateway routing, and recurring standing instructions.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransferSagaOrchestrator sagaOrchestrator;
    private final PaymentGatewayManager gatewayManager;
    private final StandingInstructionService standingInstructionService;

    public PaymentController(TransferSagaOrchestrator sagaOrchestrator,
                             PaymentGatewayManager gatewayManager,
                             StandingInstructionService standingInstructionService) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.gatewayManager = gatewayManager;
        this.standingInstructionService = standingInstructionService;
    }

    /**
     * Routes multi-channel payments (UPI, Card, NetBanking, PayPal).
     */
    @PostMapping("/process")
    @Idempotent(headerName = "Idempotency-Key", ttl = 24)
    public ResponseEntity<ApiResponse<PaymentGatewayResponse>> processGatewayPayment(
            @Valid @RequestBody PaymentGatewayRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        PaymentGatewayResponse response = gatewayManager.dispatchPayment(request, corrId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment gateway transaction authorized", corrId));
    }

    /**
     * Executes distributed Saga fund transfer between accounts with idempotency protection.
     */
    @PostMapping("/transfer")
    @Idempotent(headerName = "Idempotency-Key", ttl = 24)
    public ResponseEntity<ApiResponse<TransferResponseDto>> transfer(
            @Valid @RequestBody TransferRequestDto request,
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

    /**
     * Configures a recurring standing instruction or auto-debit sweep.
     */
    @PostMapping("/standing-instructions")
    public ResponseEntity<ApiResponse<StandingInstructionResponseDto>> createStandingInstruction(
            @Valid @RequestBody CreateStandingInstructionRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        StandingInstructionResponseDto response = standingInstructionService.createStandingInstruction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Standing instruction configured successfully", corrId));
    }

    /**
     * Lists active or paused recurring standing instructions for a customer.
     */
    @GetMapping("/standing-instructions/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<StandingInstructionResponseDto>>> getCustomerStandingInstructions(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<StandingInstructionResponseDto> list = standingInstructionService.getCustomerInstructions(customerId);
        return ResponseEntity.ok(ApiResponse.success(list, "Customer standing instructions retrieved", corrId));
    }

    /**
     * Pauses, resumes, or cancels a standing instruction.
     */
    @PutMapping("/standing-instructions/{id}/status")
    public ResponseEntity<ApiResponse<StandingInstructionResponseDto>> updateInstructionStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateInstructionStatusRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        StandingInstructionResponseDto updated = standingInstructionService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.success(updated, "Standing instruction status updated", corrId));
    }
}
