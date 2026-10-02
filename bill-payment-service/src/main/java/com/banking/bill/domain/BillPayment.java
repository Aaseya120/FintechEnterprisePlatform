package com.banking.bill.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bill_payments", schema = "bill_schema")
public class BillPayment {

    @Id
    private String id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 64)
    private String paymentReference;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "source_account_number", nullable = false, length = 34)
    private String sourceAccountNumber;

    @Column(name = "biller_code", nullable = false, length = 32)
    private String billerCode;

    @Column(name = "consumer_number", nullable = false, length = 64)
    private String consumerNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillPaymentStatus status;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "biller_transaction_ref", length = 128)
    private String billerTransactionRef;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public BillPayment() {}

    public BillPayment(String id, String paymentReference, String customerId,
                       String sourceAccountNumber, String billerCode, String consumerNumber,
                       BigDecimal amount, String currency, BigDecimal serviceFee,
                       String idempotencyKey) {
        this.id = id;
        this.paymentReference = paymentReference;
        this.customerId = customerId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.billerCode = billerCode;
        this.consumerNumber = consumerNumber;
        this.amount = amount;
        this.currency = currency;
        this.serviceFee = serviceFee != null ? serviceFee : BigDecimal.ZERO;
        this.status = BillPaymentStatus.INITIATED;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markSuccess(String billerTransactionRef) {
        this.status = BillPaymentStatus.SUCCESS;
        this.billerTransactionRef = billerTransactionRef;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = BillPaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPaymentReference() { return paymentReference; }
    public String getCustomerId() { return customerId; }
    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getBillerCode() { return billerCode; }
    public String getConsumerNumber() { return consumerNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public BillPaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getBillerTransactionRef() { return billerTransactionRef; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
