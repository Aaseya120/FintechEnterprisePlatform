package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String accountNumber, String requestedAmount) {
        super("INSUFFICIENT_FUNDS",
              String.format("Account %s has insufficient funds to fulfill requested debit of %s", accountNumber, requestedAmount),
              HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
