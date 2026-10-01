package com.banking.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Fund Transfer Request")
public record TransferRequestDto(
        @NotBlank(message = "Source account number is required")
        @Schema(description = "Debited account number", example = "US1000000001")
        String sourceAccountNumber,

        @NotBlank(message = "Target account number is required")
        @Schema(description = "Credited account number", example = "US2000000002")
        String targetAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Transfer amount must be at least 0.01")
        @Schema(description = "Monetary transfer amount", example = "250.00")
        BigDecimal amount,

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code")
        @Schema(description = "ISO 4217 Currency", example = "USD")
        String currency
) implements Serializable {}
