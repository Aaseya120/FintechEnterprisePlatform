package com.enterprise.fintech.order.domain;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    COMPLETED,
    FAILED,
    CANCELLED
}
