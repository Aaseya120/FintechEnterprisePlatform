package com.banking.payment.dto;

import com.banking.payment.domain.StandingInstruction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class StandingInstructionDtos {

    public record CreateStandingInstructionRequestDto(
            @NotBlank(message = "Instruction name is required")
            String instructionName,

            @NotBlank(message = "Customer ID is required")
            String customerId,

            @NotBlank(message = "Source account number is required")
            String sourceAccountNumber,

            @NotBlank(message = "Target account number is required")
            String targetAccountNumber,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "1.00", message = "Minimum recurring amount is 1.00")
            BigDecimal amount,

            @NotBlank(message = "Currency is required")
            String currency,

            @NotNull(message = "Frequency is required")
            StandingInstruction.ScheduleFrequency frequency,

            Integer executionDay,

            LocalDate startDate,

            @NotNull(message = "Payment category is required")
            StandingInstruction.PaymentCategory category
    ) {}

    public record StandingInstructionResponseDto(
            String id,
            String instructionName,
            String customerId,
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            String currency,
            StandingInstruction.ScheduleFrequency frequency,
            Integer executionDay,
            LocalDate nextExecutionDate,
            StandingInstruction.PaymentCategory category,
            StandingInstruction.InstructionStatus status,
            int totalExecutionsCount,
            Instant lastExecutedAt,
            Instant createdAt
    ) {
        public static StandingInstructionResponseDto fromEntity(StandingInstruction si) {
            return new StandingInstructionResponseDto(
                    si.getId(),
                    si.getInstructionName(),
                    si.getCustomerId(),
                    si.getSourceAccountNumber(),
                    si.getTargetAccountNumber(),
                    si.getAmount(),
                    si.getCurrency(),
                    si.getFrequency(),
                    si.getExecutionDay(),
                    si.getNextExecutionDate(),
                    si.getCategory(),
                    si.getStatus(),
                    si.getTotalExecutionsCount(),
                    si.getLastExecutedAt(),
                    si.getCreatedAt()
            );
        }
    }

    public record UpdateInstructionStatusRequestDto(
            @NotNull(message = "Status is required")
            StandingInstruction.InstructionStatus status
    ) {}
}
