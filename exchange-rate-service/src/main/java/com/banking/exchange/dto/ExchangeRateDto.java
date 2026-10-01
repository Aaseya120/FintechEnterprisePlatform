package com.banking.exchange.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Foreign Exchange Rate Information")
public record ExchangeRateDto(
        @Schema(description = "Source ISO 4217 Currency", example = "USD")
        String fromCurrency,

        @Schema(description = "Target ISO 4217 Currency", example = "EUR")
        String toCurrency,

        @Schema(description = "Exchange rate multiplier", example = "0.9234")
        BigDecimal rate,

        @Schema(description = "Timestamp of FX quote")
        Instant timestamp
) implements Serializable {}
