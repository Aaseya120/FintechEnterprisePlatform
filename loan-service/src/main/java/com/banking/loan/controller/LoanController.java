package com.banking.loan.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.loan.dto.LoanDtos.*;
import com.banking.loan.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for loan origination, credit underwriting,
 * amortization schedules, EMI servicing, and foreclosure payoffs.
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * Calculates monthly EMI, total interest, and estimated payment schedule.
     */
    @PostMapping("/calculate-emi")
    public ResponseEntity<ApiResponse<EmiCalculationResponseDto>> calculateEmi(
            @Valid @RequestBody EmiCalculationRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        EmiCalculationResponseDto response = loanService.calculateEmi(request);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    /**
     * Submits a formal loan application for credit assessment.
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanResponseDto>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanResponseDto response = loanService.applyForLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Loan application submitted", corrId));
    }

    /**
     * Approves loan application (officer review).
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approveLoan(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanResponseDto response = loanService.approveLoan(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan approved", corrId));
    }

    /**
     * Disburses loan amount into customer's account and generates amortization schedule.
     */
    @PostMapping("/{id}/disburse")
    public ResponseEntity<ApiResponse<LoanResponseDto>> disburseLoan(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanResponseDto response = loanService.disburseLoan(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan disbursed and scheduled", corrId));
    }

    /**
     * Retrieves month-by-month repayment amortization schedule.
     */
    @GetMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<List<RepaymentScheduleItemDto>>> getSchedule(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<RepaymentScheduleItemDto> schedule = loanService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(schedule, corrId));
    }

    /**
     * Pays a monthly loan installment / EMI.
     */
    @PostMapping("/{id}/repay")
    public ResponseEntity<ApiResponse<LoanRepaymentResponseDto>> payInstallment(
            @PathVariable String id,
            @Valid @RequestBody LoanRepaymentRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanRepaymentResponseDto response = loanService.payInstallment(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan installment processed successfully", corrId));
    }

    /**
     * Generates a 7-day valid premature loan foreclosure payoff quote.
     */
    @GetMapping("/{id}/foreclosure-quote")
    public ResponseEntity<ApiResponse<LoanForeclosureQuoteDto>> getForeclosureQuote(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanForeclosureQuoteDto response = loanService.getForeclosureQuote(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Foreclosure payoff quote generated", corrId));
    }

    /**
     * Prematurely forecloses and closes an active loan.
     */
    @PostMapping("/{id}/foreclose")
    public ResponseEntity<ApiResponse<LoanRepaymentResponseDto>> forecloseLoan(
            @PathVariable String id,
            @Valid @RequestBody LoanRepaymentRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LoanRepaymentResponseDto response = loanService.forecloseLoan(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan foreclosed and closed successfully", corrId));
    }

    /**
     * Retrieves all repayment transactions recorded against a loan.
     */
    @GetMapping("/{id}/repayments")
    public ResponseEntity<ApiResponse<List<LoanRepaymentResponseDto>>> getRepayments(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<LoanRepaymentResponseDto> response = loanService.getRepayments(id);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }
}
