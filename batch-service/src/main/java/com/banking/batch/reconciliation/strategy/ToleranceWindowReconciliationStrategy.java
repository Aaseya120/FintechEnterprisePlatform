package com.banking.batch.reconciliation.strategy;

import com.banking.batch.model.StagingTransaction;
import com.banking.batch.reconciliation.ReconciliationModels.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy implementation: Matches records allowing a micro-tolerance threshold (e.g. 0.05)
 * for foreign exchange rounding variances or intermediary clearing fees.
 */
@Component
public class ToleranceWindowReconciliationStrategy implements ReconciliationRuleStrategy {

    private static final BigDecimal TOLERANCE_LIMIT = new BigDecimal("0.05");

    @Override
    public ReconRuleType getRuleType() {
        return ReconRuleType.TOLERANCE_WINDOW;
    }

    @Override
    public StrategyReconResult reconcile(List<InternalLedgerRecord> internalTxns, List<StagingTransaction> externalTxns) {
        var externalMap = externalTxns.stream()
                .collect(Collectors.toMap(
                        StagingTransaction::getExternalTxnRef,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        var processedExternalRefs = new HashSet<String>();
        var breaks = new ArrayList<ReconciliationBreak>();
        int matchedCount = 0;
        var totalDiscrepancy = BigDecimal.ZERO;

        for (var internal : internalTxns) {
            var external = externalMap.get(internal.txnRef());

            if (external != null) {
                processedExternalRefs.add(external.getExternalTxnRef());
                var diff = internal.amount().subtract(external.getAmount()).abs();

                if (diff.compareTo(TOLERANCE_LIMIT) <= 0) {
                    // Accepted within financial tolerance threshold
                    matchedCount++;
                } else {
                    totalDiscrepancy = totalDiscrepancy.add(diff);
                    breaks.add(new ReconciliationBreak(
                            UUID.randomUUID().toString(),
                            internal.txnRef(),
                            external.getExternalTxnRef(),
                            internal.amount(),
                            external.getAmount(),
                            diff,
                            ReconStatus.AMOUNT_MISMATCH,
                            "Amount difference " + diff + " exceeds permitted tolerance threshold " + TOLERANCE_LIMIT,
                            "OPEN",
                            null,
                            null,
                            Instant.now(),
                            null
                    ));
                }
            } else {
                totalDiscrepancy = totalDiscrepancy.add(internal.amount());
                breaks.add(new ReconciliationBreak(
                        UUID.randomUUID().toString(),
                        internal.txnRef(),
                        null,
                        internal.amount(),
                        null,
                        internal.amount(),
                        ReconStatus.UNMATCHED_INTERNAL,
                        "Internal txn ref [" + internal.txnRef() + "] missing in clearing feed (Tolerance Rule)",
                        "OPEN",
                        null,
                        null,
                        Instant.now(),
                        null
                ));
            }
        }

        for (var external : externalTxns) {
            if (!processedExternalRefs.contains(external.getExternalTxnRef())) {
                totalDiscrepancy = totalDiscrepancy.add(external.getAmount());
                breaks.add(new ReconciliationBreak(
                        UUID.randomUUID().toString(),
                        null,
                        external.getExternalTxnRef(),
                        null,
                        external.getAmount(),
                        external.getAmount(),
                        ReconStatus.UNMATCHED_EXTERNAL,
                        "External clearing txn [" + external.getExternalTxnRef() + "] unmatched in core ledger (Tolerance Rule)",
                        "OPEN",
                        null,
                        null,
                        Instant.now(),
                        null
                ));
            }
        }

        return new StrategyReconResult(matchedCount, breaks, totalDiscrepancy);
    }
}
