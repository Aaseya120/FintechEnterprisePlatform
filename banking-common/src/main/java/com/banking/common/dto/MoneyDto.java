package com.banking.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Currency;

@Schema(description = "Monetary representation ensuring precision for financial accounting")
public record MoneyDto(
        @Schema(description = "Exact monetary amount with precision", example = "1500.50")
        BigDecimal amount,

        @Schema(description = "ISO 4217 Currency Code", example = "USD")
        String currency
) {
    public MoneyDto {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null || currency.trim().length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO code");
        }
        // Validate ISO 4217 code
        Currency.getInstance(currency.toUpperCase());
    }

    public static MoneyDto of(double amount, String currency) {
        return new MoneyDto(BigDecimal.valueOf(amount), currency);
    }
}
