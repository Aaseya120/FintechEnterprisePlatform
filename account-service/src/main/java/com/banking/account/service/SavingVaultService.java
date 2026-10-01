package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.SavingVault;
import com.banking.account.dto.SavingVaultDtos.*;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.SavingVaultRepository;
import com.banking.common.audit.BankingServiceRegistry;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SavingVaultService {

    private static final Logger log = LoggerFactory.getLogger(SavingVaultService.class);

    private final SavingVaultRepository vaultRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public SavingVaultService(SavingVaultRepository vaultRepository,
                              AccountRepository accountRepository,
                              AccountService accountService) {
        this.vaultRepository = vaultRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    @Transactional
    public SavingVaultResponseDto createVault(CreateSavingVaultRequestDto dto) {
        Account parent = accountRepository.findByAccountNumber(dto.parentAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", dto.parentAccountNumber()));

        if (!parent.getCustomerId().equals(dto.customerId())) {
            throw new BankingException("UNAUTHORIZED_ACCOUNT", "Parent account does not belong to customer", HttpStatus.FORBIDDEN);
        }

        String vaultId = "vlt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        boolean autoRoundup = dto.autoRoundupEnabled() != null && dto.autoRoundupEnabled();

        SavingVault vault = new SavingVault(
                vaultId,
                dto.customerId(),
                dto.parentAccountNumber(),
                dto.vaultName(),
                dto.targetAmount(),
                dto.currency(),
                dto.targetDate(),
                autoRoundup
        );

        SavingVault saved = vaultRepository.save(vault);
        log.info("[{}] Created Saving Vault: id={}, name='{}', target={}",
                BankingServiceRegistry.ACCOUNT_LEDGER.getServiceId(), saved.getId(), saved.getVaultName(), saved.getTargetAmount());

        return SavingVaultResponseDto.fromEntity(saved);
    }

    @Transactional
    public SavingVaultResponseDto depositToVault(String vaultId, BigDecimal amount) {
        SavingVault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingVault", vaultId));

        // Atomically debit parent account
        accountService.debitAccount(vault.getParentAccountNumber(), amount, UUID.randomUUID().toString(), vault.getCustomerId());

        // Credit saving vault
        vault.deposit(amount);
        SavingVault updated = vaultRepository.save(vault);

        log.info("[{}] Deposited {} {} to Vault '{}'. New balance: {}",
                BankingServiceRegistry.ACCOUNT_LEDGER.getServiceId(), amount, vault.getCurrency(), vault.getVaultName(), updated.getCurrentBalance());

        return SavingVaultResponseDto.fromEntity(updated);
    }

    @Transactional
    public SavingVaultResponseDto withdrawFromVault(String vaultId, BigDecimal amount) {
        SavingVault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingVault", vaultId));

        if (vault.getLockStatus() == SavingVault.LockStatus.LOCKED) {
            throw new BankingException("VAULT_LOCKED", "Cannot withdraw from locked vault. Unlock vault first.", HttpStatus.BAD_REQUEST);
        }

        if (vault.getCurrentBalance().compareTo(amount) < 0) {
            throw new BankingException("INSUFFICIENT_VAULT_BALANCE", "Vault balance is lower than requested withdrawal", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Debit saving vault
        vault.withdraw(amount);
        SavingVault updated = vaultRepository.save(vault);

        // Atomically credit parent account back
        accountService.creditAccount(vault.getParentAccountNumber(), amount, UUID.randomUUID().toString(), vault.getCustomerId());

        log.info("[{}] Withdrew {} {} from Vault '{}' into Account {}.",
                BankingServiceRegistry.ACCOUNT_LEDGER.getServiceId(), amount, vault.getCurrency(), vault.getVaultName(), vault.getParentAccountNumber());

        return SavingVaultResponseDto.fromEntity(updated);
    }

    @Transactional
    public SavingVaultResponseDto toggleLock(String vaultId) {
        SavingVault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingVault", vaultId));

        vault.setLockStatus(vault.getLockStatus() == SavingVault.LockStatus.LOCKED
                ? SavingVault.LockStatus.UNLOCKED
                : SavingVault.LockStatus.LOCKED);

        SavingVault saved = vaultRepository.save(vault);
        log.info("[{}] Toggled vault lock status: vaultId={}, status={}",
                BankingServiceRegistry.ACCOUNT_LEDGER.getServiceId(), vaultId, saved.getLockStatus());

        return SavingVaultResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SavingVaultResponseDto> getVaultsByCustomer(String customerId) {
        return vaultRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(SavingVaultResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavingVaultResponseDto getVaultById(String vaultId) {
        return vaultRepository.findById(vaultId)
                .map(SavingVaultResponseDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("SavingVault", vaultId));
    }
}
