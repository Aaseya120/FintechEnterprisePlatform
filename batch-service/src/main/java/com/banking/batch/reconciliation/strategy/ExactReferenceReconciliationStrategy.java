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
 * Strategy implementation: 1-to-1 exact matching on external/internal transaction reference and currency amounts.
 */
@Component
public class ExactReferenceReconciliationStrategy implements ReconciliationRuleStrategy {

    @Override
    public ReconRuleType getRuleType() {
        return ReconRuleType.EXACT_MATCH;
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

        // 1. Process internal records against external feeds
        for (var internal : internalTxns) {
            var external = externalMap.get(internal.txnRef());

            if (external != null) {
                processedExternalRefs.add(external.getExternalTxnRef());
                if (internal.amount().compareTo(external.getAmount()) == 0) {
                    matchedCount++;
                } else {
                    var diff = internal.amount().subtract(external.getAmount()).abs();
                    totalDiscrepancy = totalDiscrepancy.add(diff);
                    breaks.add(new ReconciliationBreak(
                            UUID.randomUUID().toString(),
                            internal.txnRef(),
                            external.getExternalTxnRef(),
                            internal.amount(),
                            external.getAmount(),
                            diff,
                            ReconStatus.AMOUNT_MISMATCH,
                            "Amount mismatch between internal ledger (" + internal.amount() + ") and external clearing (" + external.getAmount() + ")",
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
                        "Internal transaction ref [" + internal.txnRef() + "] missing in external clearing feed",
                        "OPEN",
                        null,
                        null,
                        Instant.now(),
                        null
                ));
            }
        }

        // 2. Identify external records that were never matched internally
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
                        "External clearing transaction [" + external.getExternalTxnRef() + "] has no corresponding internal booking record",
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
