package com.banking.batch.reconciliation.strategy;

import com.banking.batch.model.StagingTransaction;
import com.banking.batch.reconciliation.ReconciliationModels.InternalLedgerRecord;
import com.banking.batch.reconciliation.ReconciliationModels.ReconRuleType;
import com.banking.batch.reconciliation.ReconciliationModels.StrategyReconResult;

import java.util.List;

/**
 * Strategy Pattern Interface: Encapsulates specific financial matching algorithms.
 */
public interface ReconciliationRuleStrategy {

    ReconRuleType getRuleType();

    StrategyReconResult reconcile(List<InternalLedgerRecord> internalTxns, List<StagingTransaction> externalTxns);
}
