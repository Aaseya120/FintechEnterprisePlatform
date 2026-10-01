package com.banking.account.controller;

import com.banking.account.dto.SavingVaultDtos.*;
import com.banking.account.service.SavingVaultService;
import com.banking.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for customer saving vaults and financial goal pots.
 */
@RestController
@RequestMapping("/api/v1/accounts/vaults")
public class SavingVaultController {

    private final SavingVaultService vaultService;

    public SavingVaultController(SavingVaultService vaultService) {
        this.vaultService = vaultService;
    }

    /**
     * Creates a new saving vault or target goal under a parent checking/savings account.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SavingVaultResponseDto>> createVault(
            @Valid @RequestBody CreateSavingVaultRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        SavingVaultResponseDto response = vaultService.createVault(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Saving vault created successfully", corrId));
    }

    /**
     * Deposits money from parent account into the saving vault.
     */
    @PostMapping("/{vaultId}/deposit")
    public ResponseEntity<ApiResponse<SavingVaultResponseDto>> depositToVault(
            @PathVariable String vaultId,
            @Valid @RequestBody VaultTransferRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        SavingVaultResponseDto response = vaultService.depositToVault(vaultId, request.amount());
        return ResponseEntity.ok(ApiResponse.success(response, "Deposit to vault successful", corrId));
    }

    /**
     * Withdraws money from saving vault back into parent account.
     */
    @PostMapping("/{vaultId}/withdraw")
    public ResponseEntity<ApiResponse<SavingVaultResponseDto>> withdrawFromVault(
            @PathVariable String vaultId,
            @Valid @RequestBody VaultTransferRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        SavingVaultResponseDto response = vaultService.withdrawFromVault(vaultId, request.amount());
        return ResponseEntity.ok(ApiResponse.success(response, "Withdrawal from vault successful", corrId));
    }

    /**
     * Locks or unlocks a saving vault.
     */
    @PostMapping("/{vaultId}/toggle-lock")
    public ResponseEntity<ApiResponse<SavingVaultResponseDto>> toggleLock(
            @PathVariable String vaultId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        SavingVaultResponseDto response = vaultService.toggleLock(vaultId);
        return ResponseEntity.ok(ApiResponse.success(response, "Vault lock status toggled", corrId));
    }

    /**
     * Retrieves all saving vaults for a customer.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<SavingVaultResponseDto>>> getCustomerVaults(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<SavingVaultResponseDto> vaults = vaultService.getVaultsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(vaults, corrId));
    }
}
