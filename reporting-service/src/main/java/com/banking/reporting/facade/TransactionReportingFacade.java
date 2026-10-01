package com.banking.reporting.facade;

import com.banking.reporting.dto.ReportingDtos.AccountTurnoverSummaryDto;
import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;
import com.banking.reporting.dto.ReportingDtos.TransactionSearchCriteria;
import com.banking.reporting.export.ExportFormat;
import com.banking.reporting.export.ExportStrategyFactory;
import com.banking.reporting.export.TransactionExportStrategy;
import com.banking.reporting.importing.ImportModels.ImportedTransactionDto;
import com.banking.reporting.importing.ImportModels.TransactionImportParser;
import com.banking.reporting.importing.ImportParserFactory;
import com.banking.reporting.service.ReportingService;
import com.banking.reporting.domain.ReportAuditLog;
import com.banking.reporting.repository.ReportAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Facade Pattern: Unified subsystem facade orchestrating statement generation,
 * multi-format exports (PDF, Excel, CSV, JSON), and bulk file imports.
 */
@Component
public class TransactionReportingFacade {

    private static final Logger log = LoggerFactory.getLogger(TransactionReportingFacade.class);

    private final ReportingService reportingService;
    private final ExportStrategyFactory exportStrategyFactory;
    private final ImportParserFactory importParserFactory;
    private final ReportAuditLogRepository auditLogRepository;

    public TransactionReportingFacade(ReportingService reportingService,
                                      ExportStrategyFactory exportStrategyFactory,
                                      ImportParserFactory importParserFactory,
                                      ReportAuditLogRepository auditLogRepository) {
        this.reportingService = reportingService;
        this.exportStrategyFactory = exportStrategyFactory;
        this.importParserFactory = importParserFactory;
        this.auditLogRepository = auditLogRepository;
    }

    public StatementResponseDto getStatementData(TransactionSearchCriteria criteria) {
        return reportingService.generateStatement(criteria);
    }

    public AccountTurnoverSummaryDto getTurnoverSummary(TransactionSearchCriteria criteria) {
        return reportingService.generateStatement(criteria).summary();
    }

    public ExportPayload exportStatement(TransactionSearchCriteria criteria, ExportFormat format) {
        log.info("Executing statement export [Account: {}, Format: {}]", criteria.accountNumber(), format);
        StatementResponseDto statementData = reportingService.generateStatement(criteria);

        TransactionExportStrategy strategy = exportStrategyFactory.getStrategy(format);
        byte[] bytes = strategy.export(statementData);
        String fileName = String.format("statement_%s.%s", criteria.accountNumber(), strategy.getFileExtension());

        try {
            ReportAuditLog auditLog = new ReportAuditLog(
                    UUID.randomUUID().toString(),
                    criteria.accountNumber(),
                    format.name(),
                    statementData.transactions().size(),
                    (long) bytes.length,
                    fileName,
                    "USER_REQUEST",
                    Instant.now()
            );
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist report audit record: {}", e.getMessage());
        }

        return new ExportPayload(bytes, strategy.getContentType(), fileName);
    }

    public List<ReportAuditLog> getAuditLogs(String accountNumber) {
        if (accountNumber != null && !accountNumber.isBlank()) {
            return auditLogRepository.findByAccountNumberOrderByExportedAtDesc(accountNumber);
        }
        return auditLogRepository.findTop20ByOrderByExportedAtDesc();
    }

    public List<ImportedTransactionDto> importTransactions(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("Processing bulk transaction upload: {}", originalFilename);

        TransactionImportParser parser = importParserFactory.getParser(originalFilename);
        try (InputStream is = file.getInputStream()) {
            List<ImportedTransactionDto> parsed = parser.parse(is);
            log.info("Successfully imported and parsed {} transactions from {}", parsed.size(), originalFilename);
            return parsed;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    public record ExportPayload(byte[] data, String contentType, String fileName) {}
}
