package com.banking.account.dto;

import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Account details model")
public record AccountResponseDto(
        @Schema(description = "Internal unique UUID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        String id,

        @Schema(description = "IBAN / Core Banking Account Number", example = "US12BANK00000123456789")
        String accountNumber,

        @Schema(description = "Customer ID", example = "cust_987654")
        String customerId,

        @Schema(description = "Account Type", example = "CHECKING")
        AccountType accountType,

        @Schema(description = "ISO 4217 Currency", example = "USD")
        String currency,

        @Schema(description = "Ledger Balance", example = "12500.50")
        BigDecimal balance,

        @Schema(description = "Available Balance for immediate withdrawal/transfer", example = "12500.50")
        BigDecimal availableBalance,

        @Schema(description = "Account Status", example = "ACTIVE")
        AccountStatus status,

        @Schema(description = "Entity version for optimistic locking")
        Long version,

        Instant createdAt,
        Instant updatedAt
) implements Serializable {}
