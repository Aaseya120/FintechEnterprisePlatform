package com.banking.customer.dto;

import com.banking.customer.domain.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class CustomerDtos {

    public record OnboardingRequestDto(
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName,
            @NotBlank @Email(message = "Valid email is required") String email,
            @NotBlank(message = "Phone number is required") String phone,
            @NotNull @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
            @NotBlank(message = "Address is required") String address,
            String preferredTier // BASIC, PREMIUM, PLATINUM, HNI
    ) implements Serializable {}

    public record CustomerResponseDto(
            String id,
            String customerNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String address,
            Customer.RiskCategory riskCategory,
            Customer.CustomerStatus status,
            Customer.CustomerTier customerTier,
            Double dailyTransferLimit,
            Integer maxCardsAllowed,
            Instant createdAt
    ) implements Serializable {}

    public record ServiceAccessInfoDto(
            String serviceId,
            String serviceName,
            String description,
            String accessStatus, // "GRANTED", "UPGRADE_REQUIRED", "PENDING_KYC"
            String requiredTier,
            List<String> enabledFeatures
    ) implements Serializable {}

    public record CustomerDashboardDto(
            CustomerResponseDto customerProfile,
            String customerTier,
            Double dailyTransferLimit,
            Integer maxCardsAllowed,
            boolean internationalAccess,
            List<ServiceAccessInfoDto> accessibleServices
    ) implements Serializable {}

    public record LogCustomerActionRequestDto(
            @NotBlank String serviceId,
            @NotBlank String actionType,
            String resourceId,
            String details,
            String channel,
            String ipAddress,
            String status
    ) implements Serializable {}

    public record KycSubmissionDto(
            @NotBlank String idType,
            @NotBlank String idNumber,
            @NotBlank String documentUrl,
            String addressProofType,
            String addressProofUrl,
            String selfieUrl,
            String videoKycUrl,
            String audioSampleUrl,
            Double geoLatitude,
            Double geoLongitude,
            String ocrExtractedData
    ) implements Serializable {}

    public record KycReviewDto(
            @NotBlank String action, // "APPROVE" or "REJECT"
            String rejectionReason,
            @NotBlank String officerId
    ) implements Serializable {}

    public record BeneficiaryRequestDto(
            @NotBlank String beneficiaryName,
            @NotBlank String accountNumber,
            @NotBlank String bankName,
            @NotBlank String routingOrIfscCode,
            @NotBlank String beneficiaryType,
            @NotNull java.math.BigDecimal maxTransferLimit
    ) implements Serializable {}

    public record BeneficiaryResponseDto(
            String id,
            String customerId,
            String beneficiaryName,
            String accountNumber,
            String bankName,
            String routingOrIfscCode,
            String beneficiaryType,
            java.math.BigDecimal maxTransferLimit,
            boolean inCoolingPeriod,
            Instant coolingEndTime,
            boolean isActive
    ) implements Serializable {}

    public record OnboardingApprovalRequestDto(
            @NotBlank(message = "Officer ID is required") String officerId,
            String remarks
    ) implements Serializable {}

    public record OnboardingApprovalResponseDto(
            String customerId,
            String customerNumber,
            String fullName,
            String email,
            Customer.CustomerStatus status,
            String primaryAccountNumber,
            String loginUsername,
            String temporaryPassword,
            Instant approvedAt,
            String message
    ) implements Serializable {}
}
