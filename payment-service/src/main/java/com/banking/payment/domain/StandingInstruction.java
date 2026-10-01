package com.banking.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "standing_instructions",
    indexes = {
        @Index(name = "idx_si_customer", columnList = "customer_id, status"),
        @Index(name = "idx_si_next_exec", columnList = "next_execution_date, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class StandingInstruction {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "instruction_name", length = 100, nullable = false)
    private String instructionName;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "source_account_number", length = 34, nullable = false)
    private String sourceAccountNumber;

    @Column(name = "target_account_number", length = 34, nullable = false)
    private String targetAccountNumber;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 20, nullable = false)
    private ScheduleFrequency frequency;

    @Column(name = "execution_day")
    private Integer executionDay; // Day of month (1-28)

    @Column(name = "next_execution_date", nullable = false)
    private LocalDate nextExecutionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private PaymentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InstructionStatus status;

    @Column(name = "total_executions_count", nullable = false)
    private int totalExecutionsCount;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum ScheduleFrequency { DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUALLY }
    public enum PaymentCategory { UTILITY_BILL, RENT, LOAN_EMI, SAVINGS_SWEEP, SUBSCRIPTION, TAX }
    public enum InstructionStatus { ACTIVE, PAUSED, CANCELLED, COMPLETED }

    public StandingInstruction(String id, String instructionName, String customerId,
                               String sourceAccountNumber, String targetAccountNumber,
                               BigDecimal amount, String currency, ScheduleFrequency frequency,
                               Integer executionDay, LocalDate startDate, PaymentCategory category) {
        this.id = id;
        this.instructionName = instructionName;
        this.customerId = customerId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.currency = currency.toUpperCase();
        this.frequency = frequency;
        this.executionDay = executionDay != null ? executionDay : 1;
        this.nextExecutionDate = startDate != null ? startDate : LocalDate.now();
        this.category = category;
        this.status = InstructionStatus.ACTIVE;
        this.totalExecutionsCount = 0;
        this.createdAt = Instant.now();
    }

    public void advanceExecutionDate() {
        this.totalExecutionsCount++;
        this.lastExecutedAt = Instant.now();
        this.nextExecutionDate = switch (this.frequency) {
            case DAILY -> this.nextExecutionDate.plusDays(1);
            case WEEKLY -> this.nextExecutionDate.plusWeeks(1);
            case MONTHLY -> this.nextExecutionDate.plusMonths(1);
            case QUARTERLY -> this.nextExecutionDate.plusMonths(3);
            case ANNUALLY -> this.nextExecutionDate.plusYears(1);
        };
    }
}
