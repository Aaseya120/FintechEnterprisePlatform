package com.banking.fraud.dto;

import com.banking.fraud.domain.FraudAlert;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class FraudDtos {

    public record FraudCheckRequestDto(
            @NotBlank String transactionId,
            @NotBlank String accountNumber,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            String clientIp,
            String location // e.g. "US-NewYork", "JP-Tokyo"
    ) implements Serializable {}

    public record FraudCheckResultDto(
            String transactionId,
            String accountNumber,
            int riskScore,
            FraudAlert.RiskDecision decision,
            List<String> triggeredRules,
            String recommendation,
            Instant evaluatedAt
    ) implements Serializable {}
}
