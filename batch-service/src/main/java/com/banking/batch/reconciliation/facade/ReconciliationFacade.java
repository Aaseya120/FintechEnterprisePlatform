package com.banking.batch.reconciliation.facade;

import com.banking.batch.model.StagingTransaction;
import com.banking.batch.reconciliation.ReconciliationModels.*;
import com.banking.batch.reconciliation.entity.ReconciliationBreakEntity;
import com.banking.batch.reconciliation.entity.ReconciliationRunEntity;
import com.banking.batch.reconciliation.factory.ReconciliationRuleFactory;
import com.banking.batch.reconciliation.repository.ReconciliationBreakRepository;
import com.banking.batch.reconciliation.repository.ReconciliationRunRepository;
import com.banking.batch.repository.StagingTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Facade Pattern: Unified, high-level interface encapsulating the complex reconciliation
 * orchestration, matching strategy selection, break persistence, and audit resolution.
 */
@Service
public class ReconciliationFacade {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationFacade.class);

    private final StagingTransactionRepository stagingRepository;
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final ReconciliationRuleFactory ruleFactory;

    public ReconciliationFacade(StagingTransactionRepository stagingRepository,
                                ReconciliationRunRepository runRepository,
                                ReconciliationBreakRepository breakRepository,
                                ReconciliationRuleFactory ruleFactory) {
        this.stagingRepository = stagingRepository;
        this.runRepository = runRepository;
        this.breakRepository = breakRepository;
        this.ruleFactory = ruleFactory;
    }

    /**
     * Executes the end-to-end reconciliation workflow for a given date and matching rule.
     */
    @Transactional
    public ReconciliationSummary executeReconciliation(LocalDate reconDate, ReconRuleType ruleType) {
        log.info("Starting automated reconciliation run for date={} with rule={}", reconDate, ruleType);

        // 1. Fetch external clearing records from staging table
        var externalRecords = stagingRepository.findBySettlementDate(reconDate);
        if (externalRecords.isEmpty()) {
            // If no staged records exist for today, check without date filter to support demo feeds
            externalRecords = stagingRepository.findAll().stream().limit(100).toList();
        }

        // 2. Fetch or assemble internal core ledger transactions
        var internalRecords = loadInternalLedgerTransactions(reconDate, externalRecords);

        // 3. Obtain Strategy via Factory Pattern and execute matching
        var strategy = ruleFactory.getStrategy(ruleType);
        var reconResult = strategy.reconcile(internalRecords, externalRecords);

        int totalInternal = internalRecords.size();
        int totalExternal = externalRecords.size();
        int totalRecords = Math.max(totalInternal, totalExternal);

        var matchRate = (totalRecords > 0)
                ? BigDecimal.valueOf((double) reconResult.matchedCount() / totalRecords * 100)
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String runId = UUID.randomUUID().toString();
        var runEntity = new ReconciliationRunEntity(
                runId,
                reconDate,
                ruleType.name(),
                totalInternal,
                totalExternal,
                reconResult.matchedCount(),
                reconResult.breaks().size(),
                matchRate,
                reconResult.totalDiscrepancy(),
                "COMPLETED"
        );

        // 4. Save run entity
        runRepository.save(runEntity);

        // 5. Persist breaks
        for (var b : reconResult.breaks()) {
            var breakEntity = new ReconciliationBreakEntity(
                    b.id(),
                    b.internalTxnRef(),
                    b.externalTxnRef(),
                    b.internalAmount(),
                    b.externalAmount(),
                    b.discrepancyAmount(),
                    b.status().name(),
                    b.breakReason()
            );
            runEntity.addBreak(breakEntity);
            breakRepository.save(breakEntity);
        }

        log.info("Reconciliation run completed. runId={}, matched={}, breaks={}, matchRate={}%",
                runId, reconResult.matchedCount(), reconResult.breaks().size(), matchRate);

        return new ReconciliationSummary(
                runId,
                reconDate,
                ruleType,
                totalInternal,
                totalExternal,
                reconResult.matchedCount(),
                reconResult.breaks().size(),
                matchRate,
                reconResult.totalDiscrepancy(),
                reconResult.breaks(),
                "COMPLETED",
                runEntity.getExecutedAt()
        );
    }

    /**
     * Resolves an open reconciliation break with audit tracking and notes.
     */
    @Transactional
    public ReconciliationBreak resolveBreak(String breakId, BreakResolutionRequest request) {
        var breakEntity = breakRepository.findById(breakId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliation break not found: " + breakId));

        breakEntity.resolve(request.resolutionStatus(), request.resolvedBy(), request.notes());
        breakRepository.save(breakEntity);

        log.info("Reconciliation break [{}] resolved by [{}] with status [{}]",
                breakId, request.resolvedBy(), request.resolutionStatus());

        return toBreakRecord(breakEntity);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationSummary> getAllRuns() {
        return runRepository.findAllByOrderByExecutedAtDesc().stream()
                .map(this::toRunSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationSummary getRunDetails(String runId) {
        var run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliation run not found: " + runId));
        return toRunSummary(run);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationBreak> getOpenBreaks() {
        return breakRepository.findAllOpenBreaks().stream()
                .map(this::toBreakRecord)
                .toList();
    }

    /**
     * Simulates/loads internal ledger records correlated with the external clearing records.
     * Introduces controlled real-world exceptions (slight amounts or unmatched entries)
     * to demonstrate the automated reconciliation breaks and resolution workflow.
     */
    private List<InternalLedgerRecord> loadInternalLedgerTransactions(LocalDate date, List<StagingTransaction> externalRecords) {
        var internalList = new ArrayList<InternalLedgerRecord>();

        if (externalRecords.isEmpty()) {
            // Seed a representative baseline internal dataset
            internalList.add(new InternalLedgerRecord(
                    "TXN-INT-1001", "ACCT-1001", "ACCT-2001", new BigDecimal("1500.00"), "USD", Instant.now()
            ));
            internalList.add(new InternalLedgerRecord(
                    "TXN-INT-1002", "ACCT-1002", "ACCT-2002", new BigDecimal("250.50"), "USD", Instant.now()
            ));
            return internalList;
        }

        int index = 0;
        for (var ext : externalRecords) {
            index++;
            if (index % 15 == 0) {
                // Break scenario: slight amount discrepancy (e.g. intermediary clearing fee)
                internalList.add(new InternalLedgerRecord(
                        ext.getExternalTxnRef(),
                        ext.getSourceAccount(),
                        ext.getTargetAccount(),
                        ext.getAmount().add(new BigDecimal("1.25")),
                        ext.getCurrency(),
                        Instant.now()
                ));
            } else if (index % 25 == 0) {
                // Break scenario: missing in external feed (unmatched internal)
                internalList.add(new InternalLedgerRecord(
                        "TXN-ORPHAN-" + UUID.randomUUID().toString().substring(0, 8),
                        ext.getSourceAccount(),
                        ext.getTargetAccount(),
                        new BigDecimal("990.00"),
                        ext.getCurrency(),
                        Instant.now()
                ));
            } else {
                // Exact match scenario
                internalList.add(new InternalLedgerRecord(
                        ext.getExternalTxnRef(),
                        ext.getSourceAccount(),
                        ext.getTargetAccount(),
                        ext.getAmount(),
                        ext.getCurrency(),
                        Instant.now()
                ));
            }
        }

        return internalList;
    }

    private ReconciliationSummary toRunSummary(ReconciliationRunEntity entity) {
        var breakRecords = entity.getBreaks().stream()
                .map(this::toBreakRecord)
                .toList();

        return new ReconciliationSummary(
                entity.getId(),
                entity.getReconciliationDate(),
                ReconRuleType.valueOf(entity.getRuleType()),
                entity.getTotalInternalRecords(),
                entity.getTotalExternalRecords(),
                entity.getMatchedCount(),
                entity.getBreakCount(),
                entity.getMatchRatePercentage(),
                entity.getTotalDiscrepancyAmount(),
                breakRecords,
                entity.getStatus(),
                entity.getExecutedAt()
        );
    }

    private ReconciliationBreak toBreakRecord(ReconciliationBreakEntity b) {
        return new ReconciliationBreak(
                b.getId(),
                b.getInternalTxnRef(),
                b.getExternalTxnRef(),
                b.getInternalAmount(),
                b.getExternalAmount(),
                b.getDiscrepancyAmount(),
                ReconStatus.valueOf(b.getStatus()),
                b.getBreakReason(),
                b.getResolutionStatus(),
                b.getResolvedBy(),
                b.getResolutionNotes(),
                b.getDetectedAt(),
                b.getResolvedAt()
        );
    }
}
