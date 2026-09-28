package com.enterprise.fintech.legacy.soa;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * JAXB / SOAP XML Request payload for Core Banking SOA Web Service.
 */
public class SoaPostingRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String customerAccountId;
    private String glAccountId;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String remarks;

    public SoaPostingRequest() {}

    public SoaPostingRequest(String transactionId, String customerAccountId, String glAccountId,
                             BigDecimal amount, String currency, String channel, String remarks) {
        this.transactionId = transactionId;
        this.customerAccountId = customerAccountId;
        this.glAccountId = glAccountId;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.remarks = remarks;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCustomerAccountId() { return customerAccountId; }
    public void setCustomerAccountId(String customerAccountId) { this.customerAccountId = customerAccountId; }

    public String getGlAccountId() { return glAccountId; }
    public void setGlAccountId(String glAccountId) { this.glAccountId = glAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
