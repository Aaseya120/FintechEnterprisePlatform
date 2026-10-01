package com.banking.account.dto;

import com.banking.account.domain.SavingVault;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class SavingVaultDtos {

    public record CreateSavingVaultRequestDto(
            @NotBlank(message = "Customer ID is required") String customerId,
            @NotBlank(message = "Parent account number is required") String parentAccountNumber,
            @NotBlank(message = "Vault name is required") String vaultName,
            @NotNull @DecimalMin("1.00") BigDecimal targetAmount,
            @NotBlank String currency,
            LocalDate targetDate,
            Boolean autoRoundupEnabled
    ) implements Serializable {}

    public record VaultTransferRequestDto(
            @NotNull @DecimalMin("1.00") BigDecimal amount
    ) implements Serializable {}

    public record SavingVaultResponseDto(
            String id,
            String customerId,
            String parentAccountNumber,
            String vaultName,
            BigDecimal targetAmount,
            BigDecimal currentBalance,
            String currency,
            LocalDate targetDate,
            SavingVault.LockStatus lockStatus,
            boolean autoRoundupEnabled,
            double progressPercentage,
            Instant createdAt,
            Instant updatedAt
    ) implements Serializable {
        public static SavingVaultResponseDto fromEntity(SavingVault v) {
            double progress = 0.0;
            if (v.getTargetAmount() != null && v.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                progress = v.getCurrentBalance().divide(v.getTargetAmount(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                progress = Math.min(progress, 100.0);
            }

            return new SavingVaultResponseDto(
                    v.getId(),
                    v.getCustomerId(),
                    v.getParentAccountNumber(),
                    v.getVaultName(),
                    v.getTargetAmount(),
                    v.getCurrentBalance(),
                    v.getCurrency(),
                    v.getTargetDate(),
                    v.getLockStatus(),
                    v.isAutoRoundupEnabled(),
                    progress,
                    v.getCreatedAt(),
                    v.getUpdatedAt()
            );
        }
    }
}
