-- V3__add_loan_repayments.sql
-- Loan Repayments, EMI Payments, and Foreclosure Payoffs Schema

CREATE TABLE IF NOT EXISTS loan_repayments (
    id VARCHAR(36) PRIMARY KEY,
    loan_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    amount_paid NUMERIC(19, 4) NOT NULL,
    payment_type VARCHAR(30) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_loan_repay_loan ON loan_repayments (loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_repay_cust ON loan_repayments (customer_id);

-- Seed sample repayment
INSERT INTO loan_repayments (id, loan_id, customer_id, amount_paid, payment_type, payment_method, transaction_reference, paid_at)
VALUES
    ('repay_001', 'loan_001', 'cust_001', 1146.75, 'EMI_INSTALLMENT', 'AUTO_DEBIT', 'TXN_LN_INIT_001', NOW() - INTERVAL '30 days')
ON CONFLICT (id) DO NOTHING;
