package com.banking.account.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "saving_vaults",
    indexes = {
        @Index(name = "idx_vault_customer", columnList = "customer_id"),
        @Index(name = "idx_vault_parent_acc", columnList = "parent_account_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class SavingVault {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "parent_account_number", length = 34, nullable = false)
    private String parentAccountNumber;

    @Column(name = "vault_name", length = 100, nullable = false)
    private String vaultName;

    @Column(name = "target_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "current_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_status", length = 20, nullable = false)
    private LockStatus lockStatus;

    @Column(name = "auto_roundup_enabled", nullable = false)
    private boolean autoRoundupEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum LockStatus { LOCKED, UNLOCKED }

    public SavingVault(String id, String customerId, String parentAccountNumber, String vaultName,
                       BigDecimal targetAmount, String currency, LocalDate targetDate, boolean autoRoundupEnabled) {
        this.id = id;
        this.customerId = customerId;
        this.parentAccountNumber = parentAccountNumber;
        this.vaultName = vaultName;
        this.targetAmount = targetAmount;
        this.currentBalance = BigDecimal.ZERO;
        this.currency = currency.toUpperCase();
        this.targetDate = targetDate;
        this.lockStatus = LockStatus.UNLOCKED;
        this.autoRoundupEnabled = autoRoundupEnabled;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void deposit(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void withdraw(BigDecimal amount) {
        this.currentBalance = this.currentBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }
}
