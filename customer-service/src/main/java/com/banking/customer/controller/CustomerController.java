package com.banking.customer.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.customer.domain.Customer.CustomerTier;
import com.banking.customer.domain.CustomerActionAuditLog;
import com.banking.customer.domain.CustomerKyc;
import com.banking.customer.dto.CustomerDtos.*;
import com.banking.customer.service.BeneficiaryService;
import com.banking.customer.service.CustomerKycService;
import com.banking.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for customer onboarding, document verification,
 * credentials & account provisioning, and beneficiary management.
 */
@RestController
@RequestMapping("/api/v1/customers")
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

    /**
     * Submits a new customer registration profile. Status begins in ONBOARDING.
     */
    @PostMapping("/onboard")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> onboard(
            @Valid @RequestBody OnboardingRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerResponseDto response = customerService.onboardCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer registration submitted", corrId));
    }

    /**
     * Retrieves customer profile by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomer(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerResponseDto response = customerService.getCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    /**
     * Submits national ID, address proof, selfie, and V-KYC for compliance review.
     */
    @PostMapping("/{customerId}/kyc")
    public ResponseEntity<ApiResponse<CustomerKyc>> submitKyc(
            @PathVariable String customerId,
            @Valid @RequestBody KycSubmissionDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerKyc kyc = kycService.submitKyc(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(kyc, "KYC documents submitted for review", corrId));
    }

    /**
     * Compliance review of KYC documentation.
     */
    @PostMapping("/kyc/{kycId}/review")
    public ResponseEntity<ApiResponse<CustomerKyc>> reviewKyc(
            @PathVariable String kycId,
            @Valid @RequestBody KycReviewDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerKyc kyc = kycService.reviewKyc(kycId, request);
        return ResponseEntity.ok(ApiResponse.success(kyc, "KYC review processed", corrId));
    }

    /**
     * Backoffice Onboarding Approval:
     * Validates submitted documents, approves KYC, activates customer,
     * generates primary bank account number, and creates initial secure login credentials.
     */
    @PostMapping("/{customerId}/approve-onboarding")
    public ResponseEntity<ApiResponse<OnboardingApprovalResponseDto>> approveOnboarding(
            @PathVariable String customerId,
            @Valid @RequestBody OnboardingApprovalRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        OnboardingApprovalResponseDto response = kycService.approveCustomerOnboarding(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer onboarding approved and account provisioned", corrId));
    }

    /**
     * Adds a new beneficiary with mandatory 4-hour cooling window.
     */
    @PostMapping("/{customerId}/beneficiaries")
    public ResponseEntity<ApiResponse<BeneficiaryResponseDto>> addBeneficiary(
            @PathVariable String customerId,
            @Valid @RequestBody BeneficiaryRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        BeneficiaryResponseDto response = beneficiaryService.addBeneficiary(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Beneficiary registered. 4-hour cooling window active.", corrId));
    }

    /**
     * Lists approved/active beneficiaries for customer.
     */
    @GetMapping("/{customerId}/beneficiaries")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponseDto>>> getBeneficiaries(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<BeneficiaryResponseDto> list = beneficiaryService.listBeneficiaries(customerId);
        return ResponseEntity.ok(ApiResponse.success(list, corrId));
    }

    /**
     * Returns customer tier dashboard with daily limits and permitted microservices.
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ApiResponse<CustomerDashboardDto>> getDashboard(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerDashboardDto dashboard = customerService.getDashboardServices(id);
        return ResponseEntity.ok(ApiResponse.success(dashboard, corrId));
    }

    /**
     * Upgrades customer tier (BASIC -> PREMIUM -> PLATINUM -> HNI).
     */
    @PutMapping("/{id}/tier")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> upgradeTier(
            @PathVariable String id,
            @RequestParam CustomerTier tier,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerResponseDto response = customerService.upgradeCustomerTier(id, tier);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer upgraded to " + tier.name(), corrId));
    }

    /**
     * Captures customer action audit trail across services.
     */
    @PostMapping("/{id}/actions")
    public ResponseEntity<ApiResponse<CustomerActionAuditLog>> logAction(
            @PathVariable String id,
            @Valid @RequestBody LogCustomerActionRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CustomerActionAuditLog log = customerService.logCustomerAction(
                id, request.serviceId(), request.serviceId(), request.actionType(),
                request.resourceId(), request.details(), request.channel(),
                request.ipAddress(), request.status()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(log, "Action audited", corrId));
    }

    /**
     * Returns customer action audit logs.
     */
    @GetMapping("/{id}/actions")
    public ResponseEntity<ApiResponse<List<CustomerActionAuditLog>>> getAuditLogs(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<CustomerActionAuditLog> logs = customerService.getCustomerAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.success(logs, corrId));
    }
}
