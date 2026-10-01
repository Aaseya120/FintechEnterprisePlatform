package com.banking.reporting.export;

import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;

public interface TransactionExportStrategy {
    ExportFormat getFormat();
    String getContentType();
    String getFileExtension();
    byte[] export(StatementResponseDto data);
}
