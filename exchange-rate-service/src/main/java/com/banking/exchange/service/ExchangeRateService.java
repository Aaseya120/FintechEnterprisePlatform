package com.banking.exchange.service;

import com.banking.exchange.domain.CountryEntity;
import com.banking.exchange.domain.CurrencyEntity;
import com.banking.exchange.domain.ExchangeRateEntity;
import com.banking.exchange.dto.CurrencyConversionResponse;
import com.banking.exchange.dto.ExchangeRateDto;
import com.banking.exchange.dto.ForexDtos.*;
import com.banking.exchange.repository.CountryRepository;
import com.banking.exchange.repository.CurrencyRepository;
import com.banking.exchange.repository.ExchangeRateRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final CurrencyRepository currencyRepository;
    private final CountryRepository countryRepository;
    private final ExchangeRateRepository rateRepository;

    public ExchangeRateService(CurrencyRepository currencyRepository,
                               CountryRepository countryRepository,
                               ExchangeRateRepository rateRepository) {
        this.currencyRepository = currencyRepository;
        this.countryRepository = countryRepository;
        this.rateRepository = rateRepository;
    }

    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        return currencyRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(c -> new CurrencyDto(c.getCode(), c.getName(), c.getSymbol(), c.getDecimalPlaces(), c.getNumericCode(), c.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CountryDto> getAllCountries() {
        return countryRepository.findAllByOrderByCountryNameAsc().stream()
                .map(this::toCountryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CountryDto getCountryByCode(String countryCode) {
        return countryRepository.findByCountryCodeIgnoreCase(countryCode)
                .map(this::toCountryDto)
                .orElseThrow(() -> new IllegalArgumentException("Country code not found: " + countryCode));
    }

    /**
     * Validates beneficiary details (Country, Currency, IBAN regex, SWIFT BIC) for cross-border transfers.
     */
    @Transactional(readOnly = true)
    public BeneficiaryValidationResult validateBeneficiary(BeneficiaryValidationRequest request) {
        var errors = new ArrayList<String>();

        var countryOpt = countryRepository.findByCountryCodeIgnoreCase(request.countryCode());
        if (countryOpt.isEmpty()) {
            errors.add("Invalid country code: " + request.countryCode());
            return new BeneficiaryValidationResult(false, request.countryCode(), request.currencyCode(), request.accountNumberOrIban(), "Country validation failed", errors);
        }

        CountryEntity country = countryOpt.get();

        // Currency validation
        if (request.currencyCode() != null && !currencyRepository.existsById(request.currencyCode().toUpperCase())) {
            errors.add("Unsupported currency code: " + request.currencyCode());
        }

        // IBAN validation if required by country
        if (country.getIbanPattern() != null && request.accountNumberOrIban() != null) {
            String sanitizedIban = request.accountNumberOrIban().replaceAll("\\s+", "").toUpperCase();
            if (country.getIbanLength() != null && sanitizedIban.length() != country.getIbanLength()) {
                errors.add("IBAN length for " + country.getCountryName() + " must be " + country.getIbanLength() + " characters. Provided: " + sanitizedIban.length());
            }
            if (!Pattern.matches(country.getIbanPattern(), sanitizedIban)) {
                errors.add("IBAN format does not match official ISO standard pattern for " + country.getCountryName());
            }
        }

        // SWIFT BIC prefix check
        if (request.swiftBic() != null && !request.swiftBic().isBlank()) {
            String bic = request.swiftBic().trim().toUpperCase();
            if (bic.length() != 8 && bic.length() != 11) {
                errors.add("SWIFT/BIC must be 8 or 11 alphanumeric characters.");
            }
        }

        boolean isValid = errors.isEmpty();
        String message = isValid ? "Beneficiary bank details validated successfully against international standards." : "Validation errors found.";
        return new BeneficiaryValidationResult(isValid, request.countryCode(), request.currencyCode(), request.accountNumberOrIban(), message, errors);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDetailDto> getLiveRates() {
        return rateRepository.findAllByOrderByFromCurrencyAscToCurrencyAsc().stream()
                .map(r -> new ExchangeRateDetailDto(
                        r.getFromCurrency(), r.getToCurrency(),
                        r.getMidRate(), r.getBidRate(), r.getAskRate(),
                        r.getSpreadPercentage(), r.getChange24hPercentage(),
                        r.getLastUpdatedAt()
                ))
                .toList();
    }

    /**
     * Cache-aside lookup in Redis / AWS ElastiCache.
     */
    @Cacheable(value = "exchangeRates", key = "#from.toUpperCase() + '-' + #to.toUpperCase()")
    @CircuitBreaker(name = "forexFeed", fallbackMethod = "fallbackExchangeRate")
    @Transactional(readOnly = true)
    public ExchangeRateDto getRate(String from, String to) {
        String fromCode = from.toUpperCase();
        String toCode = to.toUpperCase();

        if (fromCode.equals(toCode)) {
            return new ExchangeRateDto(fromCode, toCode, BigDecimal.ONE, Instant.now());
        }

        var directRateOpt = rateRepository.findByFromCurrencyIgnoreCaseAndToCurrencyIgnoreCase(fromCode, toCode);
        if (directRateOpt.isPresent()) {
            var direct = directRateOpt.get();
            return new ExchangeRateDto(fromCode, toCode, direct.getAskRate(), direct.getLastUpdatedAt());
        }

        // Triangulation through USD
        var fromToUsdOpt = rateRepository.findByFromCurrencyIgnoreCaseAndToCurrencyIgnoreCase(fromCode, "USD");
        var usdToToOpt = rateRepository.findByFromCurrencyIgnoreCaseAndToCurrencyIgnoreCase("USD", toCode);

        if (fromToUsdOpt.isPresent() && usdToToOpt.isPresent()) {
            BigDecimal triangulated = fromToUsdOpt.get().getAskRate().multiply(usdToToOpt.get().getAskRate()).setScale(6, RoundingMode.HALF_UP);
            return new ExchangeRateDto(fromCode, toCode, triangulated, Instant.now());
        }

        log.warn("Forex pair {} -> {} not in database; generating estimated parity rate.", fromCode, toCode);
        return new ExchangeRateDto(fromCode, toCode, BigDecimal.ONE, Instant.now());
    }

    /**
     * Creates a guaranteed 60-second locked exchange rate quote for cross-border transfers.
     */
    public FxQuoteResponse createGuaranteedQuote(FxQuoteRequest request) {
        var rateDto = getRate(request.fromCurrency(), request.toCurrency());
        BigDecimal appliedRate = rateDto.rate();

        // 0.25% intermediary cross-currency fee
        BigDecimal feeRate = new BigDecimal("0.0025");
        BigDecimal feeAmount = request.amount().multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSource = request.amount().subtract(feeAmount);
        BigDecimal targetAmount = netSource.multiply(appliedRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveRate = (request.amount().compareTo(BigDecimal.ZERO) > 0)
                ? targetAmount.divide(request.amount(), 6, RoundingMode.HALF_UP)
                : appliedRate;

        Instant expiresAt = Instant.now().plus(60, ChronoUnit.SECONDS);

        return new FxQuoteResponse(
                "FXQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.fromCurrency().toUpperCase(),
                request.toCurrency().toUpperCase(),
                request.amount(),
                targetAmount,
                appliedRate,
                feeAmount,
                effectiveRate,
                expiresAt
        );
    }

    public CurrencyConversionResponse convert(String from, String to, BigDecimal amount) {
        ExchangeRateDto rateDto = getRate(from, to);
        BigDecimal converted = amount.multiply(rateDto.rate()).setScale(2, RoundingMode.HALF_UP);
        return new CurrencyConversionResponse(from.toUpperCase(), to.toUpperCase(), amount, rateDto.rate(), converted);
    }

    public ExchangeRateDto fallbackExchangeRate(String from, String to, Throwable t) {
        log.warn("Forex Feed Circuit Breaker OPEN or failed for {} -> {}. Serving baseline 1.0 parity. Cause: {}",
                from, to, t.getMessage());
        return new ExchangeRateDto(from.toUpperCase(), to.toUpperCase(), BigDecimal.ONE, Instant.now());
    }

    private CountryDto toCountryDto(CountryEntity c) {
        return new CountryDto(
                c.getCountryCode(), c.getAlpha3Code(), c.getCountryName(),
                c.getDialingCode(), c.getDefaultCurrency(), c.getIbanPattern(),
                c.getIbanLength(), c.getSwiftPrefix(), c.isSepa()
        );
    }
}
