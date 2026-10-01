package com.banking.reporting.importing;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ImportModels {

    public record ImportedTransactionDto(
            String externalRef,
            String sourceAccount,
            String targetAccount,
            BigDecimal amount,
            String currency,
            String channel,
            LocalDate date
    ) implements Serializable {}

    public interface TransactionImportParser {
        boolean supports(String fileName);
        List<ImportedTransactionDto> parse(InputStream inputStream);
    }
}
