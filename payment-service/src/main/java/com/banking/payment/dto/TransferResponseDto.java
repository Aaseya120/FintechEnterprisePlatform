package com.banking.payment.dto;

import com.banking.payment.domain.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Fund Transfer Details")
public record TransferResponseDto(
        @Schema(description = "Transfer ID", example = "tx_9a8b7c6d5e")
        String id,

        @Schema(description = "Distributed Saga Orchestration ID", example = "saga_12345")
        String sagaId,

        @Schema(description = "Idempotency key supplied by client", example = "idemp_550e8400-e29b-41d4-a716-446655440000")
        String idempotencyKey,

        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String failureReason,
        String channel,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {}
