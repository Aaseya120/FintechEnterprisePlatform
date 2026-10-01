package com.banking.reporting.export;

import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ExportStrategyFactory {

    private final Map<ExportFormat, TransactionExportStrategy> strategies = new EnumMap<>(ExportFormat.class);

    public ExportStrategyFactory(List<TransactionExportStrategy> strategyList) {
        for (TransactionExportStrategy strategy : strategyList) {
            this.strategies.put(strategy.getFormat(), strategy);
        }
    }

    public TransactionExportStrategy getStrategy(ExportFormat format) {
        TransactionExportStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new BankingException("UNSUPPORTED_EXPORT_FORMAT",
                    "No export strategy registered for format: " + format,
                    HttpStatus.BAD_REQUEST);
        }
        return strategy;
    }
}
