package com.banking.exchange.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ForexDtos {

    public record CurrencyDto(
            String code,
            String name,
            String symbol,
            int decimalPlaces,
            String numericCode,
            boolean active
    ) implements Serializable {}

    public record CountryDto(
            String countryCode,
            String alpha3Code,
            String countryName,
            String dialingCode,
            String defaultCurrency,
            String ibanPattern,
            Integer ibanLength,
            String swiftPrefix,
            boolean sepa
    ) implements Serializable {}

    public record ExchangeRateDetailDto(
            String fromCurrency,
            String toCurrency,
            BigDecimal midRate,
            BigDecimal bidRate,
            BigDecimal askRate,
            BigDecimal spreadPercentage,
            BigDecimal change24hPercentage,
            Instant lastUpdatedAt
    ) implements Serializable {}

    public record FxQuoteRequest(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            String beneficiaryCountryCode
    ) implements Serializable {}

    public record FxQuoteResponse(
            String quoteId,
            String fromCurrency,
            String toCurrency,
            BigDecimal sourceAmount,
            BigDecimal targetAmount,
            BigDecimal appliedRate,
            BigDecimal feeAmount,
            BigDecimal effectiveRate,
            Instant expiresAt
    ) implements Serializable {}

    public record BeneficiaryValidationRequest(
            String countryCode,
            String currencyCode,
            String accountNumberOrIban,
            String swiftBic
    ) implements Serializable {}

    public record BeneficiaryValidationResult(
            boolean isValid,
            String countryCode,
            String currencyCode,
            String accountNumberOrIban,
            String message,
            List<String> validationErrors
    ) implements Serializable {}
}
