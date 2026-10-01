package com.banking.customer.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.customer.domain.CustomerKyc;
import com.banking.customer.dto.CustomerDtos.*;
import com.banking.customer.service.BeneficiaryService;
import com.banking.customer.service.CustomerKycService;
import com.banking.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer & KYC API", description = "Digital Onboarding, KYC Verification, and Beneficiary Management")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerKycService kycService;
    private final BeneficiaryService beneficiaryService;

    public CustomerController(CustomerService customerService,
                              CustomerKycService kycService,
                              BeneficiaryService beneficiaryService) {
        this.customerService = customerService;
        this.kycService = kycService;
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping("/onboard")
    @Operation(summary = "Digital Customer Onboarding", description = "Submits new customer profile and generates customer identifier")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> onboard(
            @Valid @RequestBody OnboardingRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerResponseDto response = customerService.onboardCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer successfully onboarded", corrId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Customer Profile")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomer(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerResponseDto response = customerService.getCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @PostMapping("/{customerId}/kyc")
    @Operation(summary = "Submit KYC Documentation")
    public ResponseEntity<ApiResponse<CustomerKyc>> submitKyc(
            @PathVariable String customerId,
            @Valid @RequestBody KycSubmissionDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerKyc kyc = kycService.submitKyc(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(kyc, "KYC documents submitted for review", corrId));
    }

    @PostMapping("/kyc/{kycId}/review")
    @Operation(summary = "Review KYC Documentation (Officer / Automated Service)")
    public ResponseEntity<ApiResponse<CustomerKyc>> reviewKyc(
            @PathVariable String kycId,
            @Valid @RequestBody KycReviewDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerKyc kyc = kycService.reviewKyc(kycId, request);
        return ResponseEntity.ok(ApiResponse.success(kyc, "KYC review processed", corrId));
    }

    @PostMapping("/{customerId}/beneficiaries")
    @Operation(summary = "Add New Beneficiary with 4-Hour Cooling-Off Period")
    public ResponseEntity<ApiResponse<BeneficiaryResponseDto>> addBeneficiary(
            @PathVariable String customerId,
            @Valid @RequestBody BeneficiaryRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BeneficiaryResponseDto response = beneficiaryService.addBeneficiary(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Beneficiary registered. 4-hour cooling window active.", corrId));
    }

    @GetMapping("/{customerId}/beneficiaries")
    @Operation(summary = "List Active Beneficiaries")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponseDto>>> getBeneficiaries(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<BeneficiaryResponseDto> list = beneficiaryService.listBeneficiaries(customerId);
        return ResponseEntity.ok(ApiResponse.success(list, corrId));
    }
}
