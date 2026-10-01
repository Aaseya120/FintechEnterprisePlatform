package com.banking.account.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "account_audit_log",
    indexes = {
        @Index(name = "idx_audit_account_created", columnList = "account_id, created_at"),
        @Index(name = "idx_audit_correlation_id", columnList = "correlation_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AccountAuditLog {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "account_id", length = 36, nullable = false)
    private String accountId;

    @Column(name = "operation_type", length = 30, nullable = false)
    private String operationType;

    @Column(name = "previous_balance", precision = 19, scale = 4)
    private BigDecimal previousBalance;

    @Column(name = "new_balance", precision = 19, scale = 4)
    private BigDecimal newBalance;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AccountAuditLog(String id, String accountId, String operationType, BigDecimal previousBalance,
                           BigDecimal newBalance, String actorId, String ipAddress, String correlationId) {
        this.id = id;
        this.accountId = accountId;
        this.operationType = operationType;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.actorId = actorId;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }
}
