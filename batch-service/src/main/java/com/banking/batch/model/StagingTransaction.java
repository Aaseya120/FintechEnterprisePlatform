package com.banking.batch.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "stg_clearing_transactions",
    indexes = {
        @Index(name = "idx_stg_status_job", columnList = "processing_status, batch_job_id"),
        @Index(name = "idx_stg_settlement", columnList = "settlement_date, processing_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class StagingTransaction {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "external_txn_ref", length = 64, nullable = false)
    private String externalTxnRef;

    @Column(name = "source_account", length = 34, nullable = false)
    private String sourceAccount;

    @Column(name = "target_account", length = 34, nullable = false)
    private String targetAccount;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "processing_status", length = 20, nullable = false)
    private String processingStatus; // STAGED, PROCESSED, ERROR

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "batch_job_id")
    private Long batchJobId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StagingTransaction(String id, String externalTxnRef, String sourceAccount,
                              String targetAccount, BigDecimal amount, String currency,
                              String channel, LocalDate settlementDate, Long batchJobId) {
        this.id = id;
        this.externalTxnRef = externalTxnRef;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.settlementDate = settlementDate;
        this.processingStatus = "STAGED";
        this.batchJobId = batchJobId;
        this.createdAt = Instant.now();
    }
}
