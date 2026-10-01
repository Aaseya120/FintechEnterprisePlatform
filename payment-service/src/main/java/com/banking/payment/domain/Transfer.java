package com.banking.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "transfers",
    indexes = {
        @Index(name = "idx_transfers_saga", columnList = "saga_id, status"),
        @Index(name = "idx_transfers_src_created", columnList = "source_account, created_at"),
        @Index(name = "idx_transfers_tgt_created", columnList = "target_account, created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_transfers_idempotency", columnNames = "idempotency_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Transfer {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "saga_id", length = 36, nullable = false)
    private String sagaId;

    @Column(name = "idempotency_key", length = 64, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "source_account", length = 34, nullable = false)
    private String sourceAccount;

    @Column(name = "target_account", length = 34, nullable = false)
    private String targetAccount;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private TransferStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Transfer(String id, String sagaId, String idempotencyKey, String sourceAccount,
                    String targetAccount, BigDecimal amount, String currency, String channel) {
        this.id = id;
        this.sagaId = sagaId;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.currency = currency.toUpperCase();
        this.status = TransferStatus.INITIATED;
        this.channel = channel;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateStatus(TransferStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }
}
