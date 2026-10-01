package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateTransactionException extends BankingException {
    public DuplicateTransactionException(String idempotencyKey) {
        super("DUPLICATE_TRANSACTION",
              String.format("A transaction with idempotency key '%s' is already in-flight or completed", idempotencyKey),
              HttpStatus.CONFLICT);
    }
}
