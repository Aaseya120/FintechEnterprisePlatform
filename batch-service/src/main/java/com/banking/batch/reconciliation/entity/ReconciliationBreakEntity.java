package com.banking.batch.reconciliation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "recon_breaks",
    indexes = {
        @Index(name = "idx_recon_breaks_run", columnList = "run_id"),
        @Index(name = "idx_recon_breaks_res_status", columnList = "resolution_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationBreakEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRunEntity run;

    @Column(name = "internal_txn_ref", length = 64)
    private String internalTxnRef;

    @Column(name = "external_txn_ref", length = 64)
    private String externalTxnRef;

    @Column(name = "internal_amount", precision = 19, scale = 4)
    private BigDecimal internalAmount;

    @Column(name = "external_amount", precision = 19, scale = 4)
    private BigDecimal externalAmount;

    @Column(name = "discrepancy_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal discrepancyAmount;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "break_reason", length = 255, nullable = false)
    private String breakReason;

    @Column(name = "resolution_status", length = 20, nullable = false)
    private String resolutionStatus; // OPEN, RESOLVED, WRITTEN_OFF

    @Column(name = "resolved_by", length = 64)
    private String resolvedBy;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public ReconciliationBreakEntity(String id, String internalTxnRef, String externalTxnRef,
                                     BigDecimal internalAmount, BigDecimal externalAmount,
                                     BigDecimal discrepancyAmount, String status, String breakReason) {
        this.id = id;
        this.internalTxnRef = internalTxnRef;
        this.externalTxnRef = externalTxnRef;
        this.internalAmount = internalAmount;
        this.externalAmount = externalAmount;
        this.discrepancyAmount = discrepancyAmount;
        this.status = status;
        this.breakReason = breakReason;
        this.resolutionStatus = "OPEN";
        this.detectedAt = Instant.now();
    }

    public void resolve(String resolutionStatus, String resolvedBy, String notes) {
        this.resolutionStatus = resolutionStatus;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
        this.resolvedAt = Instant.now();
    }
}
