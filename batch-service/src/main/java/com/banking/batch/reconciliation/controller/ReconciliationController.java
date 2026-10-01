package com.banking.batch.reconciliation.controller;

import com.banking.batch.reconciliation.ReconciliationModels.*;
import com.banking.batch.reconciliation.facade.ReconciliationFacade;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(name = "Automated Transaction Reconciliation API",
     description = "Automated matching of core ledger and external clearing feeds with break detection, tolerance rules, and resolution audit workflows")
public class ReconciliationController {

    private final ReconciliationFacade reconciliationFacade;

    public ReconciliationController(ReconciliationFacade reconciliationFacade) {
        this.reconciliationFacade = reconciliationFacade;
    }

    @PostMapping("/run")
    @Operation(summary = "Execute Automated Reconciliation Process",
               description = "Applies Strategy Pattern (EXACT_MATCH, TOLERANCE_WINDOW) via Factory and Facade to detect discrepancies and breaks")
    public ResponseEntity<ApiResponse<ReconciliationSummary>> executeReconciliation(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "EXACT_MATCH") ReconRuleType ruleType,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LocalDate reconDate = (date != null) ? date : LocalDate.now();

        ReconciliationSummary summary = reconciliationFacade.executeReconciliation(reconDate, ruleType);
        return ResponseEntity.ok(ApiResponse.success(summary, "Reconciliation executed successfully", corrId));
    }

    @GetMapping("/runs")
    @Operation(summary = "List All Reconciliation Runs and Match Rates")
    public ResponseEntity<ApiResponse<List<ReconciliationSummary>>> getAllRuns(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<ReconciliationSummary> runs = reconciliationFacade.getAllRuns();
        return ResponseEntity.ok(ApiResponse.success(runs, corrId));
    }

    @GetMapping("/runs/{runId}")
    @Operation(summary = "Get Reconciliation Run Details and Detected Breaks")
    public ResponseEntity<ApiResponse<ReconciliationSummary>> getRunDetails(
            @PathVariable String runId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        ReconciliationSummary details = reconciliationFacade.getRunDetails(runId);
        return ResponseEntity.ok(ApiResponse.success(details, corrId));
    }

    @GetMapping("/breaks/open")
    @Operation(summary = "Get All Open Reconciliation Breaks Pending Investigation")
    public ResponseEntity<ApiResponse<List<ReconciliationBreak>>> getOpenBreaks(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<ReconciliationBreak> openBreaks = reconciliationFacade.getOpenBreaks();
        return ResponseEntity.ok(ApiResponse.success(openBreaks, corrId));
    }

    @PostMapping("/breaks/{breakId}/resolve")
    @Operation(summary = "Resolve or Write-Off a Reconciliation Break")
    public ResponseEntity<ApiResponse<ReconciliationBreak>> resolveBreak(
            @PathVariable String breakId,
            @RequestBody BreakResolutionRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        ReconciliationBreak resolved = reconciliationFacade.resolveBreak(breakId, request);
        return ResponseEntity.ok(ApiResponse.success(resolved, "Break resolved successfully", corrId));
    }
}
