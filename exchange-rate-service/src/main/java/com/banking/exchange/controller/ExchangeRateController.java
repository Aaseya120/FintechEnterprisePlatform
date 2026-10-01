package com.banking.exchange.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.exchange.domain.ExchangeRateEntity;
import com.banking.exchange.dto.CurrencyConversionResponse;
import com.banking.exchange.dto.ExchangeRateDto;
import com.banking.exchange.dto.ForexDtos.*;
import com.banking.exchange.service.DynamicForexRateEngine;
import com.banking.exchange.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@Tag(name = "Global Currencies & Dynamic Exchange Rates API",
     description = "ISO-4217 Currencies, ISO-3166 Countries, Beneficiary IBAN/SWIFT Validation, Dynamic Interbank FX Market Tickers, and Guaranteed Cross-Currency Quotes")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final DynamicForexRateEngine dynamicRateEngine;

    public ExchangeRateController(ExchangeRateService exchangeRateService,
                                  DynamicForexRateEngine dynamicRateEngine) {
        this.exchangeRateService = exchangeRateService;
        this.dynamicRateEngine = dynamicRateEngine;
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get All ISO-4217 Global Currencies",
               description = "Returns all active world currencies with symbols, decimal precision, and numeric codes")
    public ResponseEntity<ApiResponse<List<CurrencyDto>>> getAllCurrencies(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<CurrencyDto> currencies = exchangeRateService.getAllCurrencies();
        return ResponseEntity.ok(ApiResponse.success(currencies, corrId));
    }

    @GetMapping("/countries")
    @Operation(summary = "Get All World Countries with International Banking Specifications",
               description = "Returns countries with ISO Alpha-2/3, dialing codes, default currencies, IBAN regex formats, and SWIFT BIC prefixes")
    public ResponseEntity<ApiResponse<List<CountryDto>>> getAllCountries(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<CountryDto> countries = exchangeRateService.getAllCountries();
        return ResponseEntity.ok(ApiResponse.success(countries, corrId));
    }

    @GetMapping("/countries/{code}")
    @Operation(summary = "Get Country Details and IBAN/SWIFT Validation Rules by Code")
    public ResponseEntity<ApiResponse<CountryDto>> getCountryByCode(
            @PathVariable String code,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CountryDto country = exchangeRateService.getCountryByCode(code);
        return ResponseEntity.ok(ApiResponse.success(country, corrId));
    }

    @PostMapping("/validate-beneficiary")
    @Operation(summary = "Validate International Beneficiary Bank Account & Country",
               description = "Validates IBAN checksum, length, and regex matching per country ISO rules, as well as SWIFT/BIC syntax")
    public ResponseEntity<ApiResponse<BeneficiaryValidationResult>> validateBeneficiary(
            @RequestBody BeneficiaryValidationRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BeneficiaryValidationResult result = exchangeRateService.validateBeneficiary(request);
        return ResponseEntity.ok(ApiResponse.success(result, corrId));
    }

    @GetMapping("/live")
    @Operation(summary = "Get Live Interbank Exchange Rates with Bid/Ask Spreads",
               description = "Returns spot market FX rates including Mid rate, Bid (buy), Ask (sell), Spread %, and 24-hour change")
    public ResponseEntity<ApiResponse<List<ExchangeRateDetailDto>>> getLiveRates(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<ExchangeRateDetailDto> rates = exchangeRateService.getLiveRates();
        return ResponseEntity.ok(ApiResponse.success(rates, corrId));
    }

    @PostMapping("/quote")
    @Operation(summary = "Create Guaranteed 60-Second FX Rate Quote for Fund Transfer",
               description = "Locks in an effective conversion rate and computes foreign exchange fees for cross-border remittance")
    public ResponseEntity<ApiResponse<FxQuoteResponse>> createQuote(
            @RequestBody FxQuoteRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        FxQuoteResponse quote = exchangeRateService.createGuaranteedQuote(request);
        return ResponseEntity.ok(ApiResponse.success(quote, "Guaranteed FX quote generated", corrId));
    }

    @PostMapping("/fluctuate")
    @Operation(summary = "Simulate Real-Time Dynamic Market Fluctuation Tick",
               description = "Triggers interbank Brownian motion rate change, recalculates Bid/Ask, records history, and invalidates Redis cache")
    public ResponseEntity<ApiResponse<List<ExchangeRateEntity>>> triggerRateFluctuation(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<ExchangeRateEntity> updated = dynamicRateEngine.fluctuateAllRates("MANUAL_SIMULATION_TICK");
        return ResponseEntity.ok(ApiResponse.success(updated, "Dynamic market tick completed; Redis cache evicted", corrId));
    }

    @GetMapping
    @Operation(summary = "Get FX Exchange Rate", description = "Returns cached FX conversion rate from Redis/ElastiCache")
    public ResponseEntity<ApiResponse<ExchangeRateDto>> getRate(
            @Parameter(example = "USD") @RequestParam String from,
            @Parameter(example = "EUR") @RequestParam String to,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        ExchangeRateDto rate = exchangeRateService.getRate(from, to);
        return ResponseEntity.ok(ApiResponse.success(rate, corrId));
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert Currency Amount", description = "Computes converted monetary sum using real-time cached FX rate")
    public ResponseEntity<ApiResponse<CurrencyConversionResponse>> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CurrencyConversionResponse response = exchangeRateService.convert(from, to, amount);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }
}
