package com.enterprise.fintech.legacy.ejb;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Journal Entry Request DTO for Core Banking EJB and SOA interfaces.
 */
public class JournalEntryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transactionReference;
    private String accountId;
    private String contraAccountId;
    private BigDecimal amount;
    private String currency;
    private String narration;
    private LocalDate valueDate;

    public JournalEntryRequest() {}

    public JournalEntryRequest(String transactionReference, String accountId, String contraAccountId,
                               BigDecimal amount, String currency, String narration, LocalDate valueDate) {
        this.transactionReference = transactionReference;
        this.accountId = accountId;
        this.contraAccountId = contraAccountId;
        this.amount = amount;
        this.currency = currency;
        this.narration = narration;
        this.valueDate = valueDate != null ? valueDate : LocalDate.now();
    }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getContraAccountId() { return contraAccountId; }
    public void setContraAccountId(String contraAccountId) { this.contraAccountId = contraAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }

    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
}
