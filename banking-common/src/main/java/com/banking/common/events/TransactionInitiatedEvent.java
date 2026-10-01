package com.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionInitiatedEvent(
        String sagaId,
        String transactionId,
        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String channel, // "IOS", "ANDROID", "WEB"
        Instant timestamp
) {}
