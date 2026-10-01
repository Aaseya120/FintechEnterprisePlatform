package com.banking.customer.dto;

import com.banking.customer.domain.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public class CustomerDtos {

    public record OnboardingRequestDto(
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName,
            @NotBlank @Email(message = "Valid email is required") String email,
            @NotBlank(message = "Phone number is required") String phone,
            @NotNull @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
            @NotBlank(message = "Address is required") String address
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
            Instant createdAt
    ) implements Serializable {}

    public record KycSubmissionDto(
            @NotBlank String idType,
            @NotBlank String idNumber,
            @NotBlank String documentUrl
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
}
