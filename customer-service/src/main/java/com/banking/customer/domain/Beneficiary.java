package com.banking.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
    name = "beneficiaries",
    indexes = {
        @Index(name = "idx_beneficiary_cust_status", columnList = "customer_id, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Beneficiary {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "beneficiary_name", length = 100, nullable = false)
    private String beneficiaryName;

    @Column(name = "account_number", length = 34, nullable = false)
    private String accountNumber;

    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;

    @Column(name = "routing_or_ifsc_code", length = 20, nullable = false)
    private String routingOrIfscCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "beneficiary_type", length = 20, nullable = false)
    private BeneficiaryType beneficiaryType;

    @Column(name = "max_transfer_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal maxTransferLimit;

    @Column(name = "cooling_end_time", nullable = false)
    private Instant coolingEndTime;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum BeneficiaryType { INTRA_BANK, INTER_BANK, INTERNATIONAL }

    public Beneficiary(String id, String customerId, String beneficiaryName, String accountNumber,
                       String bankName, String routingOrIfscCode, BeneficiaryType beneficiaryType,
                       BigDecimal maxTransferLimit, int coolingPeriodHours) {
        this.id = id;
        this.customerId = customerId;
        this.beneficiaryName = beneficiaryName;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.routingOrIfscCode = routingOrIfscCode;
        this.beneficiaryType = beneficiaryType;
        this.maxTransferLimit = maxTransferLimit;
        this.coolingEndTime = Instant.now().plus(coolingPeriodHours, ChronoUnit.HOURS);
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public boolean isInCoolingPeriod() {
        return Instant.now().isBefore(this.coolingEndTime);
    }
}
