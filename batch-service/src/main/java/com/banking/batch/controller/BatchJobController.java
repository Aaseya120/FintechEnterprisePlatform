package com.banking.batch.controller;

import com.banking.batch.procedure.OracleProcedureService;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batch")
@Tag(name = "Batch & High-Volume Processing API", description = "Spring Batch Job Triggering, Million-Record File Ingestion, and Oracle 19c PL/SQL Procedures")
public class BatchJobController {

    private final JobLauncher jobLauncher;
    private final Job clearingSettlementJob;
    private final JobExplorer jobExplorer;
    private final OracleProcedureService oracleProcedureService;

    public BatchJobController(JobLauncher jobLauncher,
                              Job clearingSettlementJob,
                              JobExplorer jobExplorer,
                              OracleProcedureService oracleProcedureService) {
        this.jobLauncher = jobLauncher;
        this.clearingSettlementJob = clearingSettlementJob;
        this.jobExplorer = jobExplorer;
        this.oracleProcedureService = oracleProcedureService;
    }

    @PostMapping("/clearing/run")
    @Operation(summary = "Trigger High-Volume Clearing File Ingestion Job",
               description = "Streams huge volume of records from CSV feed into staging DB and triggers Oracle PL/SQL reconciliation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runClearingJob(
            @RequestParam(required = false, defaultValue = "data/clearing_transactions_500k.csv") String filePath,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) throws Exception {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();

        JobParameters params = new JobParametersBuilder()
                .addString("filePath", filePath)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(clearingSettlementJob, params);

        Map<String, Object> result = Map.of(
                "jobExecutionId", execution.getId(),
                "jobName", execution.getJobInstance().getJobName(),
                "status", execution.getStatus().toString(),
                "startTime", String.valueOf(execution.getStartTime())
        );

        return ResponseEntity.ok(ApiResponse.success(result, "Spring Batch job triggered successfully", corrId));
    }

    @PostMapping("/oracle/accrue-interest")
    @Operation(summary = "Invoke Oracle 19c PL/SQL SP_ACCRUE_DAILY_SAVINGS_INTEREST",
               description = "Directly triggers Oracle PL/SQL bulk interest accrual with bulk collect and FORALL batching")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runInterestAccrual(
            @RequestParam(defaultValue = "5000") int batchSize,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        int processedCount = oracleProcedureService.executeDailyInterestAccrual(batchSize);

        Map<String, Object> result = Map.of(
                "procedure", "PKG_BANKING_CORE.SP_ACCRUE_DAILY_SAVINGS_INTEREST",
                "batchSize", batchSize,
                "accountsProcessed", processedCount,
                "status", "SUCCESS"
        );

        return ResponseEntity.ok(ApiResponse.success(result, "Oracle PL/SQL procedure executed", corrId));
    }

    @PostMapping("/oracle/reconcile-eod")
    @Operation(summary = "Invoke Oracle 19c PL/SQL SP_EOD_RECONCILIATION",
               description = "Triggers Oracle PL/SQL End-Of-Day zero-sum ledger double-entry reconciliation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runEodReconciliation(
            @RequestParam(required = false) LocalDate date,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LocalDate reconDate = (date != null) ? date : LocalDate.now();
        Map<String, Object> reconResult = oracleProcedureService.executeEodReconciliation(reconDate);

        return ResponseEntity.ok(ApiResponse.success(reconResult, "Oracle PL/SQL EOD reconciliation executed", corrId));
    }

    @GetMapping("/jobs/{executionId}")
    @Operation(summary = "Get Batch Job Execution Status and Metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatus(
            @PathVariable Long executionId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> statusMap = Map.of(
                "executionId", execution.getId(),
                "status", execution.getStatus().toString(),
                "exitStatus", execution.getExitStatus().getExitCode(),
                "stepExecutions", execution.getStepExecutions().stream().map(s -> Map.of(
                        "stepName", s.getStepName(),
                        "readCount", s.getReadCount(),
                        "writeCount", s.getWriteCount(),
                        "skipCount", s.getSkipCount()
                )).toList()
        );

        return ResponseEntity.ok(ApiResponse.success(statusMap, corrId));
    }
}
