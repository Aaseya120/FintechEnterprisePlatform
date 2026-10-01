package com.banking.account.dto;

import com.banking.account.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(description = "Account Creation Request")
public record CreateAccountRequest(
        @NotBlank(message = "Customer ID is required")
        @Schema(description = "Customer ID to associate account with", example = "cust_987654")
        String customerId,

        @NotNull(message = "Account type is required")
        @Schema(description = "Type of account", example = "CHECKING")
        AccountType accountType,

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code")
        @Schema(description = "ISO 4217 Currency", example = "USD")
        String currency,

        @NotNull(message = "Initial deposit amount is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Initial deposit cannot be negative")
        @Schema(description = "Opening deposit amount", example = "500.00")
        BigDecimal initialDeposit
) {}
