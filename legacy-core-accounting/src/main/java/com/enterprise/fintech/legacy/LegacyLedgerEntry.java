package com.enterprise.fintech.legacy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Legacy double-entry ledger entity written to Java 8 specifications.
 */
public class LegacyLedgerEntry {

    public enum EntryType {
        DEBIT,
        CREDIT
    }

    private final String entryId;
    private final String accountId;
    private final BigDecimal amount;
    private final EntryType entryType;
    private final String currency;
    private final LocalDate postingDate;
    private final String narration;

    public LegacyLedgerEntry(String entryId, String accountId, BigDecimal amount,
                             EntryType entryType, String currency, LocalDate postingDate, String narration) {
        this.entryId = Objects.requireNonNull(entryId, "entryId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.entryType = Objects.requireNonNull(entryType, "entryType must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.postingDate = Objects.requireNonNull(postingDate, "postingDate must not be null");
        this.narration = narration;
    }

    public String getEntryId() { return entryId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public EntryType getEntryType() { return entryType; }
    public String getCurrency() { return currency; }
    public LocalDate getPostingDate() { return postingDate; }
    public String getNarration() { return narration; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegacyLedgerEntry that = (LegacyLedgerEntry) o;
        return Objects.equals(entryId, that.entryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryId);
    }
}
