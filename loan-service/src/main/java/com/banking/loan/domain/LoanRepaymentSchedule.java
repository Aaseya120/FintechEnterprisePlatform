package com.banking.loan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "loan_repayment_schedule",
    indexes = {
        @Index(name = "idx_repayment_loan_due", columnList = "loan_id, due_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class LoanRepaymentSchedule {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "loan_id", length = 36, nullable = false)
    private String loanId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal interestComponent;

    @Column(name = "total_installment", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalInstallment;

    @Column(name = "remaining_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ScheduleStatus status;

    public enum ScheduleStatus { PENDING, PAID, OVERDUE }

    public LoanRepaymentSchedule(String id, String loanId, int installmentNumber, LocalDate dueDate,
                                 BigDecimal principalComponent, BigDecimal interestComponent,
                                 BigDecimal totalInstallment, BigDecimal remainingBalance) {
        this.id = id;
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.principalComponent = principalComponent;
        this.interestComponent = interestComponent;
        this.totalInstallment = totalInstallment;
        this.remainingBalance = remainingBalance;
        this.status = ScheduleStatus.PENDING;
    }
}
