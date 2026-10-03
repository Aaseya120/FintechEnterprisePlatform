package com.banking.account.controller;

import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.TermDepositDtos.*;
import com.banking.account.service.AccountService;
import com.banking.account.service.TermDepositService;
import com.banking.common.dto.ApiResponse;
import com.banking.common.security.BankingRoles;
import com.banking.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for core banking accounts, balance inquiries, and term deposits.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountRestController {

    private final AccountService accountService;
    private final TermDepositService termDepositService;

    public AccountRestController(AccountService accountService, TermDepositService termDepositService) {
        this.accountService = accountService;
        this.termDepositService = termDepositService;
    }

    /**
     * Opens a new core banking account for a customer.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "X-Channel", required = false, defaultValue = "WEB") String channel) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        AccountResponseDto created = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Account successfully opened", corrId));
    }

    /**
     * Retrieves account metadata and balance by account number.
     */
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getAccount(
            @PathVariable String accountNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        AccountResponseDto account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(account, corrId));
    }

    /**
     * Ultra-low latency available balance inquiry.
     */
    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(
            @PathVariable String accountNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BigDecimal balance = accountService.getAvailableBalance(accountNumber);
        Map<String, Object> data = Map.of("accountNumber", accountNumber, "availableBalance", balance);
        return ResponseEntity.ok(ApiResponse.success(data, corrId));
    }

    /**
     * Lists all accounts owned by a customer.
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getCustomerAccounts(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<AccountResponseDto> accounts = accountService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(accounts, corrId));
    }

    /**
     * Internal debit operation with pessimistic ledger locking.
     */
    @PostMapping("/{accountNumber}/debit")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> debit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, BigDecimal> payload,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BigDecimal amount = payload.get("amount");
        if (amount == null) {
            throw new com.banking.common.exception.BankingException("MISSING_AMOUNT",
                    "Request body must contain 'amount' field", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        String actorId = SecurityUtils.getCurrentUserId().orElse("SYSTEM");
        AccountResponseDto updated = accountService.debitAccount(accountNumber, amount, corrId, actorId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Account debited successfully", corrId));
    }

    /**
     * Internal credit operation with pessimistic ledger locking.
     */
    @PostMapping("/{accountNumber}/credit")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> credit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, BigDecimal> payload,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BigDecimal amount = payload.get("amount");
        if (amount == null) {
            throw new com.banking.common.exception.BankingException("MISSING_AMOUNT",
                    "Request body must contain 'amount' field", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        String actorId = SecurityUtils.getCurrentUserId().orElse("SYSTEM");
        AccountResponseDto updated = accountService.creditAccount(accountNumber, amount, corrId, actorId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Account credited successfully", corrId));
    }

    /**
     * Opens a fixed/term deposit with compound interest calculation.
     */
    @PostMapping("/term-deposits")
    public ResponseEntity<ApiResponse<TermDepositResponse>> openTermDeposit(
            @Valid @RequestBody OpenTermDepositRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        TermDepositResponse response = termDepositService.openTermDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Term Deposit opened successfully", corrId));
    }

    /**
     * Lists term deposits for a customer.
     */
    @GetMapping("/term-deposits/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<TermDepositResponse>>> getCustomerDeposits(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<TermDepositResponse> list = termDepositService.getCustomerDeposits(customerId);
        return ResponseEntity.ok(ApiResponse.success(list, corrId));
    }

    /**
     * Prematurely liquidates a term deposit with penalty deduction.
     */
    @PostMapping("/term-deposits/{depositNumber}/liquidate")
    public ResponseEntity<ApiResponse<TermDepositLiquidationResponse>> liquidateDeposit(
            @PathVariable String depositNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        TermDepositLiquidationResponse response = termDepositService.liquidateDeposit(depositNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Term deposit liquidated successfully", corrId));
    }
}
