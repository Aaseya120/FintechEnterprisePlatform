package com.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCompensatedEvent(
        String sagaId,
        String transactionId,
        String sourceAccountNumber,
        BigDecimal amount,
        String reason,
        Instant timestamp
) {}
