package com.banking.account.service;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountType;
import com.banking.account.dto.AccountMapper;
import com.banking.account.dto.AccountResponseDto;
import com.banking.account.repository.AccountAuditLogRepository;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.InsufficientFundsException;
import com.banking.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service Unit & Concurrency Invariant Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountAuditLogRepository auditLogRepository;

    @Spy
    private AccountMapper accountMapper = new AccountMapper();

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private final String accountNumber = "US1234567890";

    @BeforeEach
    void setUp() {
        testAccount = new Account(
                UUID.randomUUID().toString(),
                accountNumber,
                "cust_001",
                AccountType.CHECKING,
                "USD",
                new BigDecimal("1000.00")
        );
    }

    @Test
    @DisplayName("Should successfully debit account when funds are sufficient")
    void shouldDebitAccountSuccessfully() {
        when(accountRepository.findByAccountNumberWithLock(accountNumber)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponseDto response = accountService.debitAccount(
                accountNumber,
                new BigDecimal("250.00"),
                "corr-123",
                "user-1"
        );

        assertThat(response).isNotNull();
        assertThat(response.availableBalance()).isEqualByComparingTo("750.00");
        verify(auditLogRepository, times(1)).save(any());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when debit exceeds available balance")
    void shouldThrowWhenDebitExceedsBalance() {
        when(accountRepository.findByAccountNumberWithLock(accountNumber)).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.debitAccount(
                accountNumber,
                new BigDecimal("1500.00"),
                "corr-123",
                "user-1"
        ))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessageContaining("insufficient funds");

        verify(accountRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown account number")
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findByAccountNumberWithLock("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.debitAccount(
                "UNKNOWN",
                new BigDecimal("50.00"),
                "corr-123",
                "user-1"
        ))
        .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should successfully credit account and update balance")
    void shouldCreditAccountSuccessfully() {
        when(accountRepository.findByAccountNumberWithLock(accountNumber)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponseDto response = accountService.creditAccount(
                accountNumber,
                new BigDecimal("500.00"),
                "corr-456",
                "user-1"
        );

        assertThat(response.availableBalance()).isEqualByComparingTo("1500.00");
        verify(auditLogRepository, times(1)).save(any());
    }
}
