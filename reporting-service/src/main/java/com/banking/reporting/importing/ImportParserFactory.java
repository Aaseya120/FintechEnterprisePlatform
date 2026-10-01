package com.banking.reporting.importing;

import com.banking.common.exception.BankingException;
import com.banking.reporting.importing.ImportModels.TransactionImportParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImportParserFactory {

    private final List<TransactionImportParser> parsers;

    public ImportParserFactory(List<TransactionImportParser> parsers) {
        this.parsers = parsers;
    }

    public TransactionImportParser getParser(String fileName) {
        return parsers.stream()
                .filter(p -> p.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new BankingException(
                        "UNSUPPORTED_IMPORT_FORMAT",
                        "No file parser available for upload file: " + fileName + ". Supported formats: .xlsx, .csv",
                        HttpStatus.BAD_REQUEST));
    }
}
