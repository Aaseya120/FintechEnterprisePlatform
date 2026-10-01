package com.banking.batch.reconciliation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recon_runs")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationRunEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "rule_type", length = 30, nullable = false)
    private String ruleType;

    @Column(name = "total_internal_records", nullable = false)
    private int totalInternalRecords;

    @Column(name = "total_external_records", nullable = false)
    private int totalExternalRecords;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount;

    @Column(name = "break_count", nullable = false)
    private int breakCount;

    @Column(name = "match_rate_percentage", precision = 6, scale = 2, nullable = false)
    private BigDecimal matchRatePercentage;

    @Column(name = "total_discrepancy_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalDiscrepancyAmount;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReconciliationBreakEntity> breaks = new ArrayList<>();

    public ReconciliationRunEntity(String id, LocalDate reconciliationDate, String ruleType,
                                   int totalInternalRecords, int totalExternalRecords,
                                   int matchedCount, int breakCount, BigDecimal matchRatePercentage,
                                   BigDecimal totalDiscrepancyAmount, String status) {
        this.id = id;
        this.reconciliationDate = reconciliationDate;
        this.ruleType = ruleType;
        this.totalInternalRecords = totalInternalRecords;
        this.totalExternalRecords = totalExternalRecords;
        this.matchedCount = matchedCount;
        this.breakCount = breakCount;
        this.matchRatePercentage = matchRatePercentage;
        this.totalDiscrepancyAmount = totalDiscrepancyAmount;
        this.status = status;
        this.executedAt = Instant.now();
    }

    public void addBreak(ReconciliationBreakEntity breakEntity) {
        breaks.add(breakEntity);
        breakEntity.setRun(this);
    }
}
