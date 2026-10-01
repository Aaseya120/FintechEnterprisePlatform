package com.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionFailedEvent(
        String sagaId,
        String transactionId,
        String sourceAccountNumber,
        BigDecimal amount,
        String failureReason,
        Instant timestamp
) {}
