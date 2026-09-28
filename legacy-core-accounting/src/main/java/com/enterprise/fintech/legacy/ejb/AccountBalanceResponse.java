package com.enterprise.fintech.legacy.ejb;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Account Balance Response DTO returned by Core Banking EJB inquiry.
 */
public class AccountBalanceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private String currency;
    private String accountStatus;

    public AccountBalanceResponse() {}

    public AccountBalanceResponse(String accountId, BigDecimal currentBalance, BigDecimal availableBalance,
                                  String currency, String accountStatus) {
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.currency = currency;
        this.accountStatus = accountStatus;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
}
