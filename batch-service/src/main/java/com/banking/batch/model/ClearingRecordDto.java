package com.banking.batch.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClearingRecordDto(
        String externalTxnRef,
        String sourceAccount,
        String targetAccount,
        BigDecimal amount,
        String currency,
        String channel,
        String settlementDate
) implements Serializable {}
