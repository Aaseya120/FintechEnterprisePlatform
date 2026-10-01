package com.banking.account.controller;

import com.banking.account.domain.AccountType;
import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.middleware.LegacyCbsDtos.*;
import com.banking.account.middleware.LegacyCbsMiddlewareGateway;
import com.banking.account.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AccountGraphQLController {

    private final AccountService accountService;
    private final LegacyCbsMiddlewareGateway cbsGateway;

    public AccountGraphQLController(AccountService accountService,
                                  LegacyCbsMiddlewareGateway cbsGateway) {
        this.accountService = accountService;
        this.cbsGateway = cbsGateway;
    }

    @QueryMapping
    public AccountResponseDto accountByNumber(@Argument String accountNumber) {
        return accountService.getAccountByNumber(accountNumber);
    }

    @QueryMapping
    public List<AccountResponseDto> accountsByCustomer(@Argument String customerId) {
        return accountService.getAccountsByCustomer(customerId);
    }

    @QueryMapping
    public Map<String, Object> accountBalance(@Argument String accountNumber) {
        BigDecimal balance = accountService.getAvailableBalance(accountNumber);
        return Map.of("accountNumber", accountNumber, "availableBalance", balance.doubleValue());
    }

    @QueryMapping
    public CbsAccountInquiryResponse legacyCbsAccount(@Argument String accountNumber) {
        return cbsGateway.queryAccount(accountNumber);
    }

    @QueryMapping
    public CbsHealthCheckResponse legacyCbsHealth() {
        return cbsGateway.checkConnectivity();
    }

    @MutationMapping
    public AccountResponseDto openAccount(@Argument OpenAccountInput input) {
        CreateAccountRequest request = new CreateAccountRequest(
                input.customerId(),
                input.accountType(),
                input.currency(),
                BigDecimal.valueOf(input.initialDeposit())
        );
        return accountService.createAccount(request);
    }

    @MutationMapping
    public AccountResponseDto debitAccount(@Argument String accountNumber, @Argument Float amount) {
        return accountService.debitAccount(
                accountNumber,
                BigDecimal.valueOf(amount),
                UUID.randomUUID().toString(),
                "GRAPHQL_CLIENT"
        );
    }

    @MutationMapping
    public AccountResponseDto creditAccount(@Argument String accountNumber, @Argument Float amount) {
        return accountService.creditAccount(
                accountNumber,
                BigDecimal.valueOf(amount),
                UUID.randomUUID().toString(),
                "GRAPHQL_CLIENT"
        );
    }

    @MutationMapping
    public CbsPostingResponse postLegacyCbsTransaction(@Argument LegacyCbsPostInput input) {
        CbsPostingRequest request = new CbsPostingRequest(
                input.transactionRef(),
                input.sourceAccount(),
                input.targetAccount(),
                BigDecimal.valueOf(input.amount()),
                input.currency(),
                input.narration(),
                "GRAPHQL_SOA_MIDDLEWARE"
        );
        return cbsGateway.postTransaction(request);
    }

    public record OpenAccountInput(String customerId, AccountType accountType, String currency, Double initialDeposit) {}

    public record LegacyCbsPostInput(String transactionRef, String sourceAccount, String targetAccount, Double amount, String currency, String narration) {}
}
