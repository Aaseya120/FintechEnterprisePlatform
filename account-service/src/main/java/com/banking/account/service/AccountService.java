package com.banking.account.service;

import com.banking.account.config.RedisCacheConfig;
import com.banking.account.domain.Account;
import com.banking.account.domain.AccountAuditLog;
import com.banking.account.domain.AccountStatus;
import com.banking.account.dto.AccountMapper;
import com.banking.account.dto.AccountResponseDto;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.repository.AccountAuditLogRepository;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final AccountAuditLogRepository auditLogRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository,
                          AccountAuditLogRepository auditLogRepository,
                          AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountResponseDto createAccount(CreateAccountRequest request) {
        String id = UUID.randomUUID().toString();
        String accountNumber = generateAccountNumber();

        Account account = new Account(
                id,
                accountNumber,
                request.customerId(),
                request.accountType(),
                request.currency(),
                request.initialDeposit()
        );

        Account saved = accountRepository.save(account);

        AccountAuditLog audit = new AccountAuditLog(
                UUID.randomUUID().toString(),
                saved.getId(),
                "ACCOUNT_OPENED",
                BigDecimal.ZERO,
                saved.getBalance(),
                request.customerId(),
                "127.0.0.1",
                UUID.randomUUID().toString()
        );
        auditLogRepository.save(audit);

        log.info("Created new banking account {} for customer {}", accountNumber, request.customerId());
        return accountMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.CACHE_ACCOUNTS, key = "#accountNumber")
    public AccountResponseDto getAccountByNumber(String accountNumber) {
        log.debug("Cache miss for account {}. Querying database.", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.CACHE_ACCOUNT_BALANCES, key = "#accountNumber")
    public BigDecimal getAvailableBalance(String accountNumber) {
        log.debug("Cache miss for balance {}. Querying database.", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
        return account.getAvailableBalance();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.CACHE_CUSTOMER_ACCOUNTS, key = "#customerId")
    public List<AccountResponseDto> getAccountsByCustomer(String customerId) {
        log.debug("Fetching accounts for customer {}", customerId);
        return accountRepository.findByCustomerIdAndStatus(customerId, AccountStatus.ACTIVE)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_ACCOUNTS, key = "#accountNumber"),
            @CacheEvict(value = RedisCacheConfig.CACHE_ACCOUNT_BALANCES, key = "#accountNumber"),
            @CacheEvict(value = RedisCacheConfig.CACHE_CUSTOMER_ACCOUNTS, allEntries = true)
    })
    public AccountResponseDto debitAccount(String accountNumber, BigDecimal amount, String correlationId, String actorId) {
        // Use pessimistic write lock for high-throughput concurrency safety
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));

        BigDecimal previousBalance = account.getBalance();
        account.debit(amount);
        Account saved = accountRepository.save(account);

        AccountAuditLog audit = new AccountAuditLog(
                UUID.randomUUID().toString(),
                saved.getId(),
                "DEBIT",
                previousBalance,
                saved.getBalance(),
                actorId,
                "0.0.0.0",
                correlationId
        );
        auditLogRepository.save(audit);

        log.info("Debited {} {} from account {}. New balance: {}. CorrelationId: {}",
                amount, account.getCurrency(), accountNumber, saved.getBalance(), correlationId);
        return accountMapper.toDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_ACCOUNTS, key = "#accountNumber"),
            @CacheEvict(value = RedisCacheConfig.CACHE_ACCOUNT_BALANCES, key = "#accountNumber"),
            @CacheEvict(value = RedisCacheConfig.CACHE_CUSTOMER_ACCOUNTS, allEntries = true)
    })
    public AccountResponseDto creditAccount(String accountNumber, BigDecimal amount, String correlationId, String actorId) {
        // Use pessimistic write lock
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));

        BigDecimal previousBalance = account.getBalance();
        account.credit(amount);
        Account saved = accountRepository.save(account);

        AccountAuditLog audit = new AccountAuditLog(
                UUID.randomUUID().toString(),
                saved.getId(),
                "CREDIT",
                previousBalance,
                saved.getBalance(),
                actorId,
                "0.0.0.0",
                correlationId
        );
        auditLogRepository.save(audit);

        log.info("Credited {} {} to account {}. New balance: {}. CorrelationId: {}",
                amount, account.getCurrency(), accountNumber, saved.getBalance(), correlationId);
        return accountMapper.toDto(saved);
    }

    private String generateAccountNumber() {
        long randomNum = 1000000000L + RANDOM.nextLong(9000000000L);
        return "US" + randomNum;
    }
}
