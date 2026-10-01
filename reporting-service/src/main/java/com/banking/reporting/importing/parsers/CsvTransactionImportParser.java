package com.banking.reporting.importing.parsers;

import com.banking.common.exception.BankingException;
import com.banking.reporting.importing.ImportModels.ImportedTransactionDto;
import com.banking.reporting.importing.ImportModels.TransactionImportParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvTransactionImportParser implements TransactionImportParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv");
    }

    @Override
    public List<ImportedTransactionDto> parse(InputStream inputStream) {
        List<ImportedTransactionDto> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] tokens = line.split(",");
                if (tokens.length >= 6) {
                    list.add(new ImportedTransactionDto(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            tokens[2].trim(),
                            new BigDecimal(tokens[3].trim()),
                            tokens[4].trim().toUpperCase(),
                            tokens[5].trim(),
                            LocalDate.now()
                    ));
                }
            }
        } catch (Exception e) {
            throw new BankingException("CSV_IMPORT_ERROR", "Failed to parse CSV transaction upload", HttpStatus.BAD_REQUEST, e);
        }
        return list;
    }
}
