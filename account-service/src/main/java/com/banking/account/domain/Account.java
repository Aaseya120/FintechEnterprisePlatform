package com.banking.account.domain;

import com.banking.common.exception.BankingException;
import com.banking.common.exception.InsufficientFundsException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_accounts_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_accounts_curr_bal", columnList = "currency, available_balance")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_accounts_account_number", columnNames = "account_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "account_number", length = 34, nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private AccountType accountType;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Column(name = "available_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal availableBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AccountStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Account(String id, String accountNumber, String customerId, AccountType accountType, String currency, BigDecimal initialDeposit) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.currency = currency.toUpperCase();
        this.balance = initialDeposit;
        this.availableBalance = initialDeposit;
        this.status = AccountStatus.ACTIVE;
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        validateActiveStatus();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("INVALID_AMOUNT", "Debit amount must be strictly positive", HttpStatus.BAD_REQUEST);
        }
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(this.accountNumber, amount.toPlainString());
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        validateActiveStatus();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("INVALID_AMOUNT", "Credit amount must be strictly positive", HttpStatus.BAD_REQUEST);
        }
        this.availableBalance = this.availableBalance.add(amount);
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    private void validateActiveStatus() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new BankingException("ACCOUNT_INACTIVE",
                    String.format("Account %s is %s; cannot process financial operations", this.accountNumber, this.status),
                    HttpStatus.FORBIDDEN);
        }
    }
}
