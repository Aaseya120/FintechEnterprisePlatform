package com.banking.batch.reconciliation.factory;

import com.banking.batch.reconciliation.ReconciliationModels.ReconRuleType;
import com.banking.batch.reconciliation.strategy.ReconciliationRuleStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory Pattern: Resolves and provides the appropriate ReconciliationRuleStrategy instance.
 */
@Component
public class ReconciliationRuleFactory {

    private final Map<ReconRuleType, ReconciliationRuleStrategy> strategies = new EnumMap<>(ReconRuleType.class);

    public ReconciliationRuleFactory(List<ReconciliationRuleStrategy> strategyList) {
        for (var strategy : strategyList) {
            this.strategies.put(strategy.getRuleType(), strategy);
        }
    }

    public ReconciliationRuleStrategy getStrategy(ReconRuleType ruleType) {
        return Optional.ofNullable(strategies.get(ruleType))
                .orElseThrow(() -> new IllegalArgumentException("No reconciliation strategy registered for rule type: " + ruleType));
    }
}
