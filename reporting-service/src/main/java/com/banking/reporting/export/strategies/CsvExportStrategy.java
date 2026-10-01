package com.banking.reporting.export.strategies;

import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;
import com.banking.reporting.dto.ReportingDtos.TransactionRecordDto;
import com.banking.reporting.export.ExportFormat;
import com.banking.reporting.export.TransactionExportStrategy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CsvExportStrategy implements TransactionExportStrategy {

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.CSV;
    }

    @Override
    public String getContentType() {
        return "text/csv";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public byte[] export(StatementResponseDto data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction ID,Timestamp,Type,Amount,Currency,Channel,Status\n");
        for (TransactionRecordDto tx : data.transactions()) {
            sb.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    tx.transactionId(),
                    tx.timestamp(),
                    tx.type(),
                    tx.amount(),
                    tx.currency(),
                    tx.channel(),
                    tx.status()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
