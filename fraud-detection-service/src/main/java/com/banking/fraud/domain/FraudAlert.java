package com.banking.fraud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "fraud_alerts",
    indexes = {
        @Index(name = "idx_fraud_acc_created", columnList = "account_number, created_at"),
        @Index(name = "idx_fraud_decision", columnList = "risk_decision")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FraudAlert {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "transaction_id", length = 64, nullable = false)
    private String transactionId;

    @Column(name = "account_number", length = 34, nullable = false)
    private String accountNumber;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_decision", length = 30, nullable = false)
    private RiskDecision riskDecision;

    @Column(name = "triggered_rules", nullable = false, columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum RiskDecision { APPROVED, REVIEW_REQUIRED, REJECTED }

    public FraudAlert(String id, String transactionId, String accountNumber, BigDecimal amount,
                      int riskScore, RiskDecision riskDecision, String triggeredRules,
                      String clientIp, String location) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.riskScore = riskScore;
        this.riskDecision = riskDecision;
        this.triggeredRules = triggeredRules;
        this.clientIp = clientIp;
        this.location = location;
        this.createdAt = Instant.now();
    }
}
