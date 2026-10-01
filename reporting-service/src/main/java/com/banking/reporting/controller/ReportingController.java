package com.banking.reporting.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.reporting.domain.ReportAuditLog;
import com.banking.reporting.dto.ReportingDtos.StatementResponseDto;
import com.banking.reporting.dto.ReportingDtos.TransactionSearchCriteria;
import com.banking.reporting.export.ExportFormat;
import com.banking.reporting.facade.TransactionReportingFacade;
import com.banking.reporting.facade.TransactionReportingFacade.ExportPayload;
import com.banking.reporting.importing.ImportModels.ImportedTransactionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reporting, Import & Export API", description = "Multi-Format Statement Export (PDF, Excel, CSV, JSON) and Bulk File Ingestion")
public class ReportingController {

    private final TransactionReportingFacade reportingFacade;

    public ReportingController(TransactionReportingFacade reportingFacade) {
        this.reportingFacade = reportingFacade;
    }

    @GetMapping("/statement")
    @Operation(summary = "Get Statement Data (JSON)")
    public ResponseEntity<ApiResponse<StatementResponseDto>> getStatement(
            @RequestParam String accountNumber,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false, defaultValue = "ALL") String transactionType,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                accountNumber, fromDate, toDate, minAmount, maxAmount, transactionType
        );
        StatementResponseDto statement = reportingFacade.getStatementData(criteria);
        return ResponseEntity.ok(ApiResponse.success(statement, corrId));
    }

    @GetMapping("/export")
    @Operation(summary = "Export Statement in Different Formats (PDF, Excel, CSV, JSON)",
               description = "Generates styled PDF bank statement, Apache POI Excel .xlsx, CSV, or formatted JSON")
    public ResponseEntity<byte[]> exportStatement(
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "PDF") ExportFormat format,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                accountNumber, fromDate, toDate, null, null, "ALL"
        );
        ExportPayload payload = reportingFacade.exportStatement(criteria, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.data());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Transaction History from Excel (.xlsx) or CSV",
               description = "Uploads and parses transaction files via the Import Strategy and Factory pattern")
    public ResponseEntity<ApiResponse<List<ImportedTransactionDto>>> importTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<ImportedTransactionDto> imported = reportingFacade.importTransactions(file);
        return ResponseEntity.ok(ApiResponse.success(imported, "Imported " + imported.size() + " transactions successfully", corrId));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get Statement Export Audit Logs",
               description = "Returns historical report export audit trail for compliance, SOX, and BCBS-239 tracking")
    public ResponseEntity<ApiResponse<List<ReportAuditLog>>> getAuditLogs(
            @RequestParam(required = false) String accountNumber,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<ReportAuditLog> logs = reportingFacade.getAuditLogs(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(logs, corrId));
    }
}
