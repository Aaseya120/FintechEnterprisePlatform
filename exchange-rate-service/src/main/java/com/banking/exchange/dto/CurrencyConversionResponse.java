package com.banking.exchange.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(description = "Currency Conversion Calculation Result")
public record CurrencyConversionResponse(
        String fromCurrency,
        String toCurrency,
        BigDecimal originalAmount,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount
) implements Serializable {}
