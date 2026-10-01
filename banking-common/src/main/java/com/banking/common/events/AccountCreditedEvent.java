package com.banking.common.events;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountCreditedEvent(
        String sagaId,
        String transactionId,
        String accountNumber,
        BigDecimal amount,
        String currency,
        BigDecimal updatedBalance,
        Instant timestamp
) {}
