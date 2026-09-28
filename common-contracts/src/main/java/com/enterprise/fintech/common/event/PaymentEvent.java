package com.enterprise.fintech.common.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS,
        FAILED,
        REVERSED
    }

    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private Status status;
    private String transactionRef;
    private String reason;
    private Instant timestamp;
    private String traceId;

    public PaymentEvent() {}

    public PaymentEvent(String paymentId, String orderId, BigDecimal amount, Status status,
                        String transactionRef, String reason, Instant timestamp, String traceId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.transactionRef = transactionRef;
        this.reason = reason;
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
