package com.banking.loan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "loan_repayments",
    indexes = {
        @Index(name = "idx_loan_repay_loan", columnList = "loan_id"),
        @Index(name = "idx_loan_repay_cust", columnList = "customer_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class LoanRepayment {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "loan_id", length = 36, nullable = false)
    private String loanId;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "amount_paid", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 30, nullable = false)
    private PaymentType paymentType;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @Column(name = "transaction_reference", length = 100, nullable = false)
    private String transactionReference;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    public enum PaymentType { EMI_INSTALLMENT, FORECLOSURE_PAYOFF, PARTIAL_PREPAYMENT }

    public LoanRepayment(String id, String loanId, String customerId, BigDecimal amountPaid,
                         PaymentType paymentType, String paymentMethod, String transactionReference) {
        this.id = id;
        this.loanId = loanId;
        this.customerId = customerId;
        this.amountPaid = amountPaid;
        this.paymentType = paymentType;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.paidAt = Instant.now();
    }
}
