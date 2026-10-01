-- Flyway Migration V1: Loan Management and Repayment Amortization Schedule

CREATE TABLE loans (
    id VARCHAR(36) NOT NULL,
    loan_account_number VARCHAR(34) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    loan_type VARCHAR(30) NOT NULL,
    principal_amount NUMERIC(19, 4) NOT NULL,
    annual_interest_rate NUMERIC(6, 4) NOT NULL,
    tenure_months INT NOT NULL,
    emi_amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
    disbursement_account VARCHAR(34) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_loans PRIMARY KEY (id),
    CONSTRAINT uk_loan_acc UNIQUE (loan_account_number)
);

CREATE INDEX idx_loans_customer_status ON loans (customer_id, status);

CREATE TABLE loan_repayment_schedule (
    id VARCHAR(36) NOT NULL,
    loan_id VARCHAR(36) NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_component NUMERIC(19, 4) NOT NULL,
    interest_component NUMERIC(19, 4) NOT NULL,
    total_installment NUMERIC(19, 4) NOT NULL,
    remaining_balance NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_repayment_schedule PRIMARY KEY (id),
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loans(id)
);

CREATE INDEX idx_repayment_loan_due ON loan_repayment_schedule (loan_id, due_date);
