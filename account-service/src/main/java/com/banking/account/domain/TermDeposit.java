package com.banking.account.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "term_deposits",
    indexes = {
        @Index(name = "idx_td_customer", columnList = "customer_id, status"),
        @Index(name = "idx_td_maturity", columnList = "maturity_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TermDeposit {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "deposit_number", length = 32, nullable = false, unique = true)
    private String depositNumber;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "linked_account_number", length = 34, nullable = false)
    private String linkedAccountNumber;

    @Column(name = "principal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "interest_rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal interestRate; // e.g. 0.0725 for 7.25% p.a.

    @Column(name = "tenor_months", nullable = false)
    private int tenorMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "compounding_frequency", length = 20, nullable = false)
    private CompoundingFrequency compoundingFrequency;

    @Column(name = "maturity_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal maturityAmount;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TermDepositStatus status;

    @Column(name = "auto_renewal", nullable = false)
    private boolean autoRenewal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum CompoundingFrequency { MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY }
    public enum TermDepositStatus { ACTIVE, MATURED, PREMATURE_CLOSED }

    public TermDeposit(String id, String depositNumber, String customerId, String linkedAccountNumber,
                       BigDecimal principalAmount, String currency, BigDecimal interestRate,
                       int tenorMonths, CompoundingFrequency compoundingFrequency, boolean autoRenewal) {
        this.id = id;
        this.depositNumber = depositNumber;
        this.customerId = customerId;
        this.linkedAccountNumber = linkedAccountNumber;
        this.principalAmount = principalAmount;
        this.currency = currency.toUpperCase();
        this.interestRate = interestRate;
        this.tenorMonths = tenorMonths;
        this.compoundingFrequency = compoundingFrequency;
        this.autoRenewal = autoRenewal;
        this.status = TermDepositStatus.ACTIVE;
        this.maturityDate = LocalDate.now().plusMonths(tenorMonths);
        this.maturityAmount = calculateMaturity(principalAmount, interestRate, tenorMonths, compoundingFrequency);
        this.createdAt = Instant.now();
    }

    /**
     * Mathematical Compound Interest Formula:
     * A = P * (1 + r/n)^(n*t)
     */
    public static BigDecimal calculateMaturity(BigDecimal principal, BigDecimal rate, int months, CompoundingFrequency freq) {
        int n = switch (freq) {
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case HALF_YEARLY -> 2;
            case ANNUALLY -> 1;
        };
        double p = principal.doubleValue();
        double r = rate.doubleValue();
        double t = months / 12.0;
        double a = p * Math.pow(1.0 + (r / n), n * t);
        return BigDecimal.valueOf(a).setScale(4, RoundingMode.HALF_UP);
    }
}
