package com.banking.loan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "loans",
    indexes = {
        @Index(name = "idx_loans_customer_status", columnList = "customer_id, status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_loan_acc", columnNames = "loan_account_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Loan {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "loan_account_number", length = 34, nullable = false, unique = true)
    private String loanAccountNumber;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", length = 30, nullable = false)
    private LoanType loanType;

    @Column(name = "principal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "annual_interest_rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal annualInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "emi_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal emiAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LoanStatus status;

    @Column(name = "disbursement_account", length = 34, nullable = false)
    private String disbursementAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum LoanType { HOME, PERSONAL, AUTO, EDUCATION, SME }
    public enum LoanStatus { APPLIED, UNDER_REVIEW, APPROVED, REJECTED, DISBURSED, CLOSED }

    public Loan(String id, String loanAccountNumber, String customerId, LoanType loanType,
                BigDecimal principalAmount, BigDecimal annualInterestRate, int tenureMonths,
                BigDecimal emiAmount, String disbursementAccount) {
        this.id = id;
        this.loanAccountNumber = loanAccountNumber;
        this.customerId = customerId;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.annualInterestRate = annualInterestRate;
        this.tenureMonths = tenureMonths;
        this.emiAmount = emiAmount;
        this.disbursementAccount = disbursementAccount;
        this.status = LoanStatus.APPLIED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void approve() {
        this.status = LoanStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void disburse() {
        this.status = LoanStatus.DISBURSED;
        this.updatedAt = Instant.now();
    }
}
