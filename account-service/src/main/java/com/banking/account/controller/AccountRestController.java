package com.banking.account.controller;

import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.service.AccountService;
import com.banking.common.dto.ApiResponse;
import com.banking.common.security.BankingRoles;
import com.banking.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts API", description = "High-throughput Core Banking Account & Balance REST APIs")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    @Operation(summary = "Open a new banking account", description = "Creates a new ledger account for a verified customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account opened successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error on input payload")
    })
    public ResponseEntity<ApiResponse<AccountResponseDto>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "X-Channel", required = false, defaultValue = "WEB") String channel) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        AccountResponseDto created = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Account successfully opened", corrId));
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    @Operation(summary = "Get account details", description = "Retrieves account metadata and balance via cache-aside pattern")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getAccount(
            @Parameter(description = "Account number to fetch") @PathVariable String accountNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        AccountResponseDto account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(account, corrId));
    }

    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    @Operation(summary = "Get available balance", description = "Retrieves ultra-low-latency cached account balance from Redis / AWS ElastiCache")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(
            @PathVariable String accountNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BigDecimal balance = accountService.getAvailableBalance(accountNumber);
        Map<String, Object> data = Map.of("accountNumber", accountNumber, "availableBalance", balance);
        return ResponseEntity.ok(ApiResponse.success(data, corrId));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_CUSTOMER + "', '" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    @Operation(summary = "List accounts by customer", description = "Lists all active accounts associated with customer ID")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getCustomerAccounts(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<AccountResponseDto> accounts = accountService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(accounts, corrId));
    }

    @PostMapping("/{accountNumber}/debit")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    @Operation(summary = "Internal debit", description = "Directly debits account with pessimistic lock (Internal/Saga invocation)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> debit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, BigDecimal> payload,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BigDecimal amount = payload.get("amount");
        String actorId = SecurityUtils.getCurrentUserId().orElse("SYSTEM");
        AccountResponseDto updated = accountService.debitAccount(accountNumber, amount, corrId, actorId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Account debited successfully", corrId));
    }

    @PostMapping("/{accountNumber}/credit")
    @PreAuthorize("hasAnyRole('" + BankingRoles.ROLE_TELLER + "', '" + BankingRoles.ROLE_ADMIN + "')")
    @Operation(summary = "Internal credit", description = "Directly credits account with pessimistic lock (Internal/Saga invocation)")
    public ResponseEntity<ApiResponse<AccountResponseDto>> credit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, BigDecimal> payload,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BigDecimal amount = payload.get("amount");
        String actorId = SecurityUtils.getCurrentUserId().orElse("SYSTEM");
        AccountResponseDto updated = accountService.creditAccount(accountNumber, amount, corrId, actorId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Account credited successfully", corrId));
    }
}
