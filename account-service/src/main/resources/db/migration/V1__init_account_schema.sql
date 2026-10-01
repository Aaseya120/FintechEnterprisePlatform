-- Flyway Migration V1: Core Banking Accounts and Balance History Schema
-- Designed for high-throughput reads/writes with composite indexes and optimistic locking

CREATE TABLE accounts (
    id VARCHAR(36) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number)
);

-- High-performance composite indexes for query optimization
-- 1. Index for customer dashboard querying active accounts:
CREATE INDEX idx_accounts_customer_status ON accounts (customer_id, status);

-- 2. Index for currency-based balance queries:
CREATE INDEX idx_accounts_curr_bal ON accounts (currency, available_balance);

-- Account Audit History Table
CREATE TABLE account_audit_log (
    id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    previous_balance NUMERIC(19, 4),
    new_balance NUMERIC(19, 4),
    actor_id VARCHAR(64),
    ip_address VARCHAR(45),
    correlation_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_account_audit PRIMARY KEY (id),
    CONSTRAINT fk_audit_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_audit_account_created ON account_audit_log (account_id, created_at DESC);
CREATE INDEX idx_audit_correlation_id ON account_audit_log (correlation_id);
