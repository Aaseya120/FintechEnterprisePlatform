package com.banking.account.dto;

import com.banking.account.domain.TermDeposit.CompoundingFrequency;
import com.banking.account.domain.TermDeposit.TermDepositStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class TermDepositDtos {

    public record OpenTermDepositRequest(
            @NotBlank String customerId,
            @NotBlank String linkedAccountNumber,
            @NotNull @DecimalMin("500.00") BigDecimal principalAmount,
            @NotBlank String currency,
            @Min(3) int tenorMonths,
            CompoundingFrequency compoundingFrequency,
            boolean autoRenewal
    ) implements Serializable {}

    public record TermDepositResponse(
            String id,
            String depositNumber,
            String customerId,
            String linkedAccountNumber,
            BigDecimal principalAmount,
            String currency,
            BigDecimal interestRate,
            int tenorMonths,
            CompoundingFrequency compoundingFrequency,
            BigDecimal maturityAmount,
            LocalDate maturityDate,
            TermDepositStatus status,
            boolean autoRenewal,
            Instant createdAt
    ) implements Serializable {}

    public record TermDepositLiquidationResponse(
            String depositNumber,
            BigDecimal principalRefunded,
            BigDecimal interestPaid,
            BigDecimal penaltyDeducted,
            BigDecimal netPayout,
            String creditedAccountNumber,
            Instant liquidatedAt
    ) implements Serializable {}
}
