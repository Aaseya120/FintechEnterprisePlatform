package com.banking.payment.domain;

public enum TransferStatus {
    INITIATED,
    DEBITED,
    CREDITED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
