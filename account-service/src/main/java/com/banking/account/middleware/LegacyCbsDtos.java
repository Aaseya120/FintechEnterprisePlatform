package com.banking.account.middleware;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class LegacyCbsDtos {

    public record CbsAccountInquiryResponse(
            String legacyAccountRef,
            String coreSystemName,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            String currency,
            String status,
            Instant lastSyncedAt
    ) implements Serializable {}

    public record CbsPostingRequest(
            String transactionRef,
            String sourceAccount,
            String targetAccount,
            BigDecimal amount,
            String currency,
            String narration,
            String channel
    ) implements Serializable {}

    public record CbsPostingResponse(
            String cbsHostRef,
            String status,
            BigDecimal updatedBalance,
            String responseCode,
            String message,
            Instant postedAt
    ) implements Serializable {}

    public record CbsHoldRequest(
            String holdRef,
            String accountNumber,
            BigDecimal amount,
            String reason,
            int durationHours
    ) implements Serializable {}

    public record CbsHoldResponse(
            String holdId,
            String status,
            Instant expiresAt
    ) implements Serializable {}

    public record CbsHealthCheckResponse(
            String coreSystem,
            String endpointUrl,
            String status,
            long latencyMs,
            Instant timestamp
    ) implements Serializable {}
}
