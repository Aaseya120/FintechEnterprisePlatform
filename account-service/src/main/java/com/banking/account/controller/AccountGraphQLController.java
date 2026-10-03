package com.banking.account.controller;

import com.banking.account.domain.AccountType;
import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.SavingVaultDtos.CreateSavingVaultRequestDto;
import com.banking.account.dto.SavingVaultDtos.SavingVaultResponseDto;
import com.banking.account.middleware.LegacyCbsDtos.*;
import com.banking.account.middleware.LegacyCbsMiddlewareGateway;
import com.banking.account.service.AccountService;
import com.banking.account.service.SavingVaultService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AccountGraphQLController {

    private final AccountService accountService;
    private final LegacyCbsMiddlewareGateway cbsGateway;
    private final SavingVaultService vaultService;

    public AccountGraphQLController(AccountService accountService,
                                  LegacyCbsMiddlewareGateway cbsGateway,
                                  SavingVaultService vaultService) {
        this.accountService = accountService;
        this.cbsGateway = cbsGateway;
        this.vaultService = vaultService;
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
        return Map.of("accountNumber", accountNumber, "availableBalance", balance.toPlainString());
    }

    @QueryMapping
    public CbsAccountInquiryResponse legacyCbsAccount(@Argument String accountNumber) {
        return cbsGateway.queryAccount(accountNumber);
    }

    @QueryMapping
    public CbsHealthCheckResponse legacyCbsHealth() {
        return cbsGateway.checkConnectivity();
    }

    @QueryMapping
    public List<SavingVaultResponseDto> savingVaultsByCustomer(@Argument String customerId) {
        return vaultService.getVaultsByCustomer(customerId);
    }

    @QueryMapping
    public SavingVaultResponseDto savingVaultById(@Argument String vaultId) {
        return vaultService.getVaultById(vaultId);
    }

    @MutationMapping
    public AccountResponseDto openAccount(@Argument OpenAccountInput input) {
        CreateAccountRequest request = new CreateAccountRequest(
                input.customerId(),
                input.accountType(),
                input.currency(),
                new BigDecimal(input.initialDeposit())
        );
        return accountService.createAccount(request);
    }

    @MutationMapping
    public AccountResponseDto debitAccount(@Argument String accountNumber, @Argument String amount) {
        return accountService.debitAccount(
                accountNumber,
                new BigDecimal(amount),
                UUID.randomUUID().toString(),
                "GRAPHQL_CLIENT"
        );
    }

    @MutationMapping
    public AccountResponseDto creditAccount(@Argument String accountNumber, @Argument String amount) {
        return accountService.creditAccount(
                accountNumber,
                new BigDecimal(amount),
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
                new BigDecimal(input.amount()),
                input.currency(),
                input.narration(),
                "GRAPHQL_SOA_MIDDLEWARE"
        );
        return cbsGateway.postTransaction(request);
    }

    @MutationMapping
    public SavingVaultResponseDto createSavingVault(@Argument CreateSavingVaultInput input) {
        LocalDate targetDate = input.targetDate() != null && !input.targetDate().isBlank()
                ? LocalDate.parse(input.targetDate()) : null;

        CreateSavingVaultRequestDto req =
                new CreateSavingVaultRequestDto(
                        input.customerId(),
                        input.parentAccountNumber(),
                        input.vaultName(),
                        new BigDecimal(input.targetAmount()),
                        input.currency(),
                        targetDate,
                        input.autoRoundupEnabled()
                );
        return vaultService.createVault(req);
    }

    @MutationMapping
    public SavingVaultResponseDto depositToVault(@Argument String vaultId, @Argument String amount) {
        return vaultService.depositToVault(vaultId, new BigDecimal(amount));
    }

    @MutationMapping
    public SavingVaultResponseDto withdrawFromVault(@Argument String vaultId, @Argument String amount) {
        return vaultService.withdrawFromVault(vaultId, new BigDecimal(amount));
    }

    public record OpenAccountInput(String customerId, AccountType accountType, String currency, String initialDeposit) {}

    public record CreateSavingVaultInput(String customerId, String parentAccountNumber, String vaultName, String targetAmount, String currency, String targetDate, Boolean autoRoundupEnabled) {}

    public record LegacyCbsPostInput(String transactionRef, String sourceAccount, String targetAccount, String amount, String currency, String narration) {}
}
