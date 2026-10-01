package com.banking.loan.dto;

import com.banking.loan.domain.Loan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class LoanDtos {

    public record LoanApplicationRequestDto(
            @NotBlank String customerId,
            @NotNull Loan.LoanType loanType,
            @NotNull @DecimalMin("1000.00") BigDecimal principalAmount,
            @NotNull @DecimalMin("1.00") BigDecimal annualInterestRate,
            @Min(6) int tenureMonths,
            @NotBlank String disbursementAccount
    ) implements Serializable {}

    public record EmiCalculationRequestDto(
            @NotNull @DecimalMin("1000.00") BigDecimal principalAmount,
            @NotNull @DecimalMin("1.00") BigDecimal annualInterestRate,
            @Min(6) int tenureMonths
    ) implements Serializable {}

    public record EmiCalculationResponseDto(
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            int tenureMonths,
            BigDecimal monthlyEmi,
            BigDecimal totalInterestPayable,
            BigDecimal totalPayment
    ) implements Serializable {}

    public record LoanResponseDto(
            String id,
            String loanAccountNumber,
            String customerId,
            Loan.LoanType loanType,
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            int tenureMonths,
            BigDecimal emiAmount,
            Loan.LoanStatus status,
            String disbursementAccount,
            Instant createdAt
    ) implements Serializable {}

    public record RepaymentScheduleItemDto(
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal principalComponent,
            BigDecimal interestComponent,
            BigDecimal totalInstallment,
            BigDecimal remainingBalance,
            String status
    ) implements Serializable {}
}
