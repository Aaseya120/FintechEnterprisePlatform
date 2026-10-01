package com.banking.loan.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.loan.dto.LoanDtos.*;
import com.banking.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loan Management API", description = "Loan Applications, Underwriting, Amortization Schedules & Disbursement")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/calculate-emi")
    @Operation(summary = "Calculate Loan EMI & Amortization Estimates")
    public ResponseEntity<ApiResponse<EmiCalculationResponseDto>> calculateEmi(
            @Valid @RequestBody EmiCalculationRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        EmiCalculationResponseDto response = loanService.calculateEmi(request);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @PostMapping("/apply")
    @Operation(summary = "Submit Loan Application")
    public ResponseEntity<ApiResponse<LoanResponseDto>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanResponseDto response = loanService.applyForLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Loan application submitted", corrId));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve Loan Application (Credit Officer)")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approveLoan(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanResponseDto response = loanService.approveLoan(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan approved", corrId));
    }

    @PostMapping("/{id}/disburse")
    @Operation(summary = "Disburse Loan Funds & Generate Repayment Schedule")
    public ResponseEntity<ApiResponse<LoanResponseDto>> disburseLoan(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanResponseDto response = loanService.disburseLoan(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan disbursed and scheduled", corrId));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Get Monthly Repayment Schedule")
    public ResponseEntity<ApiResponse<List<RepaymentScheduleItemDto>>> getSchedule(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<RepaymentScheduleItemDto> schedule = loanService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(schedule, corrId));
    }
}
