package com.banking.common.idempotency;

import java.io.Serializable;
import java.time.Instant;

public record IdempotentRecord(
        String idempotencyKey,
        String requestHash,
        IdempotencyStatus status,
        Object responseBody,
        int httpStatusCode,
        Instant createdAt
) implements Serializable {

    public enum IdempotencyStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public static IdempotentRecord inProgress(String key, String requestHash) {
        return new IdempotentRecord(key, requestHash, IdempotencyStatus.IN_PROGRESS, null, 0, Instant.now());
    }

    public IdempotentRecord complete(Object responseBody, int httpStatusCode) {
        return new IdempotentRecord(this.idempotencyKey, this.requestHash, IdempotencyStatus.COMPLETED, responseBody, httpStatusCode, this.createdAt);
    }
}
