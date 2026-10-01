package com.banking.payment.gateway;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class GatewayDtos {

    @Schema(description = "Universal Payment Gateway Request")
    public record PaymentGatewayRequest(
            @NotNull PaymentChannel channel,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull String currency,
            String sourceAccountOrVpaOrCard,
            String targetAccountOrVpaOrMerchant,
            Map<String, String> metadata // Card CVV/Expiry or UPI Pin or PayPal Auth
    ) implements Serializable {}

    @Schema(description = "Payment Gateway Processing Result")
    public record PaymentGatewayResponse(
            String transactionReference,
            PaymentChannel channel,
            BigDecimal amount,
            String currency,
            GatewayStatus status,
            String gatewayMessage,
            String settlementReference,
            Instant timestamp
    ) implements Serializable {}

    public enum GatewayStatus {
        SUCCESS,
        PENDING_3DS,
        PROCESSING,
        FAILED
    }
}
