-- Flyway Migration V4: Add Term Deposits / Fixed Deposits (FD) Schema

CREATE TABLE IF NOT EXISTS term_deposits (
    id VARCHAR(36) PRIMARY KEY,
    deposit_number VARCHAR(32) NOT NULL UNIQUE,
    customer_id VARCHAR(36) NOT NULL,
    linked_account_number VARCHAR(34) NOT NULL,
    principal_amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    interest_rate NUMERIC(6, 4) NOT NULL,
    tenor_months INT NOT NULL,
    compounding_frequency VARCHAR(20) NOT NULL,
    maturity_amount NUMERIC(19, 4) NOT NULL,
    maturity_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    auto_renewal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_td_customer ON term_deposits (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_td_maturity ON term_deposits (maturity_date);
