package com.banking.payment.client;

import com.banking.common.exception.BankingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Component
public class AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);
    private final RestTemplate restTemplate;
    private final String accountServiceUrl;

    public AccountClient(RestTemplateBuilder builder,
                         @Value("${account.service.url:http://localhost:8081}") String accountServiceUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(2000))
                .setReadTimeout(Duration.ofMillis(3000))
                .build();
        this.accountServiceUrl = accountServiceUrl;
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "debitFallback")
    @Retry(name = "accountService")
    public boolean debit(String accountNumber, BigDecimal amount, String correlationId) {
        log.info("Calling AccountService DEBIT for {} amount: {} [corr: {}]", accountNumber, amount, correlationId);
        String url = accountServiceUrl + "/api/v1/accounts/" + accountNumber + "/debit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-ID", correlationId);

        HttpEntity<Map<String, BigDecimal>> request = new HttpEntity<>(Map.of("amount", amount), headers);
        restTemplate.postForEntity(url, request, String.class);
        return true;
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "creditFallback")
    @Retry(name = "accountService")
    public boolean credit(String accountNumber, BigDecimal amount, String correlationId) {
        log.info("Calling AccountService CREDIT for {} amount: {} [corr: {}]", accountNumber, amount, correlationId);
        String url = accountServiceUrl + "/api/v1/accounts/" + accountNumber + "/credit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-ID", correlationId);

        HttpEntity<Map<String, BigDecimal>> request = new HttpEntity<>(Map.of("amount", amount), headers);
        restTemplate.postForEntity(url, request, String.class);
        return true;
    }

    // Fallback handlers to isolate downstream failures and prevent cascading collapse
    public boolean debitFallback(String accountNumber, BigDecimal amount, String correlationId, Throwable t) {
        log.error("Resilience4j Fallback triggered on DEBIT for account {}! Cause: {}", accountNumber, t.getMessage());
        throw new BankingException("ACCOUNT_SERVICE_UNAVAILABLE",
                "Account microservice is currently unreachable. Transaction halted by circuit breaker.",
                HttpStatus.SERVICE_UNAVAILABLE, t);
    }

    public boolean creditFallback(String accountNumber, BigDecimal amount, String correlationId, Throwable t) {
        log.error("Resilience4j Fallback triggered on CREDIT for account {}! Cause: {}", accountNumber, t.getMessage());
        throw new BankingException("ACCOUNT_SERVICE_UNAVAILABLE",
                "Account microservice credit failed. Initiating Saga compensation.",
                HttpStatus.SERVICE_UNAVAILABLE, t);
    }
}
