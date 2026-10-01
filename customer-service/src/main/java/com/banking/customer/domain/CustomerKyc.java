package com.banking.customer.domain;

import com.banking.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "customer_kyc",
    indexes = {
        @Index(name = "idx_kyc_customer_status", columnList = "customer_id, verification_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CustomerKyc {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", length = 30, nullable = false)
    private IdType idType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "id_number", length = 255, nullable = false)
    private String idNumber;

    @Column(name = "document_url", length = 255, nullable = false)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_proof_type", length = 30)
    private AddressProofType addressProofType;

    @Column(name = "address_proof_url", length = 255)
    private String addressProofUrl;

    @Column(name = "selfie_url", length = 255)
    private String selfieUrl;

    @Column(name = "liveness_score")
    private Double livenessScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "liveness_status", length = 20)
    private LivenessStatus livenessStatus;

    @Column(name = "video_kyc_url", length = 255)
    private String videoKycUrl;

    @Column(name = "audio_sample_url", length = 255)
    private String audioSampleUrl;

    @Column(name = "geo_latitude")
    private Double geoLatitude;

    @Column(name = "geo_longitude")
    private Double geoLongitude;

    @Column(name = "ocr_extracted_data", length = 1000)
    private String ocrExtractedData;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20, nullable = false)
    private KycStatus verificationStatus;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum IdType { PASSPORT, NATIONAL_ID, DRIVERS_LICENSE, PAN, SSN, AADHAAR }
    public enum AddressProofType { UTILITY_BILL, BANK_STATEMENT, RENTAL_AGREEMENT, VOTER_ID, MUNICIPAL_TAX_RECEIPT }
    public enum LivenessStatus { PASSED, FAILED, PENDING }
    public enum KycStatus { SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED }

    public CustomerKyc(String id, String customerId, IdType idType, String idNumber, String documentUrl) {
        this.id = id;
        this.customerId = customerId;
        this.idType = idType;
        this.idNumber = idNumber;
        this.documentUrl = documentUrl;
        this.verificationStatus = KycStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    public void approve(String officerId) {
        this.verificationStatus = KycStatus.APPROVED;
        this.verifiedBy = officerId;
        this.verifiedAt = Instant.now();
    }

    public void reject(String reason, String officerId) {
        this.verificationStatus = KycStatus.REJECTED;
        this.rejectionReason = reason;
        this.verifiedBy = officerId;
        this.verifiedAt = Instant.now();
    }
}
