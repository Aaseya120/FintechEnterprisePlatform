package com.banking.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReportingDtos {

    public record TransactionSearchCriteria(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String transactionType // DEBIT, CREDIT, ALL
    ) implements Serializable {}

    public record TransactionRecordDto(
            String transactionId,
            String sourceAccount,
            String targetAccount,
            BigDecimal amount,
            String currency,
            String type, // DEBIT or CREDIT
            String channel,
            String status,
            Instant timestamp
    ) implements Serializable {}

    public record AccountTurnoverSummaryDto(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal totalCredits,
            BigDecimal totalDebits,
            BigDecimal netCashFlow,
            int totalTransactions
    ) implements Serializable {}

    public record StatementResponseDto(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate,
            AccountTurnoverSummaryDto summary,
            List<TransactionRecordDto> transactions
    ) implements Serializable {}
}
