package com.enterprise.fintech.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Enterprise Accounting Calculation Engine.
 * Implemented using pure Java 8 constructs:
 * - Stream API (filter, map, reduce, groupingBy)
 * - java.time LocalDate
 * - Optional & Method references
 * - Thread-safe CopyOnWriteArrayList for concurrent access
 */
public class LegacyAccountingEngine {

    private static final Logger log = LoggerFactory.getLogger(LegacyAccountingEngine.class);

    private final List<LegacyLedgerEntry> entries = new CopyOnWriteArrayList<>();

    public void recordEntry(LegacyLedgerEntry entry) {
        Objects.requireNonNull(entry, "Entry must not be null");
        entries.add(entry);
        log.info("Recorded ledger entry {} for account {} [{}]",
                entry.getEntryId(), entry.getAccountId(), entry.getEntryType());
    }

    /**
     * Calculates net balance for account using Java 8 Stream API reduce.
     */
    public BigDecimal calculateNetBalance(String accountId) {
        Objects.requireNonNull(accountId, "accountId cannot be null");

        return entries.stream()
                .filter(e -> accountId.equals(e.getAccountId()))
                .map(e -> e.getEntryType() == LegacyLedgerEntry.EntryType.CREDIT
                        ? e.getAmount()
                        : e.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Look up the currency for a given account from recorded ledger entries.
     */
    public Optional<String> getAccountCurrency(String accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(e -> accountId.equals(e.getAccountId()))
                .map(LegacyLedgerEntry::getCurrency)
                .findFirst();
    }

    /**
     * Groups entries by account using Collectors.groupingBy.
     */
    public Map<String, List<LegacyLedgerEntry>> groupEntriesByAccount() {
        return entries.stream()
                .collect(Collectors.groupingBy(LegacyLedgerEntry::getAccountId));
    }

    /**
     * Filters entries for audit within date range using Stream API and java.time.
     */
    public List<LegacyLedgerEntry> findEntriesInDateRange(LocalDate start, LocalDate end) {
        return entries.stream()
                .filter(e -> !e.getPostingDate().isBefore(start) && !e.getPostingDate().isAfter(end))
                .sorted(Comparator.comparing(LegacyLedgerEntry::getPostingDate))
                .collect(Collectors.toList());
    }

    /**
     * Returns total credits for a specific currency.
     */
    public BigDecimal calculateTotalCreditsByCurrency(String currency) {
        return entries.stream()
                .filter(e -> e.getEntryType() == LegacyLedgerEntry.EntryType.CREDIT)
                .filter(e -> currency.equalsIgnoreCase(e.getCurrency()))
                .map(LegacyLedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
