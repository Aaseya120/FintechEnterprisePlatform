package com.banking.fraud.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.fraud.domain.FraudAlert;
import com.banking.fraud.dto.FraudDtos.FraudCheckRequestDto;
import com.banking.fraud.dto.FraudDtos.FraudCheckResultDto;
import com.banking.fraud.repository.FraudAlertRepository;
import com.banking.fraud.service.FraudRuleEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@Tag(name = "Fraud Detection API", description = "Real-Time Risk Scoring, Velocity Checking & Anomaly Prevention")
public class FraudDetectionController {

    private final FraudRuleEngine ruleEngine;
    private final FraudAlertRepository alertRepository;

    public FraudDetectionController(FraudRuleEngine ruleEngine, FraudAlertRepository alertRepository) {
        this.ruleEngine = ruleEngine;
        this.alertRepository = alertRepository;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Real-Time Risk Evaluation", description = "Evaluates transaction against velocity, high-amount, and impossible travel rules")
    public ResponseEntity<ApiResponse<FraudCheckResultDto>> evaluate(
            @Valid @RequestBody FraudCheckRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        FraudCheckResultDto result = ruleEngine.evaluateTransaction(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Fraud evaluation completed", corrId));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Query Fraud Alerts (Fraud Analyst Portal)")
    public ResponseEntity<ApiResponse<List<FraudAlert>>> getAlerts(
            @RequestParam(required = false) FraudAlert.RiskDecision decision,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<FraudAlert> alerts = (decision != null)
                ? alertRepository.findByRiskDecision(decision)
                : alertRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(alerts, corrId));
    }
}
