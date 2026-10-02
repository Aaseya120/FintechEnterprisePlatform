package com.banking.bill.dto;

import com.banking.bill.domain.BillerCategory;
import com.banking.bill.domain.BillPaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BillDtos {

    private BillDtos() {}

    public record BillerResponseDto(
            String id,
            String billerCode,
            String billerName,
            BillerCategory category,
            BigDecimal serviceFee,
            String currency,
            boolean active
    ) {}

    public record BillInquiryRequestDto(
            @NotBlank(message = "Biller code is required")
            String billerCode,

            @NotBlank(message = "Consumer number / Account number is required")
            String consumerNumber
    ) {}

    public record BillInquiryResponseDto(
            String billerCode,
            String billerName,
            String consumerNumber,
            String customerName,
            BigDecimal outstandingAmount,
            String currency,
            LocalDate dueDate,
            String billCycle
    ) {}

    public record BillPayRequestDto(
            @NotBlank(message = "Customer ID is required")
            String customerId,

            @NotBlank(message = "Source account number is required")
            @Size(min = 10, max = 34, message = "Account number must be between 10 and 34 characters")
            String sourceAccountNumber,

            @NotBlank(message = "Biller code is required")
            String billerCode,

            @NotBlank(message = "Consumer number is required")
            String consumerNumber,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            BigDecimal amount,

            @NotBlank(message = "Currency is required")
            @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
            String currency
    ) {}

    public record BillPaymentResponseDto(
            String paymentReference,
            String customerId,
            String sourceAccountNumber,
            String billerCode,
            String consumerNumber,
            BigDecimal amount,
            BigDecimal serviceFee,
            BigDecimal totalDebited,
            String currency,
            BillPaymentStatus status,
            String billerTransactionRef,
            Instant paidAt
    ) {}
}
