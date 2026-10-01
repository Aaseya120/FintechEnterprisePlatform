package com.banking.batch.reconciliation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReconciliationModels {

    public enum ReconStatus {
        MATCHED,
        UNMATCHED_INTERNAL,
        UNMATCHED_EXTERNAL,
        AMOUNT_MISMATCH
    }

    public enum ReconRuleType {
        EXACT_MATCH,
        TOLERANCE_WINDOW
    }

    public record InternalLedgerRecord(
            String txnRef,
            String sourceAccount,
            String targetAccount,
            BigDecimal amount,
            String currency,
            Instant timestamp
    ) implements Serializable {}

    public record ReconciliationBreak(
            String id,
            String internalTxnRef,
            String externalTxnRef,
            BigDecimal internalAmount,
            BigDecimal externalAmount,
            BigDecimal discrepancyAmount,
            ReconStatus status,
            String breakReason,
            String resolutionStatus, // OPEN, RESOLVED, WRITTEN_OFF
            String resolvedBy,
            String resolutionNotes,
            Instant detectedAt,
            Instant resolvedAt
    ) implements Serializable {}

    public record StrategyReconResult(
            int matchedCount,
            List<ReconciliationBreak> breaks,
            BigDecimal totalDiscrepancy
    ) implements Serializable {}

    public record ReconciliationSummary(
            String runId,
            LocalDate reconciliationDate,
            ReconRuleType ruleType,
            int totalInternalRecords,
            int totalExternalRecords,
            int matchedCount,
            int breakCount,
            BigDecimal matchRatePercentage,
            BigDecimal totalDiscrepancyAmount,
            List<ReconciliationBreak> breaks,
            String status,
            Instant executedAt
    ) implements Serializable {}

    public record BreakResolutionRequest(
            String resolutionStatus, // RESOLVED, WRITTEN_OFF
            String resolvedBy,
            String notes
    ) implements Serializable {}
}
