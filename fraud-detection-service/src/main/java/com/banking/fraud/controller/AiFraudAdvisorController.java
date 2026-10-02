package com.banking.fraud.controller;

import com.banking.fraud.dto.FraudDtos.FraudCheckRequestDto;
import com.banking.fraud.dto.FraudDtos.FraudCheckResultDto;
import com.banking.fraud.service.AiFraudAdvisorService;
import com.banking.fraud.service.AiFraudAdvisorService.AiRiskExplanation;
import com.banking.fraud.service.FraudRuleEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing AI-Assisted Fraud Analysis & LLM Risk Reasoning endpoints.
 */
@RestController
@RequestMapping("/api/v1/fraud/ai")
@Tag(name = "AI Fraud Intelligence", description = "Spring AI & GenAI-assisted transaction risk reasoning and AML triage")
public class AiFraudAdvisorController {

    private final FraudRuleEngine fraudRuleEngine;
    private final AiFraudAdvisorService aiFraudAdvisorService;

    public AiFraudAdvisorController(FraudRuleEngine fraudRuleEngine, AiFraudAdvisorService aiFraudAdvisorService) {
        this.fraudRuleEngine = fraudRuleEngine;
        this.aiFraudAdvisorService = aiFraudAdvisorService;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze transaction with AI risk reasoning", description = "Combines Redis velocity heuristics with GenAI reasoning to output an AML narrative and recommended triage action")
    public ResponseEntity<AiRiskExplanation> analyzeWithAi(@Valid @RequestBody FraudCheckRequestDto request) {
        FraudCheckResultDto ruleResult = fraudRuleEngine.evaluateTransaction(request);
        AiRiskExplanation explanation = aiFraudAdvisorService.explainRisk(request, ruleResult);
        return ResponseEntity.ok(explanation);
    }
}
