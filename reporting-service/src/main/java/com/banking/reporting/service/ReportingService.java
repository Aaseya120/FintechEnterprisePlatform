package com.banking.reporting.service;

import com.banking.reporting.dto.ReportingDtos.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingService {

    public StatementResponseDto generateStatement(TransactionSearchCriteria criteria) {
        List<TransactionRecordDto> sampleRecords = fetchLedgerRecords(criteria);

        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        for (TransactionRecordDto record : sampleRecords) {
            if ("CREDIT".equalsIgnoreCase(record.type())) {
                totalCredits = totalCredits.add(record.amount());
            } else {
                totalDebits = totalDebits.add(record.amount());
            }
        }

        BigDecimal netCashFlow = totalCredits.subtract(totalDebits);
        AccountTurnoverSummaryDto summary = new AccountTurnoverSummaryDto(
                criteria.accountNumber(),
                criteria.fromDate() != null ? criteria.fromDate() : LocalDate.now().minusMonths(1),
                criteria.toDate() != null ? criteria.toDate() : LocalDate.now(),
                totalCredits,
                totalDebits,
                netCashFlow,
                sampleRecords.size()
        );

        return new StatementResponseDto(
                criteria.accountNumber(),
                summary.fromDate(),
                summary.toDate(),
                summary,
                sampleRecords
        );
    }

    public String generateCsvStatement(TransactionSearchCriteria criteria) {
        StatementResponseDto statement = generateStatement(criteria);
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction ID,Date,Type,Amount,Currency,Source Account,Target Account,Channel,Status\n");

        for (TransactionRecordDto tx : statement.transactions()) {
            sb.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    tx.transactionId(),
                    tx.timestamp(),
                    tx.type(),
                    tx.amount(),
                    tx.currency(),
                    tx.sourceAccount(),
                    tx.targetAccount(),
                    tx.channel(),
                    tx.status()));
        }
        return sb.toString();
    }

    private List<TransactionRecordDto> fetchLedgerRecords(TransactionSearchCriteria criteria) {
        List<TransactionRecordDto> records = new ArrayList<>();
        records.add(new TransactionRecordDto(
                "tx_1001", criteria.accountNumber(), "US2000000002",
                new BigDecimal("250.00"), "USD", "DEBIT", "IOS", "COMPLETED", Instant.now().minusSeconds(86400)
        ));
        records.add(new TransactionRecordDto(
                "tx_1002", "US9999999999", criteria.accountNumber(),
                new BigDecimal("1200.00"), "USD", "CREDIT", "WEB", "COMPLETED", Instant.now().minusSeconds(43200)
        ));
        records.add(new TransactionRecordDto(
                "tx_1003", criteria.accountNumber(), "MERCHANT_AMAZON",
                new BigDecimal("84.50"), "USD", "DEBIT", "CARD", "COMPLETED", Instant.now().minusSeconds(10800)
        ));
        return records;
    }
}
